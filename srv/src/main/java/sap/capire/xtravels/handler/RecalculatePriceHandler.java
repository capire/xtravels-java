package sap.capire.xtravels.handler;

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
import com.sap.cds.ql.Value;
import com.sap.cds.ql.cqn.CqnSelectListValue;
import com.sap.cds.ql.cqn.CqnStructuredTypeRef;
import com.sap.cds.ql.cqn.CqnValue;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static cds.gen.travelservice.TravelService_.TRAVELS;
import static com.sap.cds.services.cds.CqnService.EVENT_CREATE;

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

    Function<Bookings_, CqnValue> bookingCost =
        b -> orZero(b.FlightPrice()).plus(orZero(b.Supplements().sum(s -> s.Price())));
    Function<Travels_, CqnSelectListValue> travelCost =
        t -> t.BookingFee().plus(orZero(t.Bookings().sum(bookingCost))).as(Travels.TOTAL_PRICE);

    var aggregation = Select.from(travel).columns(travelCost);
    BigDecimal totalPrice = service.run(aggregation).single().getTotalPrice();

    service.run(
        Update.entity(travel).data(Travels.TOTAL_PRICE, totalPrice).hint("@readonly", false));
  }

    @After(event = EVENT_CREATE, entity = Travels_.CDS_NAME)
    void setTotalPriceAfterCreation(Travels travels) {

        //travel is created with the total price being the booking fee in case no total price is set
        if (travels.getTotalPrice() == null || travels.getTotalPrice().equals(BigDecimal.ZERO)) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put(Travels.ID, travels.getId());
            updateData.put(Travels.TOTAL_PRICE, travels.getBookingFee());

            service.run(
                    Update.entity(Travels_.class).data(updateData).hint("@readonly", false));
        }
    }

  private Value<Number> orZero(Value<? extends Number> value) {
    return CQL.func("coalesce", value, CQL.constant(0));
  }
}
