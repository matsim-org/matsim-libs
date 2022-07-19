This package represents an ongoing project on modeling charging behavior for private motorized transport in ubran transport.
The code is in experimental state.
Please refer to VSP (Tilmann Schlenther) for any question.

***TODO*** put in some information on what the code does and the general idea and assumptions


**A few notes to the current state of this package (and it's issues and TODOs):**

1. The code generally was tested only with the Open Berlin Scenario v5.5.? 
1. The initial SoC (as well as other important attributes) is read/written from/to the vehicle _type_ instead of the (single) vehicle. This is, because the corresponding code was written before MATSim-PR1605. So, the code could use adoption to PR1605. Now, if you want to have individual initial SoCs, you need to have one vehicle type for each person/value.
1. Overnight charging currently only takes place on the overnight (home) activity. Instead, nearby chargers should be considered.
1. The first leg of the next day is not considered in the planning of the charging. So, if no home charger is available, a vehicle might run out of energy during it's first trip of the next day.
1. Related to the last point: If a vehicle (is expected to) runs out of energy during it's first leg of the day, this can not be prevented at the moment.
While the UrbanEVTripsPlanner actually is equipped to detect that and to plan charging from the beginning of the day/qsim, in case there is a home charger, this is strangely not reflected in the qsim. Meaning,
   the planner builds in an additional 0m trip to and from the home charger at the beginning of the qsim, but the plugin activity is never executed in the qsim. This seems like a fixeable bug, but currently persists in the code. (July 2022)
1. The code has not been tested with 'open plans and/or subtours'. There might evolve artefacts when simulating such plans.

   