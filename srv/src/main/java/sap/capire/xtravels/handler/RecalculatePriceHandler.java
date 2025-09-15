package sap.capire.xtravels.handler;

import static cds.gen.travelservice.TravelService_.TRAVELS;
import static com.sap.cds.ql.CQL.func;

import cds.gen.travelservice.Bookings;
import cds.gen.travelservice.Bookings_;
import cds.gen.travelservice.TravelService;
import cds.gen.travelservice.TravelService_;
import cds.gen.travelservice.Travels;
import cds.gen.travelservice.Travels_;
import com.sap.cds.CdsData;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnStructuredTypeRef;
import com.sap.cds.ql.cqn.CqnValue;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

// Update a Travel's TotalPrice whenever its BookingFee is modified,
// or when a nested Booking is deleted or its FlightPrice is modified,
// or when a nested Supplement is deleted or its Price is modified.
// -> should be automated by Calculated Elements + auto-GROUP BY
@Component
@ServiceName(TravelService_.CDS_NAME)
class RecalculatePriceHandler implements EventHandler {

  private final TravelService service;

  RecalculatePriceHandler(TravelService service) {
    this.service = service;
  }

  @After(entity = {Travels_.CDS_NAME, Bookings_.CDS_NAME, Bookings_.Supplements_.CDS_NAME})
  void updateTotalsOnPatch(CqnStructuredTypeRef ref, CdsData data, DraftPatchEventContext context) {
    if (!(data.containsKey(Travels.BOOKING_FEE)
        || data.containsKey(Bookings.FLIGHT_PRICE)
        || data.containsKey(Bookings.Supplements.PRICE))) return;
    updateTotals(ref, context);
  }

  @After(entity = {Bookings_.CDS_NAME, Bookings_.Supplements_.CDS_NAME})
  void updateTotalsOnDelete(CqnStructuredTypeRef ref, DraftCancelEventContext context) {
    updateTotals(ref, context);
  }

  private void updateTotals(CqnStructuredTypeRef ref, EventContext context) {
    var travel = CQL.entity(TRAVELS, CQL.to(ref.rootSegment()));

    CqnValue zero = CQL.constant(0);
    var fee =
        service.run(
            Select.from(travel)
                .columns(t -> func("coalesce", t.BookingFee(), zero).as(Travels.BOOKING_FEE)));
    var bookings =
        service.run(
            Select.from(travel.Bookings())
                .columns(
                    b -> func("coalesce", b.FlightPrice().sum(), zero).as(Bookings.FLIGHT_PRICE)));
    var supplements =
        service.run(
            Select.from(travel.Bookings().Supplements())
                .columns(
                    s -> func("coalesce", s.Price().sum(), zero).as(Bookings.Supplements.PRICE)));
    BigDecimal totalPrice =
        fee.single()
            .getBookingFee()
            .add(bookings.single().getFlightPrice())
            .add(supplements.single().getPrice());
    service.run(
        Update.entity(travel).data(Travels.TOTAL_PRICE, totalPrice).hint("@readonly", false));
  }
}
