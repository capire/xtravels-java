package sap.capire.xtravels.handler;

import static cds.gen.travelservice.TravelService_.FLIGHTS;
import static cds.gen.travelservice.TravelService_.TRAVELS;

import cds.gen.travelservice.AddFlightToTravelContext;
import cds.gen.travelservice.Bookings;
import cds.gen.travelservice.CreateTravelContext;
import cds.gen.travelservice.TravelService;
import cds.gen.travelservice.TravelService_;
import cds.gen.travelservice.Travels;
import cds.gen.travelservice.UpdateTravelContext;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
@ServiceName(TravelService_.CDS_NAME)
public class TravelServiceTools implements EventHandler {

  private final TravelService service;

  public TravelServiceTools(TravelService service) {
    this.service = service;
  }

  @On
  public void addFlightToTravel(AddFlightToTravelContext context) {
    var flight =
        context
            .getService()
            .run(
                Select.from(FLIGHTS)
                    .where(
                        f ->
                            f.ID()
                                .eq(context.getFlightID())
                                .and(f.date().eq(context.getFlightDate())))
                    .columns(f -> f.price(), f -> f.currency_code()))
            .single();

    Bookings booking = Bookings.create();
    booking.setFlightId(context.getFlightID());
    booking.setFlightDate(context.getFlightDate());
    booking.setFlightPrice(flight.getPrice());
    booking.setCurrencyCode(flight.getCurrencyCode());
    booking.setBookingDate(LocalDate.now());
    service.run(
        Insert.into(
                TRAVELS,
                t ->
                    t.filter(t.ID().eq(context.getTravelID()).and(t.IsActiveEntity().eq(true)))
                        .Bookings())
            .entry(booking));
    context.setCompleted();
  }

  @On
  public void updateTravelHeader(UpdateTravelContext context) {
    Travels patch = Travels.create();
    if (context.getDescription() != null) patch.setDescription(context.getDescription());
    if (context.getBeginDate() != null) patch.setBeginDate(context.getBeginDate());
    if (context.getEndDate() != null) patch.setEndDate(context.getEndDate());
    service.run(
        Update.entity(TRAVELS)
            .where(t -> t.ID().eq(context.getTravelID()).and(t.IsActiveEntity().eq(true)))
            .data(patch));
    context.setCompleted();
  }

  @On
  public int createTravel(CreateTravelContext context) {
    Travels travel = Travels.create();
    travel.setDescription(context.getDescription());
    travel.setBeginDate(context.getBeginDate());
    travel.setEndDate(context.getEndDate());
    travel.setCurrencyCode(context.getCurrencyCode() != null ? context.getCurrencyCode() : "EUR");
    travel.setBookingFee(
        context.getBookingFee() != null ? context.getBookingFee() : BigDecimal.ZERO);

    return service.run(Insert.into(TRAVELS).entry(travel)).single(Travels.class).getId();
  }
}
