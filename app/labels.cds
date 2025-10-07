using { sap.capire.travels as schema } from '../db/schema';

//
// annotations that control rendering of fields and labels
//



// Required to allow fetching Travel.Agency.Name and Travel.Customer.LastName
annotate schema.TravelAgencies with @cds.autoexpose;
annotate schema.Passengers with @cds.autoexpose;

annotate schema.Travels with @title: '{i18n>Travel}' {
  ID          @title: '{i18n>Travel}';
  BeginDate   @title: '{i18n>BeginDate}';
  EndDate     @title: '{i18n>EndDate}';
  Description @title: '{i18n>Description}';
  BookingFee  @title: '{i18n>BookingFee}'    @Measures.ISOCurrency: Currency_code;
  TotalPrice  @title: '{i18n>TotalPrice}'    @Measures.ISOCurrency: Currency_code;
  Customer    @title: '{i18n>Customer}'      @Common: { Text: Customer.LastName, TextArrangement : #TextOnly };
  Agency      @title: '{i18n>Agency}'        @Common: { Text: Agency.Name, TextArrangement : #TextOnly };
  Status      @title: '{i18n>TravelStatus}'
}

annotate schema.TravelStatus {
  code @title: '{i18n>TravelStatus}'
    @Common.Text: name
    @UI.ValueCriticality: [
      { Criticality: #Positive, Value: 'A', },
      { Criticality: #Critical, Value: 'O',  },
      { Criticality: #Negative, Value: 'X',  }
    ]
}

annotate schema.Bookings with @title: '{i18n>Booking}' {
  Travel @UI.Hidden;
  Pos @title: '{i18n>BookingID}';
  BookingDate @title: '{i18n>BookingDate}';
  Flight @title: '{i18n>Flight}';
  Currency @title: '{i18n>CurrencyCode}';
  FlightPrice  @title: '{i18n>FlightPrice}'  @Measures.ISOCurrency: Currency_code;
}

annotate schema.Bookings.Supplements with @title: '{i18n>BookingSupplement}' {
  ID  @title: '{i18n>BookingSupplementID}';
  booked        @title: '{i18n>SupplementID}'  @Common.Text: booked.descr;
  Price             @title: '{i18n>Price}'         @Measures.ISOCurrency: Currency_code;
  Currency          @title: '{i18n>CurrencyCode}';
}

annotate schema.TravelAgencies with @title: '{i18n>TravelAgency}' {
  ID           @title: '{i18n>Agency}'      @Common.Text: Name;
  Name         @title: '{i18n>Agency}';
  Street       @title: '{i18n>Street}';
  PostalCode   @title: '{i18n>PostalCode}';
  City         @title: '{i18n>City}';
  Country      @title: '{i18n>CountryCode}';
  PhoneNumber  @title: '{i18n>PhoneNumber}';
  EMailAddress @title: '{i18n>EMailAddress}';
  WebAddress   @title: '{i18n>WebAddress}';
}

annotate schema.Passengers with @title: '{i18n>Passenger}' {
  ID           @title: '{i18n>Customer}'    @Common.Text: LastName;
  FirstName    @title: '{i18n>FirstName}';
  LastName     @title: '{i18n>LastName}';
  Title        @title: '{i18n>Title}';
  Street       @title: '{i18n>Street}';
  PostalCode   @title: '{i18n>PostalCode}';
  City         @title: '{i18n>City}';
  Country      @title: '{i18n>CountryCode}';
  PhoneNumber  @title: '{i18n>PhoneNumber}';
  EMailAddress @title: '{i18n>EMailAddress}';
}


using sap.capire.flights.data;

annotate data.Airlines with @title: '{i18n>Airline}' {
  ID  @title: '{i18n>AirlineID}'  @Common.Text: name;
  name @title: '{i18n>Name}';
  currency @title: '{i18n>CurrencyCode}';
}

annotate data.Flights with @title: '{i18n>Flight}' {
  aircraft        @title: '{i18n>PlaneType}';
  date            @title: '{i18n>FlightDate}';
  price           @title: '{i18n>Price}'        @Measures.ISOCurrency: currency_code;
  currency        @title: '{i18n>CurrencyCode}';
  maximum_seats   @title: '{i18n>MaximumSeats}';
  occupied_seats  @title: '{i18n>OccupiedSeats}';
}

annotate data.Supplements with @title: '{i18n>Supplement}' {
  ID @title: '{i18n>SupplementID}'  @Common.Text: descr;
  price        @title: '{i18n>Price}'         @Measures.ISOCurrency: currency_code;
  currency @title: '{i18n>CurrencyCode}';
  descr  @title: '{i18n>Description}';
}
