using { TravelService.Travels } from './travel-service';

// Note: The @flow.status annotation makes that field @readonly by default
annotate Travels with @flow.status: (Status) actions {
  deductDiscount  @from: [ #Open ]; // can only be called on #Open travels
  acceptTravel    @from: [ #Open ]                @to: #Accepted;
  rejectTravel    @from: [ #Open ]                @to: #Rejected;
  reopenTravel    @from: [ #Rejected, #Accepted ] @to: #Open;
}

// workaround to integrate with draft lifecycle
extend Travels with actions {
  @from: [ #Open, #Accepted ]
  action draftEdit(PreserveChanges: Boolean) returns Travels; // define to annotate
}
