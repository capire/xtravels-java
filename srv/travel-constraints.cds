using { TravelService } from './travel-service';


annotate TravelService.Travels with {

  // Description @assert: (case
  //   when length(Description) < 3 then 'Description too short'
  // end);

  // Agency @assert: (case
  //   when not exists Agency then 'Agency does not exist'
  // end);

  // Customer @assert: (case
  //   when Customer is null then '409003'
  //   when not exists Customer then 'Customer does not exist'
  // end);

  // EndDate @assert: (case 
  //   when EndDate < BeginDate then error('ASSERT_ENDDATE_AFTER_BEGINDATE', null, (BeginDate, EndDate))
  //   when exists Bookings [Flight.date > Travel.EndDate] then 'ASSERT_BOOKINGS_IN_TRAVEL_PERIOD'
  // end);

  BookingFee @assert: (case
    when BookingFee < 0 then 'ASSERT_BOOKING_FEE_NON_NEGATIVE'
  end);

};


annotate TravelService.Bookings with {

  // Flight {
  //   date @assert: (case
  //     when date not between $self.Travel.BeginDate and $self.Travel.EndDate then 'ASSERT_BOOKINGS_IN_TRAVEL_PERIOD'
  //   end);
  // }

  FlightPrice @assert: (case
    when FlightPrice < 0 then 'ASSERT_FLIGHT_PRICE_POSITIVE'
  end);

  Currency {
    code @assert: (case
      when code != $self.Travel.Currency.code then 'ASSERT_BOOKING_CURRENCY_MATCHES_TRAVEL'
    end);
  }

};


annotate TravelService.Travels with @Capabilities.FilterRestrictions.FilterExpressionRestrictions: [
  { Property: 'BeginDate', AllowedExpressions : 'SingleRange' },
  { Property: 'EndDate', AllowedExpressions : 'SingleRange' }
];
