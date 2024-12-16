# Strategic electric vehicle charging (SEVC)

## Configuration parameters

A range of configuration options control the generation of new charging plans:

- `minimumActivityChargingDuration` (*Double*, default `900`, seconds): defines the shortest possible activity or chain of activities during which charging may be planned

- `maximumActivityChargingDuration` (*Double*, default `Inf`, seconds): defines the longest possible activity or chain of activities during which charging may be planned

- `minimumEnrouteDriveTime` (*Double*, default `3600`, seconds): defines the shortest possible drive during which enroute charging may be planned

- `minimumEnrouteChargingDuration` (*Double*, default `900`, seconds): defines the shortest possible charging slot that may be generated enroute

- `maximumEnrouteChargingDuration` (*Double*, default `3600`, seconds): defines the shortest possible charging slot that may be generated enroute

- `chargerSearchRadius` (*Double*, default `1000`, meters): defines the radius within which chargers are searched

The following parmeters control the selection / innovation process for charging plans:

- `selectionProbability` (*Double*, default `0.8`): defines the frequency with which charging plans are switched rather than newly generawted

- `selectionStrategy` (*Exponential|Random|Best*, default `Exponential`): defines the way that plans are selected from the plan memory, based on their charging score

- `maximumChargingPlans` (*Integer*, default `4`): size of the charging plan memory

The following parameters control scoring of charging plans:

- `chargingScoreWeight` (*Double*, default `0.0`): defines with which factor the calculated charging score for a plan is fed back into the score of the regular plan

- `scoreTrackingInterval` (*Integer*, default `0`): defines how often detailed information on the scoring process is written out (`0` means never, any other value produces surely an output in the last iteration)

The following parmeters control the online search process during the simulation:

- `maximumAlternatives` (*Integer*, default `2`): maximum attempts (including the initial one) that an agent tries to find a charger before the search is aborted

- `maximumQueueTime` (*Double*, default `300`): maximum queue time at a charger until an alternative charger is searched

- `useProactiveAlternativeSearch` (*Boolean*, default `true`): defines if agents consider potential alternatives proactively upon starting the leg that leads to a planned charging activity

- `alternativeSearchStrategy`: defines the way agents make decisions for alternative chargers during the simulation

- *Naive*: agents select a random alternative charger 
- *OccupancyBased*: agents select a random alternative charger that has a free spot
- *ReservationBased*: agents select a random alternative charger and reserves it for the planned duration

- `alternativeCacheSize` (*Integer*, default `-1`): defines whether to precompute viable charger alternatives for each charging activity planned in an agent's plan. In that case the search process is restricted to one alternative attempt. 

## Scoring parameters

- `cost` (*Doule*, default `-1.0`): weight the monetary cost units incurred from charging

- `waitTime_min` (*Double*, default `0.0`): weight the time waited in a queue for charging

- `detourTime_min` (*Double*, default `0.0`): weighs the travel time reduced by the planned travel time of the base plan. Evaluation is mostly relevant compared to the detour component of another charging plan.

- `detourDistance_km` (*Double*, default `0.0`): weighs the travel time reduced by the planned travel time of the base plan. Evaluation is mostly relevant compared to the detour component of another charging plan.

- `zeroSoc` (*Double*, default `-100.0`): penalty added when the SoC of the agent drops to zero

- `failedChargingAttempt` (*Double*, default `-10.0`): penalty added when a charging attempt of the agent fails (probably followed by an alternative attempt)

- `failedChargingProcess` (*Double*, default `-100.0`): penalty added when a charging process fails (no new alternative is found)

- `belowMinimumSoc` (*Double*, default `0.0`): penalty added when the SoC of a person falls below an attributable value (see person attributes)

- `belowMinimumEndSoc` (*Double*, default `0.0`): penalty added when the SoC of the person at the end of the simulation is below an attributable value (see person attributes)

## Cost models

Two cost models can be selected by default. 

The *DefaultCostModel* defines one cost parameter:

- `costPerEnergy_kWh` (*Double*, default `0.28`): cost registered for charging energy
- `costPerDuration_min` (*Double*, default `0.0`): cost registered for charging duration

