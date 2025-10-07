using { TravelService } from '../../srv/travel-service';

//
// annotations that control the Fiori layout
//

annotate TravelService.Travels with @UI : {

  Identification : [
    { $Type  : 'UI.DataFieldForAction', Action : 'TravelService.acceptTravel',   Label  : '{i18n>AcceptTravel}'   },
    { $Type  : 'UI.DataFieldForAction', Action : 'TravelService.rejectTravel',   Label  : '{i18n>RejectTravel}'   },
    { $Type  : 'UI.DataFieldForAction', Action : 'TravelService.deductDiscount', Label  : '{i18n>DeductDiscount}' }
  ],

  HeaderInfo : {
    TypeName       : '{i18n>Travel}',
    TypeNamePlural : '{i18n>Travels}',
    Title          : { Value: Description },
    Description    : { Value: ID }
  },

  PresentationVariant : {
    Text           : 'Default',
    Visualizations : ['@UI.LineItem'],
    SortOrder      : [{ Property: ID, Descending: true }]
  },

  // REVISIT: We need to refer to generated foreign keys here, for related value helps
  // to work. Should be able to use associations instead.
  SelectionFields : [
    (Agency.ID),
    (Customer.ID),
    (Status.code),
  ],

  LineItem : [

    { $Type  : 'UI.DataFieldForAction', Action : 'TravelService.acceptTravel',   Label  : '{i18n>AcceptTravel}'   },
    { $Type  : 'UI.DataFieldForAction', Action : 'TravelService.rejectTravel',   Label  : '{i18n>RejectTravel}'   },
    { $Type  : 'UI.DataFieldForAction', Action : 'TravelService.deductDiscount', Label  : '{i18n>DeductDiscount}' },

    { Value : ID,            @UI.Importance : #High },
    { Value : Description,   @UI.Importance : #High },
    { Value : (Customer.ID), @UI.Importance : #High, @HTML5.CssDefaults: {width:'14em'} },
    { Value : (Agency.ID),   @HTML5.CssDefaults: {width:'16em'} },
    { Value : BeginDate,     @HTML5.CssDefaults: {width:'9em'} },
    { Value : EndDate,       @HTML5.CssDefaults: {width:'9em'} },
    { Value : BookingFee,    @HTML5.CssDefaults: {width:'10em'} },
    { Value : TotalPrice,    @HTML5.CssDefaults: {width:'12em'} },
    { Value : (Status.code),
      Criticality : (
        Status.code == #Accepted ? 3 :
        Status.code == #Open ? 2 :
        Status.code == #Canceled ? 1 : 0
      ),
      @UI.Importance : #High,
      @HTML5.CssDefaults: {width:'10em'}
    }
  ],

  Facets : [{
    $Type  : 'UI.ReferenceFacet', Target : '@UI.FieldGroup#TravelData',
    Label  : '{i18n>GeneralInformation}',
  }, {  // booking list
    $Type  : 'UI.ReferenceFacet',
    Target : 'Bookings/@UI.PresentationVariant',
    Label  : '{i18n>Bookings}'
  }],

  FieldGroup#TravelData : { Data : [
    { Value : (Customer.ID) },
    { Value : (Agency.ID) },
    { Value : Description },
    { Value : (Status.code),
      Criticality : (
        Status.code == #Accepted ? 3 :
        Status.code == #Open ? 2 :
        Status.code == #Canceled ? 1 : 0
      ),
    },
    { Value : BeginDate },
    { Value : EndDate },
    { Value : BookingFee },
    { Value : TotalPrice },
  ]}
};

annotate TravelService.Bookings with @UI : {
  Identification : [
    { Value : Pos },
  ],

  HeaderInfo : {
    TypeName       : '{i18n>Bookings}',
    TypeNamePlural : '{i18n>Bookings}',
    Title          : { Value : Travel.Customer.LastName },
    Description    : { Value : Pos }
  },

  PresentationVariant : {
    Visualizations : ['@UI.LineItem'],
    SortOrder      : [{
      $Type      : 'Common.SortOrderType',
      Property   : Pos,
      Descending : false
    }]
  },

  SelectionFields : [],

  LineItem : [
    { Value : Flight.icon, Label : '  '},
    { Value : (Flight.ID), Label : '{i18n>FlightID}' },
    { Value : (Flight.date), Label : '{i18n>FlightDate}' },
    { Value : BookingDate, Label : '{i18n>BookingDate}' },
    { Value : FlightPrice, Label : '{i18n>FlightPrice}' },
    { Value : Flight.departure, Label : '{i18n>Departure}', ![@Common.FieldControl]: #ReadOnly },
    { Value : Flight.arrival, Label : '{i18n>Arrival}', ![@Common.FieldControl]: #ReadOnly },
    { Value : Flight.origin, Label : '{i18n>Origin}', ![@Common.FieldControl]: #ReadOnly },
    { Value : Flight.destination, Label : '{i18n>Destination}', ![@Common.FieldControl]: #ReadOnly },
    { Value : Flight.airline, Label : '{i18n>Airline}', ![@Common.FieldControl]: #ReadOnly },
  ],

  Facets : [{
    $Type  : 'UI.CollectionFacet',
    Label  : '{i18n>GeneralInformation}',
    ID     : 'Booking',
    Facets : [{  // booking details
      $Type  : 'UI.ReferenceFacet',
      ID     : 'BookingData',
      Target : '@UI.FieldGroup#GeneralInformation',
      Label  : '{i18n>Booking}'
    }, {  // flight details
      $Type  : 'UI.ReferenceFacet',
      ID     : 'FlightData',
      Target : '@UI.FieldGroup#Flight',
      Label  : '{i18n>Flight}'
    }]
  }, {  // supplements list
    $Type  : 'UI.ReferenceFacet',
    ID     : 'SupplementsList',
    Target : 'Supplements/@UI.PresentationVariant',
    Label  : '{i18n>BookingSupplements}'
  }],

  FieldGroup #GeneralInformation : { Data : [
    { Value : Pos },
    { Value : BookingDate },
    { Value : Travel.Customer.ID },
  ]},

  FieldGroup #Flight: {Data: [
    { Value: Flight.airline },
    { Value: Flight.ID },
    { Value: Flight.date },
    { Value: FlightPrice }
  ]},
};

annotate sap.capire.travels.masterdata.Flights:icon with @UI.IsImageURL;

annotate TravelService.Bookings.Supplements with @UI : {
  Identification : [
    { Value : ID }
  ],
  HeaderInfo : {
    TypeName       : '{i18n>BookingSupplement}',
    TypeNamePlural : '{i18n>BookingSupplements}',
    Title          : { Value : ID },
    Description    : { Value : ID }
  },
  PresentationVariant : {
    Text           : 'Default',
    Visualizations : ['@UI.LineItem'],
    SortOrder      : [{
      $Type      : 'Common.SortOrderType',
      Property   : ID,
      Descending : false
    }]
  },
  LineItem : [
    { Value : ID                                       },
    { Value : (booked.ID), Label : '{i18n>ProductID}'    },
    { Value : Price,     Label : '{i18n>ProductPrice}' }
  ],
};
