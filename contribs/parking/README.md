# Parking

The MATSim simulation by default does not consider parking infrastructure or supply constraints. There are different
approaches as to how parking can be handled in MATSim, depending on the use case.

- Parking Choice, based on work of Rashid Waraich. Parts of the contrib are used in the carsharing contrib. Its main
  goal, as far as we understand it, is the simulation of parking choice (e.g. between garages). There is no modelling of
  circulating traffic searching for parking or walking of agents to/from parking.
- Parking Search, by Joschka Bischoff and Tillman Schlenther. This contrib has been currently developed at TU Berlin.
  The main goal is to model parking search, including walk legs of agents and vehicles searching for parking spaces.
- Parking Proxy, Parking Proxy by Tobias Kohl (Senozon)
  . This was designed for large scenarios where it's not feasable to fully simulate parking agents. Rather, the
  additional time needed for parking is estimated
- Parking Costs, developed by Marcel Rieser and Joschka Bischoff at SBB. This modules allows the integration of parking
  costs based on link attribute data.

## Implementations

### Parking Search

Model parking search, including walking segments and parking search traffic.

Different Parking Search Logics:

1. **Random:**
    1. Drive to the destination.
    2. The next link is chosen randomly.
2. **DistanceMemoryParkingSearch:**
    1. Drive to the destination.
    2. Select the next link:
        1. Choose an unknown link with the shortest straight-line distance to the destination.
        2. If all links are known, choose randomly.
3. **NearestParkingSpotSearchLogic:**
    1. Drive to the destination (??).
    2. Search for the facility with the shortest distance to the current location, considering the expected driving time and parking duration (and
       possibly parking time restrictions).
    3. If no suitable facility is found ?
4. **BenensonParkingSearchLogic:** A more sophisticated strategy based on the Benenson
   model: https://www.sciencedirect.com/science/article/pii/S0198971508000689

### Parking Proxy

### Parking Costs

### Parking Choice
