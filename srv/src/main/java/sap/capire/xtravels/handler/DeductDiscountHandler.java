package sap.capire.xtravels.handler;

import cds.gen.travelservice.TravelService;
import cds.gen.travelservice.TravelService_;
import cds.gen.travelservice.Travels;
import cds.gen.travelservice.TravelsDeductDiscountContext;
import cds.gen.travelservice.Travels_;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
@ServiceName(TravelService_.CDS_NAME)
class DeductDiscountHandler implements EventHandler {

  private final TravelService service;

  DeductDiscountHandler(TravelService service) {
    this.service = service;
  }

  @On
  Travels deductDiscount(Travels_ ref, final TravelsDeductDiscountContext context) {
    var travel = service.run(Select.from(ref)).single();
    BigDecimal discount = BigDecimal.valueOf(context.getPercent() / 100.0);

    travel.setBookingFee(
        travel
            .getBookingFee()
            .subtract(travel.getBookingFee().multiply(discount))
            .round(new MathContext(3)));
    travel.setTotalPrice(
        travel
            .getTotalPrice()
            .subtract(travel.getTotalPrice().multiply(discount))
            .round(new MathContext(3)));

    Travels update = Travels.create();
    update.setTotalPrice(travel.getTotalPrice());
    update.setBookingFee(travel.getBookingFee());
    service.run(Update.entity(ref).data(update).hint("@readonly", false));

    return travel;
  }
}
