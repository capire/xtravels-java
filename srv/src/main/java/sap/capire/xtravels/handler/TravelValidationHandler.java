package sap.capire.xtravels.handler;

import static cds.gen.travelservice.TravelService_.TRAVELS;
import static com.sap.cds.services.cds.CqnService.EVENT_CREATE;
import static com.sap.cds.services.cds.CqnService.EVENT_UPDATE;
import static com.sap.cds.services.draft.DraftService.EVENT_DRAFT_PATCH;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.Row;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.messages.MessageTarget;

import cds.gen.travelservice.TravelService;
import cds.gen.travelservice.TravelService_;
import cds.gen.travelservice.Travels;
import cds.gen.travelservice.Travels_;

@Component
@ServiceName(TravelService_.CDS_NAME)
public class TravelValidationHandler implements EventHandler {

    @Autowired
    TravelService ts;

    @Before(event = { EVENT_CREATE, EVENT_UPDATE, EVENT_DRAFT_PATCH })
    public void validateTravelBeforeWrite(Travels_ ref, Travels travel, EventContext ctx) {
        if (!travel.containsKey(Travels.BEGIN_DATE) && !travel.containsKey(Travels.END_DATE)) {
            return;
        }

        ref = travelRef(ref, travel);

        LocalDate beginDate = travel.getBeginDate();
        LocalDate endDate = travel.getEndDate();

        if (beginDate != null && endDate == null) {
            Travels beforeImage = ts.run(Select.from(ref).columns(Travels_::EndDate)).single();
            endDate = beforeImage.getEndDate();
        }

        if (endDate != null && beginDate == null) {
            Travels beforeImage = ts.run(Select.from(ref).columns(Travels_::BeginDate)).single();
            endDate = beforeImage.getBeginDate();
        }

        if (beginDate != null && endDate != null && beginDate.isAfter(endDate)) {
            ctx.getMessages().error("ASSERT_ENDDATE_AFTER_BEGINDATE").target(TRAVELS, t -> t.BeginDate())
                    .additionalTargets(MessageTarget.create(TRAVELS, t -> t.EndDate()));
        }
    }

    // @After(event = { EVENT_DRAFT_PATCH, EVENT_CREATE, EVENT_UPDATE })
    public void validateTravelAfterWriteOnDB(Travels_ ref, Travels travel, EventContext ctx) {
        if (!travel.containsKey(Travels.BEGIN_DATE) && !travel.containsKey(Travels.END_DATE)) {
            return;
        }

        ref = travelRef(ref, travel);

        CqnSelect check = Select.from(ref).columns(t -> t.BeginDate().gt(t.EndDate()).as("beginAfterEnd"));
        Row result = ts.run(check).single();
        if (result.get("beginAfterEnd").equals(Boolean.TRUE)) {
            ctx.getMessages().error("ASSERT_ENDDATE_AFTER_BEGINDATE").target(TRAVELS, t -> t.BeginDate())
                    .additionalTargets(MessageTarget.create(TRAVELS, t -> t.EndDate()));
        }

    }

    private static Travels_ travelRef(Travels_ ref, Travels travel) {
        Integer id = travel.getId();
        if (id != null) {
            boolean isActiveEntity = travel.getIsActiveEntity() == Boolean.FALSE ? false : true;
            ref.filter(t -> t.ID().eq(id).and(t.IsActiveEntity().eq(isActiveEntity)));
        }

        return ref;
    }

    @After(event = { EVENT_CREATE, EVENT_UPDATE })
    @HandlerOrder(HandlerOrder.LATE)
    public void throwIfError(Travels travel, EventContext ctx) {
        ctx.getMessages().throwIfError();
    }

}
