using { sap.capire.travels as schema } from '../db/schema';

//
// annotations for value helps
//

// annotate schema.TravelStatus with {
//   code @Common: { Text: name, TextArrangement: #TextOnly }
// }

annotate schema.Travels {

  Status @Common.ValueListWithFixedValues;

  Agency @Common.ValueList: {
    CollectionPath : 'TravelAgencies',
    Label : '',
    Parameters : [
      {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Agency_ID, ValueListProperty: 'ID'},  // local data property is the foreign key
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'Name'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'Street'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'PostalCode'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'City'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'CountryCode_code'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'PhoneNumber'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'EMailAddress'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'WebAddress'}
    ]
  };

  Customer @Common.ValueList: {
    CollectionPath : 'Passengers',
    Label : 'Customer ID',
    Parameters : [
      {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Customer_ID, ValueListProperty: 'ID'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'FirstName'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'LastName'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'Title'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'Street'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'PostalCode'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'City'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'CountryCode_code'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'PhoneNumber'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'EMailAddress'}
    ]
  };

  Currency @Common.ValueList: {
    CollectionPath : 'Currencies',
    Label : '',
    Parameters : [
      {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Currency_code, ValueListProperty: 'code'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'name'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'descr'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'symbol'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'minor'}
    ]
  };

}


annotate schema.Bookings {

  Flight {
    @Common.ValueList: {
      CollectionPath : 'Flights',
      Label : '',
      Parameters : [
        {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Flight_ID,   ValueListProperty: 'ID'},
        {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Flight_date, ValueListProperty: 'date'},
        {$Type: 'Common.ValueListParameterOut',   LocalDataProperty: FlightPrice, ValueListProperty: 'price'},
        {$Type: 'Common.ValueListParameterOut',   LocalDataProperty: Currency_code, ValueListProperty: 'currency_code'},
        {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'airline'},
        {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'destination'}
      ]
    }
    ID;
    @Common.ValueList: {
      CollectionPath : 'Flights',
      Label : '',
      Parameters : [
        {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Flight_ID,   ValueListProperty: 'ID'},
        {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Flight_date, ValueListProperty: 'date'},
        {$Type: 'Common.ValueListParameterOut',   LocalDataProperty: FlightPrice, ValueListProperty: 'price'},
        {$Type: 'Common.ValueListParameterOut',   LocalDataProperty: Currency_code, ValueListProperty: 'currency_code'},
        {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'airline'},
        {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'destination'}
      ]
    }
    date;
  };

  Currency @Common.ValueList: {
    CollectionPath : 'Currencies',
    Label : '',
    Parameters : [
      {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Currency_code, ValueListProperty: 'code'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'name'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'descr'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'symbol'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'minor'}
    ]
  };

}


annotate schema.Bookings.Supplements {

  booked @Common.ValueList: {
    CollectionPath : 'Supplements',
    Label : '',
    Parameters : [
    {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: booked_ID, ValueListProperty: 'ID'},
    {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Price,        ValueListProperty: 'price'},
    {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Currency_code, ValueListProperty: 'currency_code'},
    {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'descr'}
    ]
  };

  Currency @Common.ValueList: {
    CollectionPath : 'Currencies',
    Label : '',
    Parameters : [
      {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: Currency_code, ValueListProperty: 'code'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'name'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'descr'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'symbol'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'minor'}
    ]
  };
}


using sap.capire.travels.masterdata as flights;


annotate flights.Flights with @UI.PresentationVariant#SortOrderPV : {    // used in ValueList for Bookings:ConnectionId above
    SortOrder      : [{
      Property   : flightDate,
      Descending : true
    }]
  }
{
  airline @Common.ValueList: {
    CollectionPath : 'Airline',
    Label : '',
    Parameters : [
      {$Type: 'Common.ValueListParameterInOut', LocalDataProperty: AirlineID, ValueListProperty: 'AirlineID'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'Name'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty: 'Currency'}
    ]
  };
};


annotate schema.Passengers {

  Country @Common.ValueList : {
    CollectionPath  : 'Countries',
    Label : '',
    Parameters : [
      {$Type: 'Common.ValueListParameterInOut',       LocalDataProperty : Country_code, ValueListProperty : 'code'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty : 'name'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty : 'descr'}
    ]
  };

}


annotate schema.TravelAgencies {

  Country @Common.ValueList: {
    CollectionPath : 'Countries',
    Label : '',
    Parameters : [
      {$Type: 'Common.ValueListParameterInOut',       LocalDataProperty: CountryCode_code, ValueListProperty: 'code'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty : 'name'},
      {$Type: 'Common.ValueListParameterDisplayOnly', ValueListProperty : 'descr'}
    ]
  };

}
