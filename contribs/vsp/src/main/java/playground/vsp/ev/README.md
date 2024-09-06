# UrbanEV
This package represents an ongoing project on modeling charging behavior for private motorized transport in urban environments.
The code is in experimental state.
Please refer to VSP (Tilmann Schlenther, @tschlenter or Michal Maciejewski, @michalmac) for any questions.

## Description of the model
The model lets agents plan their charging in advance, meaning that they do not show reactive behavior to the SoC over the course of the day.
At the beginning of the qsim (after the official replanning phase), all selected plans are processed. For each selected plan that contains legs of a network mode using an EV,
the energy consumption is estimated. If, during leg _Y_, the estimated SoC falls under a configurable threshold, the agent tries to incorporate charging into it's plan, prior to _Y_.<br>
Charging is assumed to take place during normal activities, only. Therefore, the agents needs to find an activity _A_ prior to _Y_,
that is long enough, of a corresponding type and within a maximum distance of a suitable charger (all constraints are configurable)
and represents the destination of a leg with the same EV. If, in the meantime (i.e. after _A_ but before _Y_), the agent performs another subtour with another vehicle, the agent considers the duration of the subtour as a possible period for charging.<br> 
If a suitable activity _A_ is found, the agent diverts the EV leg to the corresponding activity, drives to a charger first and walks from there to the destination of the preceding EV leg.
Before starting it's next EV leg (which is not necessarily _Y_), it needs to walk to the corresponding charger, first.<br>
If no suitable while-charging-activity _A_ can be found, a warning message is dumped into the logfile. The EV will most likely run empty in the simulation, later.
This is not prevented but included in analysis.<br>
If an agent estimates to run beyond the energy threshold on it's way back home (more precisely: back to the location from where it entered the corresponding EV for the first time),
and if there is a charger on the home link, it does not search for a suitable activity prior to this leg but just plugs in the EV at home.<br>
The final SoC at the end of the iteration is maintained and transferred as the initial SoC to the next iteration. 

For this to work, vehicles that represent an EV need to be attached to a vehicle type that is tagged as EV by providing a specific attribute (see `MATSimVehicleWrappingEVSpecificationProvider.class`).
By default, _all_ of these (electric) vehicles are planned by to potentially get charged according to the above described logic.
However, one can prevent this on the vehicle level by `UrbanEVUtils.setChargingDuringActivities(vehicle, false)` when defining the input. 

## A few notes to the current state of this package (and it's issues and TODOs)

- The code generally was tested only with the Open Berlin Scenario v5.5.x ]
- The official EV specification is the one coming from the matsim-vehicle, similar to the `ev` contrib. (MATSim-PR2218, Oct '22) 
- (needs review - obsolete, but code requires some checks) The initial SoC (as well as other important attributes) is read/written from/to the vehicle _type_ instead of the (single) vehicle. This is, because the corresponding code was written before MATSim-PR1605. So, the code could use adoption to PR1605. Now, if you want to have individual initial SoCs, you need to have one vehicle type for each person/value.

### Overnight/home charging
1. Overnight charging currently only takes place on the overnight (home) activity. Instead, nearby chargers should be considered.
1. Overnight charging should be modelled with a custom `VehicleChargingHandler` that observes vehicles still charging at the end of iteration x and adds them to the charging logic in the first time step of the next iteration - instead of incorporating an additional (plug-in) trip in the person's plan (in the `UrbanEVTripsPlanner`), one could still have a plug-out trip.
1. The first leg of the next day is not considered in the planning of the charging. So, if no home charger is available, a vehicle might run out of energy during it's first trip of the next day (this may change if the model instead assumes that every EV owner also owns a home charger).
1. Related to the last point: If a vehicle (is expected to) runs out of energy during it's first leg of the day, this can not be prevented at the moment.
While the `UrbanEVTripsPlanner` actually is equipped to detect that and to plan charging from the beginning of the day/qsim, in case there is a home charger, this is strangely not reflected in the qsim. Meaning,
   the planner builds in an additional 0m trip to and from the home charger at the beginning of the qsim, but the plugin activity is never executed in the qsim. This seems like a fixeable bug, but currently persists in the code. (July 2022)
1. The code has not been heavily tested with 'open plans and/or subtours'. This means plans where people do not return to their first activity of the day. There might evolve artefacts when simulating such plans. There is one test that checks whether the build-in of home charging respects the mass conservation logic, which it does.
   That means, home charging only takes place if destination of the last ev-leg is equal (same link id) as origin of the first ev leg. Having said this, open subtours/plans should hopefully not cause major problems. But it should be investigated again.
   
### Critical leg
1. If an agent does not find a possibility to charge prior to a leg for which the SoC is to fall under the threshold but stay over 0, it will not explore whether it would be possible to charge _after_ the critical leg (i.e. after the threshold is crossed).

   
