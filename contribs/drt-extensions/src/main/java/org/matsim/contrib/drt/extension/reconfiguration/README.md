# Generic DvrpLoad representation of DVRP vehicle capacities and occupancies

These classes allow the capacity of a DvrpVehicle to change during the simulation.
To achieve this, several features were implemented between the DVRP and DVRP modules. 
The implementation details are presented in javadoc in the code, below we show how to use this functionality to build your own simulations with complex capacities and occupancies and changing capacities.

## How to use ?
We describe below the steps to follow to implement two example tests. If you want to look at code examples, they are present in `RunDrtExampleWithChangingCapacitiesTest`

### Simulating a fleet with fixed heterogeneous capacities and its related demand
Let's say the goal is to simulate a service where some vehicles can serve persons and the others can serve goods, both being able to be represented with an integer. The steps below can be followed to achieve that:
1. Define two extensions of `IntegerLoadType`: `PersonsLoadType` and `GoodsLoadType` that both build `IntegerLoad` instances with the given type.
2. (a) If the vehicles come from a dvrp fleet file: Define an implementation of `DvrpLoadFromFleet` that sets the vehicle's capacity to persons or goods according to its ID. (b) If the vehicles come from a vehicles file: You can add the attributes `dvrp:capacityType` and `dvrp:capacityValue` in the vehicles or factor in different vehicle types.
3. (a) Define an implementation of `DvrpLoadFromDrtPassengers` the determines whether a request is a persons or a goods one. (b) Alternatively, modify your plans.xml to add the person attribute `drt:loadType` with the name of the appropriate load type.
4. Bind a DvrpLoadSerializer that is aware of the two `DvrpLoadType`s that are used in the simulation, the `DefaultDvrpLoadSerializer` can be built by passing them to the constructor.

### Simulating a fleet with changing heterogeneous capacities and its related demand
Now let's say that vehicle capacities are to be re-configured during the day, the process above can be adapted as follows:
1. Same as step 1 above.
2. If you want all Dvrp vehicles to start with the same capacity (persons or goods), you can simply bind `IntegerLoadType` to the desired one. Otherwise, follow step 2.a or 2.b from above.
3. One of steps 3.a or 3.b from above.
4. Same as step 4 from above.
5. Bind the proposed `DefaultCapacityReconfigurationLogic` or bind your own `CapacityReconfigurationLogic` implementation, an example is shown in `RunDrtExampleWithChangingCapacitiesTest.SimpleCapacityConfiguration` that configures all vehicles to switch capacities at 12pm.
6. Add the `CapacityChangeSchedulerEngine` by passing the bound `CapacityReconfigurationLogic`.