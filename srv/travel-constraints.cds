using { TravelService as my } from './travel-service';

annotate my.Travels with {
    @assert: (case
                  when BeginDate            > EndDate
                       then 'End Date must be after Begin Date.'
                  when Bookings.Flight.date < BeginDate
                       then 'Flight Date must be after Begin Date.'
                  when Bookings.Flight.date > EndDate
                       then 'Flight Date must be before End Date.'
              end)
    EndDate;
    @assert: (case
                  when BookingFee < 0
                       then 'Booking Fee must be non-negative.'
              end)
    BookingFee;

    @assert: ( Description ==  'foo' ? 'Description must not be "foo"' : null)
    Description;
}

annotate my.Bookings with {
  @mandatory Travel;
}

annotate my.Travels with @Capabilities.FilterRestrictions.FilterExpressionRestrictions: [
  { Property: 'BeginDate', AllowedExpressions : 'SingleRange' },
  { Property: 'EndDate', AllowedExpressions : 'SingleRange' }
];
