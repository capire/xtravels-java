package sap.capire.xtravels.handler;

import static cds.gen.travelservice.TravelService_.TRAVELS;
import static com.sap.cds.services.cds.CqnService.EVENT_CREATE;
import static com.sap.cds.services.draft.DraftService.EVENT_DRAFT_NEW;

import cds.gen.travelservice.Bookings;
import cds.gen.travelservice.Bookings_;
import cds.gen.travelservice.TravelService;
import cds.gen.travelservice.TravelService_;
import cds.gen.travelservice.Travels;
import com.sap.cds.ql.Select;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

@Component
@ServiceName(TravelService_.CDS_NAME)
class CreationHandler implements EventHandler {

  private final TravelService service;

  CreationHandler(TravelService service) {
    this.service = service;
  }

  // Fill in alternative keys as consecutive numbers for new Travels, Bookings, and Supplements.
  // Note: We need to handle both draft creation and final creation to avoid unique constraint violations.
  @Before(event = {EVENT_CREATE, EVENT_DRAFT_NEW})
  void calculateTravelId(final Travels travel) {
    // Only set ID if it's not already set (to avoid overwriting existing IDs)
    if (travel.getId() == null || travel.getId() == 0) {
      synchronized (this) {
        var result =
            service.run(
                Select.from(TRAVELS)
                    .columns(t -> t.ID().max().as("maxID")));
        var maxIdValue = result.single().get("maxID");
        int maxId = maxIdValue == null ? 0 : (int) maxIdValue;
        travel.setId(++maxId);
      }
    }
  }

  // Fill in IDs as sequence numbers -> could be automated by auto-generation
  @Before(event = EVENT_DRAFT_NEW)
  void calculateBookingPos(Bookings_ ref, final Bookings booking) {
    var result = service.run(Select.from(ref).columns(t -> t.Pos().max().as("maxPos")));
    var maxPos = result.single().get("maxPos");
    if (maxPos == null) {
      booking.setPos(1);
    } else {
      int pos = (int) maxPos;
      booking.setPos(++pos);
    }
  }
}
