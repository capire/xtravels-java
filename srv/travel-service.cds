using { sap, sap.capire.travels as db, sap.capire.xflights } from '../db/schema';

@path: 'travel' service TravelService {

  entity Travels as projection on db.Travels actions {
    action acceptTravel();
    action rejectTravel();
    action reopenTravel();
    action deductDiscount( percent: Percentage not null ) returns Travels;
  }

  // Also expose Flights and Currencies for travel booking UIs and Value Helps
  @readonly entity Flights as projection on xflights.Flights;
  @readonly entity Supplements as projection on xflights.Supplements;
  @readonly entity Currencies as projection on sap.common.Currencies;
}

type Percentage : Integer @assert.range: [1,100];
