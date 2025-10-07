//
// Consumption views for master data imported from xflights...
//

using { sap.capire.flights.data as external } from 'capire/xflights-data';
namespace sap.capire.travels.masterdata;

@cds.persistence.table
@federated entity Flights as projection on external.Flights {
  *,
  airline.icon     as icon,
  airline.name     as airline,
  origin.name      as origin,
  destination.name as destination,
}

@cds.persistence.table
@federated entity Supplements as projection on external.Supplements {
  ID, type, descr, price, currency
}
