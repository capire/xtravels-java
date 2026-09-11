---
name: query-flights
description: >
  Search for available flights by origin, destination, and date range.
metadata:
  tags:
    - flights
  examples:
    - Show me flights from Frankfurt to New York in August
    - Find flights from FRA on 2026-08-01
---

# Skill: Query Flights

## Instructions

To determine valid Flight IDs and dates use the query tool to query the Flights entity.
Use the describe tool to determine the structure of the Flights entity.
Prefer LIKE in case you are unsure about exact names of origin or destination airports.
For example to query for flights from Frankfurt use: SELECT FROM Flights WHERE origin LIKE '%FRA%' OR origin LIKE '%Frankfurt%'
In case you already know about a certain time frame, preferred airlines, etc. apply filters on the Flights query accordingly as well.
