package sap.capire.xtravels.handler;

import static cds.gen.sap.capire.travels.TravelsModel_.BOOKINGS;
import static cds.gen.sap.capire.travels.masterdata.Masterdata_.FLIGHTS;
import static cds.gen.sap.capire.travels.masterdata.Masterdata_.SUPPLEMENTS;
import static com.sap.cds.services.cds.CqnService.EVENT_CREATE;
import static com.sap.cds.services.cds.CqnService.EVENT_UPDATE;
import static com.sap.cds.services.cds.CqnService.EVENT_UPSERT;
import static com.sap.cds.services.draft.DraftService.EVENT_DRAFT_NEW;
import static com.sap.cds.services.draft.DraftService.EVENT_DRAFT_PATCH;

import cds.gen.sap.capire.flights.data.Data;
import cds.gen.sap.capire.travels.masterdata.Flights;
import cds.gen.sap.capire.travels.masterdata.Supplements;
import com.google.common.collect.Maps;
import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.impl.DataProcessor;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Selectable;
import com.sap.cds.ql.Upsert;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.application.ApplicationLifecycleService;
import com.sap.cds.services.application.ApplicationPreparedEventContext;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceConfiguration;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceConfiguration.TimeLimiterConfiguration;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceDecorator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ServiceName(value = "*", type = ApplicationService.class)
class FederationHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(FederationHandler.class);
  private static final ResilienceConfiguration config =
      ResilienceConfiguration.of(FederationHandler.class)
          .timeLimiterConfiguration(TimeLimiterConfiguration.of(Duration.ofSeconds(10)));

  private final PersistenceService db;
  private final Data dataService;
  private final CdsRuntime runtime;

  FederationHandler(PersistenceService db, Data dataService, CdsRuntime runtime) {
    this.db = db;
    this.dataService = dataService;
    this.runtime = runtime;
  }

  @On(serviceType = ApplicationLifecycleService.class)
  void initialLoad(ApplicationPreparedEventContext context) {
    // initial load for Flights
    logger.info("Performing initial load for Flights");
    var bookingFlights =
        db.run(
            Select.from(BOOKINGS)
                .columns(b -> b.Flight_ID().as(Flights.ID), b -> b.Flight_date().as(Flights.DATE))
                .distinct());
    var remoteFlights =
        dataService.run(
            Select.from(FLIGHTS).where(CQL.in(List.of(Flights.ID, Flights.DATE), bookingFlights)));
    db.run(Upsert.into(FLIGHTS).entries(remoteFlights));

    // intial load for Supplements
    logger.info("Performing initial load for Supplements");
    var bookingSupplements =
        db.run(
            Select.from(BOOKINGS, b -> b.Supplements())
                .columns(b -> b.booked_ID().as(Supplements.ID))
                .distinct());
    var remoteSupplements =
        dataService.run(
            Select.from(SUPPLEMENTS).where(CQL.in(List.of(Supplements.ID), bookingSupplements)));
    db.run(Upsert.into(SUPPLEMENTS).entries(remoteSupplements));
  }

  @On
  void readValueHelp(CdsReadEventContext context) {
    CdsEntity target = context.getTarget();
    if (isFederated(target) && isValueHelpRequest(context.getCqn())) {
      Supplier<Result> remote =
          () -> {
            logger.info("Delegating ValueHelp for '{}'", target.getQualifiedName());
            return dataService.run(context.getCqn());
          };
      Function<Throwable, Result> replicated =
          (t) -> {
            logger.warn("Serving replicas of '{}'", target.getQualifiedName());
            return db.run(context.getCqn());
          };
      context.setResult(ResilienceDecorator.executeSupplier(remote, config, replicated));
    }
  }

  @After(event = {EVENT_CREATE, EVENT_UPDATE, EVENT_UPSERT, EVENT_DRAFT_NEW, EVENT_DRAFT_PATCH})
  void replicate(EventContext context, List<CdsData> dataList) {
    DataProcessor.create()
        .action(this::replicateAssociations)
        .process(dataList, context.getTarget());
  }

  void replicateAssociations(CdsStructuredType type, Map<String, Object> data) {
    Consumer<CdsElement> replicateTarget =
        element -> {
          CdsAssociationType assoc = element.getType().as(CdsAssociationType.class);
          CdsEntity target = assoc.getTarget();
          if (isFederated(target)) {
            List<String> fks = assoc.refs().map(r -> r.path()).toList();
            Map<String, Object> fkValues = Maps.filterKeys(data, fks::contains);
            if (fks.size() == target.keyElements().count() && !isReplicated(target, fkValues)) {
              replicateInstance(target, fkValues);
            }
          }
        };
    type.associations().forEach(replicateTarget);
  }

  private boolean isFederated(CdsEntity entity) {
    return entity.getAnnotationValue("@federated", false);
  }

  private boolean isValueHelpRequest(CqnSelect select) {
    return select.ref().segments().size() == 1;
  }

  private boolean isReplicated(CdsEntity entity, Map<String, ?> keys) {
    var exists = Select.from(entity).columns(CQL.constant(1).as("one")).matching(keys);
    return db.run(exists).rowCount() > 0;
  }

  private void replicateInstance(CdsEntity entity, Map<String, ?> keys) {
    var select = Select.from(entity).columns(expandCompositions(entity)).matching(keys);
    // read without locale, to replicated localized data correctly
    Result remote =
        runtime
            .requestContext()
            .modifyParameters(p -> p.setLocale(null))
            .run(
                requestContext -> {
                  return dataService.run(select);
                });
    if (remote.rowCount() == 1) {
      logger.info("Replicating '{}' with keys '{}'", entity.getQualifiedName(), keys);
      db.run(Upsert.into(entity).entry(remote.single()));
    }
  }

  private List<Selectable> expandCompositions(CdsEntity entity) {
    List<Selectable> columns = new ArrayList<>(List.of(CQL.star()));
    entity
        .compositions()
        .forEach(
            co -> {
              CdsEntity target = entity.getTargetOf(co.getName());
              columns.add(CQL.to(co.getName()).expand(expandCompositions(target)));
            });
    return columns;
  }
}
