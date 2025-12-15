using { TravelService } from '../../srv/travel-service';
//
// annotations that control the behavior of fields and actions
//

annotate TravelService.Travels with @(Common : {
  SideEffects: {
    SourceProperties: [BookingFee],
    TargetProperties: ['TotalPrice']
  },
}){
  BookingFee  @readonly: (Status.code = #Accepted) @mandatory: (Status.code != #Accepted);
  BeginDate   @readonly: (Status.code = #Accepted) @mandatory: (Status.code != #Accepted);
  EndDate     @readonly: (Status.code = #Accepted) @mandatory: (Status.code != #Accepted);
  Agency      @readonly: (Status.code = #Accepted) @mandatory: (Status.code != #Accepted);
  Customer    @readonly: (Status.code = #Accepted) @mandatory: (Status.code != #Accepted);
} actions {
  deductDiscount @(
    Common.SideEffects.TargetProperties : ['in/TotalPrice', 'in/BookingFee'],
  );
}

annotate TravelService.Travels @Common.SideEffects#ReactonItemCreationOrDeletion : {
  SourceEntities : [ Bookings ],
  TargetProperties : [ 'TotalPrice' ]
};

annotate TravelService.Bookings with @UI.CreateHidden : (Travel.Status.code != #Open);
annotate TravelService.Bookings with @UI.DeleteHidden : (Travel.Status.code != #Open);

annotate TravelService.Bookings {
  BookingDate   @Core.Computed;
  Flight        @readonly: (Travel.Status.code = #Accepted) @mandatory: (Travel.Status.code != #Accepted);
  FlightPrice   @readonly: (Travel.Status.code = #Accepted) @mandatory: (Travel.Status.code != #Accepted);
};

annotate TravelService.Bookings with @Capabilities.NavigationRestrictions.RestrictedProperties : [
  {
    NavigationProperty : Supplements,
    InsertRestrictions : {
      Insertable : (Travel.Status.code = #Open)
    },
    DeleteRestrictions : {
      Deletable : (Travel.Status.code = #Open)
    }
  }
];


annotate TravelService.Bookings.Supplements with @UI.CreateHidden : (up_.Travel.Status.code != #Open) {
  Price  @readonly: (up_.Travel.Status.code = #Accepted) @mandatory: (up_.Travel.Status.code != #Accepted);
  booked @readonly: (up_.Travel.Status.code = #Accepted) @mandatory: (up_.Travel.Status.code != #Accepted);
};
