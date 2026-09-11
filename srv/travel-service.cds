using { sap, sap.capire.travels as db, sap.capire.xflights } from '../db/schema';

@agent @odata @path: 'travel' service TravelService {

  entity Travels as projection on db.Travels actions {
    action acceptTravel();
    action rejectTravel();
    action reopenTravel();
    action deductDiscount( percent: Percentage not null ) returns Travels;
  }

  @description: 'Create a new travel. Returns the assigned ID of the travel.'
  action createTravel(
    @description: 'Description'
    Description: Travels:Description not null,
    @description: 'Begin date in YYYY-MM-DD format'
    BeginDate: Date not null,
    @description: 'End date in YYYY-MM-DD format'
    EndDate: Date not null,
    @description: 'Currency code (default: EUR). Query Currencies to find valid currency codes.'
    currencyCode: Travels:Currency.code,
    @description: 'Booking fee amount (default: 0)'
    BookingFee: db.Price) returns Travels:ID;

  @description: ```
  Update the header data of an existing travel: description, begin date, end date.
  Only the fields you provide are updated.
  ```
  action updateTravel(
    @description: 'The integer ID of the travel'
    TravelID: Travels:ID not null,
    @description: 'New description'
    Description: Travels:Description,
    @description: 'New begin date in YYYY-MM-DD format'
    BeginDate: Date,
    @description: 'New end date in YYYY-MM-DD format'
    EndDate: Date);

  @description: ```
  Add a flight booking to an existing travel. Before calling this query Flights to
  find a valid flight and obtain its ID and date. The FlightID is a string like
  'LH0400', the FlightDate is the flight's departure date in YYYY-MM-DD format.
  ```
  action addFlightToTravel(
    @description: 'The integer ID of the travel'
    TravelID: Travels:ID not null,
    @description: 'The flight ID (e.g. ''LH0400'')'
    FlightID: Flights:ID not null,
    @description: 'The flight date in YYYY-MM-DD format'
    FlightDate: Flights:date not null);

  // Also expose Flights and Currencies for travel booking UIs and Value Helps
  @readonly entity Flights as projection on xflights.Flights;
  @readonly entity Supplements as projection on xflights.Supplements;
  @readonly entity Currencies as projection on sap.common.Currencies;
}

type Percentage : Integer @assert.range: [1,100];