The *AttributeCostModel* defines no parameters, but obtains the cost from the charger (see charger attributes).


## Charger and person attributes

### Chargers

A couple of attributes define the chargers search process. One option would be to save all chargers in a large global spatial index, but then filtering out which chargers are aimed at a specific purpose is costly. Hence, we define a couple of categories in which each charger needs to be sorted in to speed up identification of viable chargers for each desired enroute or activtiy-based charging activity:

- `sevc:facilities` (*String*, optional): Chargers that have this attribute are proposed during charger search when the activity during which an agent wants to charge takes place at one of the listed facilities.

- `sevc:persons` (*String*, optional): Chargers that have this attribute are proposed during charger search when the agent is in the provided list of persons.

- `sevc:public` (*Boolean*, default `false`): By default, all chargers are not public and only found if they fall in one of the previous categories. Only chargers for which is attribute is `true` are provided in a spatial search around the planned enroute or fixed charging activities of an agent, additionally to the categories above.

For public chargers (but, potentially, also for workplace chargers and others) an additional layer of access verification is performed. Each charger can have a list of subscriptions of which a user must have one in order to be allowed to charge:

- `sevc:subscriptions` (*String*, comma-separated, optional): describes the subscriptions that are necessary to use this charger. If no attribute is give, the charger is accessible to everyone.

Additional attributes:

- `sevc:operator` (*String*, optional): describes the name of the operator of the charger. The atrtibutes is used for per-operator analysis.

- `secv:costPerEnergy_kWh` (*Double*, default `0.0`): describes the cost in monetary units (for instance, EUR) for charging one kWh at the charger. Used if the `AttributeBasedChargingCost` is chosen.

- `secv:costPerDuration_min` (*Double*, default `0.0`): describes the cost for the plugging duration at the charger. Used if the `AttributeBasedChargingCost` is chosen.

**Some attributes need clarification: @ Olly**

- `plugType`: What is meant?

### Persons

For a person to be covered by the SEVC contrib, it needs to be activated. Also, access to chargers can be defined:

- `sevc:active` (*Boolean*, default `false`): only if set to true, an agent is simulated in SEVC, otherwise they are ignored

- `sevc:subscriptions` (*String*, comma-separated, optional): a list of subscription that the persons owns. In case a charger requires a subscription, it must be present in the list for the person to be eligible.

- `sevc:activityTypes` (*String*, comma-separated, optional): if given, the agent will only attempt at activities of the listed types

Some parameters relevant for the simulation dynamics:

- `sevc:criticalSoc` (*Double*, default `0.0`): if the agents departs on a leg and detects that the SoC will fall below that value along the way, a spontaneous charging activity will be created. Only used if spontaneous charging has been activated via configuration.

- `sevc:maximumQueueTime` (*Double*, default from config): the time the agent will wait at a charger to be queued before the alternative search process starts. If no value is given, the default value from the main config group is used.

Some parameters are relevant for scoring of the charging plans:

- `sevc:minimumSoc` (*Double*, default `0.2`): if the SoC of the vehicle falls below this threshold, SEVC scoring will add a configurable penalty

- `sevc:minimumEndSoc` (*Double*, default `0.8`): if the SoC of the vehicle is below this threshold at the end of the simulation, SEVC scoring will add a configurable penalty

### Vehicles

The maximum SoC per vehicle should per vehicle:

- `sevc:maximumSoc` (*Double*): defines the maximum SoC until a vehicle will be charged at a charger

**Some things that are not handled by the SEVC contrib: @ Olly**

- Battery capacity is already defined in `VehicleType` > `EngineInformation` attributes in vehicles file
- Initial SoC is already defined in `Vehicle` attributes in vehicles file

**Some things that need clarification / are obsolete @ Olly**

- `simEndSoc` is a result of the simulation and can be handled via scoring (see above)
- `chargingTypeIndex` not sure what is meant
- `chargingType` not sure what is meant
- `chargerIds` is rather handled in the chargers file, to be more consistent across different types
