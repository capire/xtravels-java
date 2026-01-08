package sap.capire.xtravels.handler;

import static cds.gen.travelservice.TravelService_.TRAVELS;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static sap.capire.xtravels.TestData.createBookingData;
import static sap.capire.xtravels.TestData.createTravelData;
import static sap.capire.xtravels.util.ServiceExceptionAssert.assertThatServiceException;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.cqn.CqnInsert;

import cds.gen.travelservice.Bookings;
import cds.gen.travelservice.TravelService;
import cds.gen.travelservice.Travels;

@SpringBootTest
public class TravelServiceTest {

    private Travels travel = createTravelData("test travel");

    @Autowired
    private TravelService srv;

    // Travels

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_OK() {
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        Result result = srv.run(insert);

        assertThat(result.rowCount()).isEqualTo(1l);
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_DescriptionTooShort() {
        travel.setDescription("12");
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatServiceException()
                .isThrownBy(() -> srv.run(insert))
                .isBadRequest()
                .withLocalizedMessage("Description too short")
                .thatTargets("Description");

    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_CustomerIsNull() {
        travel.setCustomerId(null);
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatServiceException()
                .isThrownBy(() -> srv.run(insert))
                .isBadRequest()
                .withLocalizedMessage("Provide the missing value.")
                .thatTargets("Customer");
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_CustomerDoesNotExist() {
        travel.setCustomerId("bad-ID");
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatServiceException()
                .isThrownBy(() -> srv.run(insert))
                .isBadRequest()
                .withLocalizedMessage("Customer does not exist")
                .thatTargets("Customer_ID");
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_AgencyDoesNotExist() {
        travel.setAgencyId("bad-ID");
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatServiceException()
                .isThrownBy(() -> srv.run(insert))
                .isBadRequest()
                .withLocalizedMessage("Agency does not exist")
                .thatTargets("Agency");
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_EndDateBeforeBeginDate() {
        travel.setEndDate(travel.getBeginDate().minus(1, DAYS));
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatServiceException()
                .isThrownBy(() -> srv.run(insert))
                .isBadRequest()
                .withMessageOrKey("ASSERT_ENDDATE_AFTER_BEGINDATE")
                .thatTargets("BeginDate", "EndDate");
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_NegativeBookingFee() {
        travel.setBookingFee(BigDecimal.valueOf(-1.0));
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatServiceException()
                .isThrownBy(() -> srv.run(insert))
                .isBadRequest()
                .withMessageOrKey("ASSERT_BOOKING_FEE_NON_NEGATIVE")
                .thatTargets("BookingFee");
    }

    // Bookings

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_withBooking_BookingDateNotWithinTravelDate() {
        Bookings booking = createBookingData(travel);
        booking.setFlightDate(travel.getEndDate().plus(2, DAYS));
        travel.setBookings(List.of(booking));

        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatServiceException()
                .isThrownBy(() -> srv.run(insert))
                .isBadRequest()
                .withMessageOrKey("ASSERT_BOOKINGS_IN_TRAVEL_PERIOD")
                .thatTargets("Bookings.Flight_date");
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_withBooking_NegativeFlightPrice() {
        Bookings booking = createBookingData(travel);
        booking.setFlightPrice(BigDecimal.valueOf(-1.0));
        travel.setBookings(List.of(booking));

        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatServiceException()
                .isThrownBy(() -> srv.run(insert))
                .isBadRequest()
                .withMessageOrKey("ASSERT_FLIGHT_PRICE_POSITIVE")
                .thatTargets("Bookings.FlightPrice");
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_withBooking_CurrencyMismatch() {
        Bookings booking = createBookingData(travel);
        booking.setCurrencyCode("DKK");
        travel.setBookings(List.of(booking));

        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatServiceException()
                .isThrownBy(() -> srv.run(insert))
                .isBadRequest()
                .withMessageOrKey("ASSERT_BOOKING_CURRENCY_MATCHES_TRAVEL")
                .thatTargets("Bookings.Currency_code");
    }

}
