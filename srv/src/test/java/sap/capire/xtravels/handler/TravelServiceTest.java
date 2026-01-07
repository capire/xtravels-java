package sap.capire.xtravels.handler;

import static cds.gen.travelservice.TravelService_.TRAVELS;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static sap.capire.xtravels.TravelTestUtils.createBookingData;
import static sap.capire.xtravels.TravelTestUtils.createTravelData;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.services.ServiceException;

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

        assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> srv.run(insert)).satisfies(badRequest("Description too short", "Description"));
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_CustomerIsNull() {
        travel.setCustomerId(null);
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> srv.run(insert)).satisfies(badRequest("Provide the missing value.", "Customer"));
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_CustomerDoesNotExist() {
        travel.setCustomerId("bad-ID");
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> srv.run(insert)).satisfies(badRequest("Customer does not exist", "Customer_ID"));
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_AgencyDoesNotExist() {
        travel.setAgencyId("bad-ID");
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> srv.run(insert)).satisfies(badRequest("Agency does not exist", "Agency"));
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_EndDateBeforeBeginDate() {
        travel.setEndDate(travel.getBeginDate().minus(1, DAYS));
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> srv.run(insert))
                .satisfies(badRequest("End date must be after begin date"))
                .satisfies(targets("BeginDate", "EndDate"));
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_NegativeBookingFee() {
        travel.setBookingFee(BigDecimal.valueOf(-1.0));
        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> srv.run(insert))
                .satisfies(badRequest("Booking fee cannot be negative", "BookingFee"));
    }

    // Bookings

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_withBooking_BookingDateNotWithinTravelDate() {
        Bookings booking = createBookingData(travel);
        booking.setFlightDate(travel.getEndDate().plus(2, DAYS));
        travel.setBookings(List.of(booking));

        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> srv.run(insert))
                .satisfies(badRequest("All bookings must be within the travel period", "Bookings.Flight_date"));
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_withBooking_NegativeFlightPrice() {
        Bookings booking = createBookingData(travel);
        booking.setFlightPrice(BigDecimal.valueOf(-1.0));
        travel.setBookings(List.of(booking));

        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> srv.run(insert))
                .satisfies(badRequest("Flight price must be a positive value", "Bookings.FlightPrice"));
    }

    @Test
    @WithMockUser("admin")
    public void testCreateTravel_withBooking_CurrencyMismatch() {
        Bookings booking = createBookingData(travel);
        booking.setCurrencyCode("DKK");
        travel.setBookings(List.of(booking));

        CqnInsert insert = Insert.into(TRAVELS).entry(travel);

        assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> srv.run(insert))
                .satisfies(
                        badRequest("All bookings must use the same currency as the travel", "Bookings.Currency_code"));
    }

    // assertions

    private static ThrowingConsumer<? super ServiceException> badRequest(String localizedMessage) {
        return ex -> {
            assertThat(ex.getLocalizedMessage()).isEqualTo(localizedMessage);
            assertThat(ex.getErrorStatus().getHttpStatus()).isEqualTo(400);
        };
    }

    private static ThrowingConsumer<? super ServiceException> badRequest(String localizedMessage, String target) {
        return ex -> {
            assertThat(ex.getLocalizedMessage()).isEqualTo(localizedMessage);
            assertThat(ex.getErrorStatus().getHttpStatus()).isEqualTo(400);
            assertThat(ex.getMessageTarget().getRef().path()).isEqualTo(target);
            assertThat(ex.getAdditionalTargets()).isEmpty();
        };
    }

    private static ThrowingConsumer<? super ServiceException> targets(String target, String... additional) {
        return ex -> {
            assertThat(ex.getMessageTarget().getRef().path()).isEqualTo(target);
            assertThat(ex.getAdditionalTargets().size()).isEqualTo(additional.length);
            for (int i = 0; i < additional.length; i++) {
                assertThat(ex.getAdditionalTargets().get(i).getRef().path()).isEqualTo(additional[i]);
            }
        };
    }

}
