using { sap, sap.capire.travels as db } from '../db/schema';

@path: 'travel' service TravelService {

  @(restrict: [
    { grant: 'READ', to: 'authenticated-user'},
    { grant: ['reviewTravel','reopenTravel','blockTravel','unblockTravel','acceptTravel','rejectTravel','deductDiscount'], to: 'reviewer'},
    { grant: ['*'], to: 'processor'},
    { grant: ['*'], to: 'admin'}
  ])
  entity Travels as projection on db.Travels actions {
    action reviewTravel();
    action reopenTravel();
    action blockTravel();
    action unblockTravel();
    action acceptTravel();
    action rejectTravel();
    action deductDiscount( percent: Percentage not null ) returns Travels;
  }

  // Define flow for Travels
  annotate Travels with @flow.status: Status actions {
    reviewTravel    @from: #Open               @to: #InReview;
    reopenTravel    @from: #InReview           @to: #Open;
    blockTravel     @from: [#Open, #InReview]  @to: #Blocked;
    unblockTravel   @from: #Blocked            @to: $flow.previous;
    acceptTravel    @from: #InReview           @to: #Accepted;
    rejectTravel    @from: #InReview           @to: #Canceled;
    deductDiscount  @from: #Open;
  };

  // Also expose Flights and Currencies for travel booking UIs and Value Helps
  @readonly entity Flights as projection on db.masterdata.Flights;
  @readonly entity Supplements as projection on db.masterdata.Supplements;
  @readonly entity Currencies as projection on sap.common.Currencies;
}

type Percentage : Integer @assert.range: [1,100];
