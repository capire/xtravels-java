package sap.capire.xtravels;

import cds.gen.travelservice.Bookings;
import cds.gen.travelservice.Travels;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TestData {

  public static Travels createTravelData() {
    Travels travel = Travels.create();
    travel.setIsActiveEntity(true);
    travel.setDescription("Test Travel");
    travel.setBeginDate(LocalDate.of(2024, 6, 1));
    travel.setEndDate(LocalDate.of(2024, 6, 14));
    travel.setBookingFee(BigDecimal.valueOf(100));
    travel.setCurrencyCode("EUR");
    travel.setAgencyId("070001");
    travel.setCustomerId("000001");

    return travel;
  }

  public static Bookings createBookingData() {
    Bookings booking = Bookings.create();
    booking.setFlightId("GA0322");
    booking.setFlightDate(LocalDate.of(2024, 6, 2));
    booking.setFlightPrice(BigDecimal.valueOf(1103));
    booking.setCurrencyCode("EUR");
    return booking;
  }

  public static Bookings.Supplements createSupplementData() {
    Bookings.Supplements supplement = Bookings.Supplements.create();
    supplement.setBookedId("bv-0001");
    supplement.setPrice(new BigDecimal("2.30"));
    supplement.setCurrencyCode("EUR");
    return supplement;
  }
}
