---
name: manage-bookings
description: >
  Add flight bookings to an existing travel or new travel. Supports creation of new travels.
metadata:
  tags:
    - travel
    - bookings
  examples:
    - Add a flight from FRA to SFO on 2026-08-01 to travel 1
    - Create a new travel to London from 2026-09-01 to 2026-09-10
---

# Skill: Manage Bookings

## Instructions

Use the createTravel action to create new travels.
Use the addFlightToTravel action to create a flight booking for a given travel.

To determine valid Flight IDs and dates use the query tool to query the Flights entity.
Use the describe tool to determine the structure of the Flights entity.
Prefer LIKE in case you are unsure about exact names of origin or destination airports.
For example to query for flights from Frankfurt use: SELECT FROM Flights WHERE origin LIKE '%FRA%' OR origin LIKE '%Frankfurt%'
In case you already know about a certain time frame, preferred airlines, etc. apply filters on the Flights query accordingly as well.
