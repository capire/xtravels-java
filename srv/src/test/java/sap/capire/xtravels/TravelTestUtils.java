package sap.capire.xtravels;

import java.math.BigDecimal;
import java.time.LocalDate;

import cds.gen.travelservice.Bookings;
import cds.gen.travelservice.Travels;

public class TravelTestUtils {

    public static Travels createTravelData(String testName) {
        Travels travel = Travels.create();
        travel.setIsActiveEntity(true);
        travel.setDescription(testName + " - Test Travel");
        travel.setBeginDate(LocalDate.of(2024, 6, 1));
        travel.setEndDate(LocalDate.of(2024, 6, 14));
        travel.setBookingFee(BigDecimal.valueOf(100));
        travel.setCurrencyCode("EUR");
        travel.setAgencyId("070001");
        travel.setCustomerId("000001");

        return travel;
    }

    public static Travels createTravelData() {
        return createTravelData("test");
    }

    public static Bookings createBookingData(Travels travel) {
        Bookings booking = Bookings.create();
        booking.setFlightId("SW1537");
        booking.setFlightDate(LocalDate.of(2024, 6, 07));
        booking.setFlightPrice(BigDecimal.valueOf(200.0));
        booking.setCurrencyCode("EUR");
        booking.setBookingDate(LocalDate.of(2020, 1, 01));

        return booking;
    }

}
