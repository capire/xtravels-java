package sap.capire.xtravels.handler;

import cds.gen.sap.capire.flights.data.Data;
import cds.gen.sap.capire.flights.data.Flights_;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.SelectableValue;
import com.sap.cds.ql.cqn.CqnElementRef;
import com.sap.cds.ql.cqn.CqnSelectListItem;
import com.sap.cds.ql.cqn.Modifier;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

@Component
@ServiceName(value = "*", type = Data.class)
class Workarounds implements EventHandler {

  @Before(entity = Flights_.CDS_NAME)
  void stabilize(CdsReadEventContext context) {
    context.setCqn(
        CQL.copy(
            context.getCqn(),
            new Modifier() {

              @Override
              public CqnSelectListItem selectListValue(SelectableValue value, String alias) {
                if (value.isRef() && alias == null) {
                  CqnElementRef ref = value.asRef();
                  if (ref.size() > 1) {
                    alias = ref.path();
                  }
                }
                return Modifier.super.selectListValue(value, alias);
              }
            }));
  }
}
