package sap.capire.xtravels.handler;

import static cds.gen.travelservice.TravelService_.TRAVELS;

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
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnStructuredTypeRef;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

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
    updateTotals(ref);
  }

  @After(entity = {Bookings_.CDS_NAME, Bookings_.Supplements_.CDS_NAME})
  void updateTotalsOnDelete(CqnStructuredTypeRef ref, DraftCancelEventContext context) {
    updateTotals(ref);
  }

  private void updateTotals(CqnStructuredTypeRef ref) {
    var travel = CQL.entity(TRAVELS, CQL.to(ref.rootSegment()));

    CqnSelect aggregation = Select.from(travel).columns(
              t -> t.BookingFee()
                      .plus(t.Bookings().sum(
                              b -> b.FlightPrice().plus(b.Supplements().sum(s -> s.Price()))
                      ))
              .as("total"));
    BigDecimal totalPrice = (BigDecimal) service.run(aggregation).single().get("total");
    totalPrice = Objects.requireNonNullElse(totalPrice, BigDecimal.ZERO);

    service.run(
        Update.entity(travel).data(Travels.TOTAL_PRICE, totalPrice).hint("@readonly", false));
  }
}
