using { sap, sap.capire.travels as db } from '../db/schema';

@path: 'travel' service TravelService {

  @(restrict: [
    { grant: 'READ', to: 'authenticated-user'},
    { grant: ['rejectTravel','acceptTravel','deductDiscount'], to: 'reviewer'},
    { grant: ['*'], to: 'processor'},
    { grant: ['*'], to: 'admin'}
  ])
  entity Travels as projection on db.Travels actions {
    action rejectTravel();
    action acceptTravel();
    action deductDiscount( percent: Percentage not null ) returns Travels;
  }

  // Define flow for Travels
  // NOTE: @flow.status on entity-level makes the target element read-only
  annotate Travels with @flow.status: Status actions {
    rejectTravel    @from: #Open  @to: #Canceled;
    acceptTravel    @from: #Open  @to: #Accepted;
    deductDiscount  @from: #Open;
  };

  // Also expose Flights and Currencies for travel booking UIs and Value Helps
  @readonly entity Flights as projection on db.masterdata.Flights;
  @readonly entity Supplements as projection on db.masterdata.Supplements;
  @readonly entity Currencies as projection on sap.common.Currencies;
}

type Percentage : Integer @assert.range: [1,100];
