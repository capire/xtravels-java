using {TravelService} from '../../srv/travel-flows';

// add actions
extend TravelService.Travels with actions {
  action reviewTravel();
  action blockTravel();
  action unblockTravel();
};

// extend flow
annotate TravelService.Travels with actions {
  reviewTravel   @from: [ #Open ]                           @to: #InReview;
  blockTravel    @from: [ #InReview, #Open ]                @to: #Blocked;
  reopenTravel   @from: [ #InReview, #Accepted, #Canceled ] @to: #Open;
  unblockTravel  @from: [ #Blocked ]                        @to: $flow.previous;
  // in the extended flow, accept/reject/deduct only from InReview
  acceptTravel   @from: [ #InReview ];
  rejectTravel   @from: [ #InReview ];
  deductDiscount @from: [ #InReview ]
};

// specify ui labels
annotate TravelService.Travels with actions {
  reviewTravel    @title: '{i18n>ReviewTravel}';
  blockTravel     @title: '{i18n>BlockTravel}';
  unblockTravel   @title: '{i18n>UnblockTravel}';
};
