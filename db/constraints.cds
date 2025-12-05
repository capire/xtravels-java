using { sap.capire.travels as my } from './schema';

@assert.constraint.beginAfterEnd: {
  condition: (BeginDate <= EndDate),
  message: 'End Date must be after Begin Date.',
  targets: [(EndDate)]
}
annotate my.Travels with {
  @mandatory BeginDate;
  @mandatory EndDate;
  @mandatory Agency;
  @mandatory Customer;
}

annotate my.Bookings with {
  @mandatory Flight;
  @mandatory Travel;
}

annotate my.Travels with @Capabilities.FilterRestrictions.FilterExpressionRestrictions: [
  { Property: 'BeginDate', AllowedExpressions : 'SingleRange' },
  { Property: 'EndDate', AllowedExpressions : 'SingleRange' }
];
