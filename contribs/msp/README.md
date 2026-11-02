# The MATSim MSP Contrib

Playground of the **Chair of Mobility System Planning (MSP)** at **TU Dresden**.

This contrib is the temporary home for code developed at the chair. It follows the same idea as the
`vsp` contrib: new functionality is developed here first, and once it is mature and generally useful
it is moved into a contrib of its own (or into the MATSim core).

**Please note:** nothing in this contrib should be considered stable API. Classes may be moved,
renamed, or removed without notice.

## Structure

```
src/main/java/org/matsim/contrib/msp   production code
src/test/java/org/matsim/contrib/msp   tests
```

## Getting started

`org.matsim.contrib.msp.run.RunMspScenario` is the generic entry point, built on `MATSimApplication`.
It takes the config from the command line:

```
java -cp matsim-msp.jar org.matsim.contrib.msp.run.RunMspScenario run --config path/to/config.xml
```

`RunMspScenarioTest` runs it on the `equil` example scenario for two iterations and checks that the
output is written. Use it as a blueprint for the tests of your own run classes.

## Capability analysis

`org.matsim.contrib.msp.capability` — a post-simulation analysis that measures the capability of an
agent by the discretionary activities that can be inserted into its *fundamental plan*: the plan reduced to
the activities the agent cannot choose not to perform.

The measure has **three levels**, each the one below it aggregated:

```
level 1   one insertion   u_potential     = u_0 + k * ln(1 + slack / t_typ) - c_insertion
level 2   one facility    u_potential,max = max over the insertions of that facility
level 3   one person      u_potential,max = max over the facilities
                          u*              = nested logsum over the facilities
```

`slack` is the width of the window the activity could be *started* in, so the logarithm grades
flexibility while `u_0` carries the main signal: being able to perform the activity at all. The `1 +`
matters - an insertion that only just fits has `slack = 0` and is then worth exactly `u_0`, not minus
infinity. `c_insertion` is what the detour costs, `VoT * t_extra` by default
(`valueOfTimePerHour`, 6), and it is **part of** `u_potential`, not a separate column to trade off
afterwards.

**Units.** `valueOfTimePerHour` is the only utility *rate* in the analysis and it is per hour, like
MATSim's own `marginalUtilityOfTraveling_utils_hr`. `u_0` and `k` are plain utilities, every duration -
`minDuration`, `referenceDuration`, `maxExtraTravelTime`, and every time column of the output - is in
seconds, and `c_insertion` is already in utils. `InsertionCost.ofTime` is the one place the two meet and
the only place that divides by 3600.

The two aggregation steps are deliberately different:

- **insertions → facility is a maximum.** The insertions of one facility are the same activity at
  different times of day; the person performs it once, and the best opportunity is the one they would
  take. Summing the flexibility of all of them and pairing that with the cheapest detour would describe
  a trip nobody can make - which is what the earlier version of this measure did. `sum_slack` and
  `c_insertion_min` are still reported, as diagnostics, and `best_insertion_id` says which level 1 row
  the utility came from.
- **facilities → person is a logsum.** Having a choice of places genuinely is worth something, and a
  logsum is what a discrete choice model gives for "how good is this choice set".

A level 2 row exists **only where the activity can actually be performed** - no row means the person
cannot perform it there. Level 3 has a row for **every analysed person**, including those who can reach
nothing at all: that is a result, not a missing value.

### Pipeline

```
input directory (config, network, plans)
places csv  --[DiscretionaryActivityFacilityCsv]-->  standalone facilities, coordinates only
                     |
  step 1             v
plans  --[DefaultCapabilityPersonFilter]-->  the persons the analysis speaks for
                     |                       (subpopulation, age, study area, complete chain)
  step 2             v
       --[FundamentalPlanExtractor]-->  mandatory activities only, original times kept
                     |                  -> capability_fundamental_plans.csv (the chains, per person)
                     v
       --[FreePeriod]-->  the stretches of the day an activity could go into
                     |
  step 3             v
       --[CapabilityTravelTimes]-->  per-mode travel time, routed on the fly
                     |               (a precomputed car / bike / pt matrix is still to come)
  step 4             v
       --[InsertionCalculator]-->  feasibility, slack, detour and utility per place and mode
                     |
                     v
       --[CapabilityWriter]-->  insertions (level 1)
                                  -> facilities (level 2, best insertion per place)
                                     -> persons (level 3, logsum over places)
```

```
java -cp matsim-msp.jar org.matsim.contrib.msp.capability.RunCapabilityAnalysis \
    --input-directory output/ --places large-supermarkets.csv --shp study-area.shp
```

The input directory is anything holding a config, a network and a plans file - a run's output directory or
a folder prepared by hand. A vehicles file is picked up as well if there is one, because that is where the
speed of a network mode lives, see [Modes](#modes). Files are found by name, so the usual MATSim output
naming needs no further arguments; `--config`, `--network`, `--plans` and `--vehicles` override the
guesses. The config may or may not already carry a `capability` group; whatever it does not specify gets
the defaults. `--shp` is the study area the analysed persons have to live in, see
[below](#which-persons-are-analysed).

The code is split into one package per step of the method, under `capability`:

| package | holds | role |
|---|---|---|
| `capability` | `CapabilityConfigGroup`, `DiscretionaryActivityParams` | parameters, with one `discretionaryActivity` parameter set per analysed type |
| | `CapabilityAnalysis` | runs the measure over the population and writes the three levels |
| | `RunCapabilityAnalysis` | runs it on a finished run |
| | `CapabilityModule` | chains it to the end of a MATSim run |
| `capability.prepare` | `CapabilityPersonFilter` | which persons to analyse - the hook |
| | `DefaultCapabilityPersonFilter` | the default: residents of the study area with a home based day |
| | `DiscretionaryActivityFacilityCsv` | the csv interchange format of the places, read and write |
| | `RunCreateDiscretionaryActivityFacilities` | turns that csv into a MATSim facilities file |
| `capability.facilities` | `DiscretionaryActivityFacility` | a place a discretionary activity can be performed at |
| | `DiscretionaryActivityFacilities` | those places, indexed by type and by facility |
| | `DiscretionaryActivityFacilityExtractor` | reads them out of a MATSim `ActivityFacilities` |
| `capability.structure` | `FundamentalPlanExtractor` | reduces a plan to its mandatory activities, keeping their times |
| | `FreePeriod` | splits the remaining day into insertion positions - where elastic home lives |
| | `CapabilityTimeline` | the day as blocks on a 00:00-24:00 axis, with the soft home boundaries |
| `capability.measure` | `CapabilityTravelTimes` | free speed on each mode's own part of the network, teleported walk |
| | `InsertionCalculator` | feasibility, slack, detour and `u_potential` for one position and place |
| | `FlexibilityFunction` | how total flexible time becomes utility. `LOG` only so far |
| | `InsertionCost` | what an insertion costs, `VoT * t_extra` by default |
| | `CapabilityLogsums` | the nested utility logsum `u*` and the cost logsum |
| `capability.result` | `InsertionRecord`, `FacilityRecord`, `PersonRecord` | one row of each level |
| | `CapabilityWriter` | writes the levels, and the day blocks they sit on |

Dependencies run from `capability` into the sub packages and between them in that order; only
`CapabilityConfigGroup` is read by all of them. Each package has a `package-info.java` saying what it is
for.

Chained to a run:

```java
ActivityFacilities places = new DiscretionaryActivityFacilityCsv(capabilityConfig).read("places.csv");
controler.addOverridingModule(CapabilityModule.withPlaces(places));
```

### Modes

`capability.modes` lists the modes the analysis reports on, one output row per mode. How a mode's travel
times are computed follows the **config of the run**, so that the analysis never contradicts the scenario
it is analysing:

| the run's `routing` says | the analysis does |
|---|---|
| the mode is a `networkModes` entry | least cost paths over the links that allow the mode, at `min(link free speed, mode speed)` |
| the mode has a teleported speed | beeline distance x `beelineDistanceFactor` / `teleportedModeSpeed` |
| neither | fails, naming the mode |

Being a network mode wins, which matters for bike: MATSim's own config defaults give bike a teleported
speed of 15 km/h *as well*, and a scenario that routes bike on the network is meant to be analysed on the
network.

**Where a mode's speed comes from.** From the `maximumVelocity` of the vehicle type whose id is the mode
- MATSim's own convention (`modeVehicleTypesFromVehiclesData`), and what `contribs/bicycle` reads too, so
the bike of the analysis is as fast as the bike of the run. A mode with no such vehicle type travels at
the free speed of the links, which is right for car (free speed car is the deliberate default here) and
almost certainly wrong for bike: an OSM-derived network gives a residential street the 50 km/h of its car
speed limit, so **a bike without a vehicle type cycles at 50 km/h**. The log says per mode which of the
two it used.

The cap is a `min`, not a replacement: on a link whose free speed is already below the mode speed - a
shared pedestrian zone, a steep path - the link wins. A network that already carries bike-appropriate
free speeds therefore needs no vehicle type at all; the cap simply never binds.

Walk stays teleported, which is what MATSim does for it unless the scenario routes it.

**Where a place sits in the network.** Nowhere in particular until a mode asks: each mode positions every
place - the shops and the person's own activities alike - at the nearest link of **its own** network to
the place's coordinate. Nothing is snapped ahead of time and no `linkId` is written into the facilities.

The coordinate therefore **wins over any link a place names**, which is the other way round from what one
would expect. A named link is a position in somebody else's network: the link a run assigned to an
activity is a car link, and a `link_id` in the places csv was chosen without knowing which modes would be
analysed. Taking it over would measure a cyclist's trip from where the car is parked. A named link is
used only when there is no coordinate at all, which the places csv permits (`link_id` alone is a
location); a place like that is simply unreachable by a mode that may not use the link.

The alternative - one shared link per place, chosen among the links every mode may use - makes the modes
differ in speed alone and nothing else, which is tidier. It was rejected because the error it carries is
not noise but **bias in the direction of the question**: a place in a car-free street would be measured
from the nearest link that does carry cars, hundreds of metres away, understating the advantage of cycling
exactly where cycling has one. For a car-free zone policy case that is the result, not a rounding error.

### Which persons are analysed

`DefaultCapabilityPersonFilter` is what the run command applies. A person is kept if

1. **they are a person.** Only the subpopulations given by `--subpopulation` (default `person`) are kept,
   so that the freight and commercial agents of the scenario stay out. A population that carries no
   subpopulations at all passes this criterion as a whole.
2. **they are an adult**, i.e. at least `minAge` years old (default 18, `--min-age` overrides it), read
   from the `age` person attribute. The measure asks which discretionary activities a person could
   *choose* to perform, and a child's day is not theirs to rearrange. A person whose age cannot be read
   is dropped, for the same reason an unlocatable home is: there is no telling whether they qualify.
   **A population that carries no ages at all therefore needs `minAge = 0`**, which switches the
   criterion off - the log says how many persons were dropped this way.
3. **they live in the study area**, passed as `--shp`. Home is the `home_x` / `home_y` person attribute
   if the population carries it (`ExtractHomeCoordinates` of `contribs/application` writes it),
   otherwise the coordinate of the first home activity of the selected plan. A person whose home cannot
   be located either way is dropped - there is no telling whether they belong to the study area. Without
   `--shp` the criterion is skipped, with a warning.
4. **their day starts and ends at home** and the last non-home activity is over by 24:00:00.

Criterion 4 is a requirement of the method rather than a choice about the population, and
`CapabilityAnalysis` checks it on the fundamental plan again. It is checked here, on the full plan, so
that such persons are out of the **denominator** as well: a day that only becomes home based because a
discretionary activity was stripped off is not a resident the analysis can speak for.

Which criteria were applied, how many persons remain and why the others were dropped all go into the
log, so that a row count can be read without going back to the command line that produced it:

```
Person filter criteria:
  subpopulation   : [person]
  minimum age     : 18 years, from the 'age' attribute
  study area      : home location has to be inside the given shape
  activity chain  : has to start and end at home, and the last non-home activity has to be over by 24:00:00
Person filter: 1035 of 1200 persons remain (86.3%).
  120 dropped (10.0%): not in subpopulation [person].
  43 dropped (3.6%): living outside the study area.
  2 dropped (0.2%): the last non-home activity is not over by 24:00:00.
```

An unknown age and an unlocatable home are reported as **warnings** rather than as plain counts: they
are statements about the input being incomplete, not about the population, and they silently shrink what
the analysis speaks for.

A filter of your own goes into `new CapabilityAnalysis(config, filter)`;
`CapabilityPersonFilter.acceptAll()` switches the filtering off altogether.

### Elastic home

`home` is mandatory but **elastic**: the agent may come home later, leave earlier, or skip a home stay,
so home activities never constrain when a discretionary activity can be inserted. Only the other mandatory
activities - the *fixed anchors* - pin the day down.

This is not a detail. For `home 00:00-08:00 -> work 08:30-17:00 -> home 17:20-...`, pinning home down
leaves gaps of 30 and 20 minutes, into which nothing fits once travel and a minimum duration are
accounted for, and **every commuter would come out with no capability at all**. With home elastic the
evening opens up to the end of the day, which is where a commuter's shopping flexibility actually is.

A person with no fixed anchor - someone who stays home all day - has one free period covering the whole
day, which is the only way such a person enters the analysis, since they have no gap *between* two
activities.

Insertion is always en route, `origin -> place -> destination`. Going home first and making a round trip
is provably never better for a single mode: by the triangle inequality its detour is at least as long, it
cannot start earlier, and it ends at the same deadline.

### Output

Five files, written by `CapabilityWriter` into a `capability-analysis/` directory inside the input
directory, or wherever `--output` points:

| file | one row per | level |
|---|---|---|
| `capability_insertions.csv.gz` | person x mode x type x facility x insertion | 1 |
| `capability_facilities.csv.gz` | person x mode x type x facility | 2 |
| `capability_persons.csv` | person x mode x type | 3 |
| `capability_activities.csv.gz` | block of the agent's day | — |
| `capability_fundamental_plans.csv` | person the filter accepted | — |

The three tables that grow with the number of facilities and insertions are gzipped. The two that stay
the size of the population - the person level and the fundamental plans - are plain csv, because those
are the ones opened by hand. Level 1
grows fastest and `--skip-insertions` leaves it out entirely, which a run over a whole population
usually wants.

**Level 1**, `capability_insertions.csv.gz`:

```
person_id, mode, act_type, facility_id, facility_name, facility_sub_type, insertion_id,
slack, t_extra, c_insertion, u_0, t_typ, k, u_potential, n_mandatory_act, n_fixed_act,
period_from, period_to, origin_act, dest_act, origin_x, origin_y, dest_x, dest_y,
facility_x, facility_y, earliest_start, latest_start, travel_in, travel_out
```

Each row carries the parameters its own utility was computed with (`u_0`, `t_typ`, `k`), so the whole
measure can be recomputed - under a different `k`, a different functional form, a different cost term -
**from the csv alone, without re-running the analysis**. The trailing columns are the geometry: all
three corners of the detour, so the triangle can be drawn from the row alone (a solid line origin to
destination for the trip the agent was making anyway, dotted lines through the facility), plus the
`earliest_start` / `latest_start` window that `slack` is the width of.

**Level 2**, `capability_facilities.csv.gz`:

```
person_id, mode, act_type, facility_id, facility_name, facility_sub_type,
n_mandatory_act, n_fixed_act, n_insertions, best_insertion_id,
best_slack, best_t_extra, sum_slack, c_insertion_min, u_potential_max
```

**Level 3**, `capability_persons.csv`:

```
person_id, mode, act_type, n_mandatory_act, n_fixed_act,
n_reachable_facilities, n_sub_types, n_insertions,
u_potential_max, u_star, cost_logsum, beta_1, beta_2
```

`cost_logsum` is the accessibility-contrib style measure over the costs alone, kept for comparison with
the accessibility literature; it is blind to what can be done at a place and to flexibility.
`beta_1` / `beta_2` are written out because `u*` cannot be recomputed from the csv without them. The
rule is that **every row carries the parameters that produced its own numbers**.

`n_mandatory_act` is the number of activities in the fundamental plan and `n_fixed_act` the non-home
subset of them - the activities that pin the day down, of which there is one less than there are free
periods. They are on every level so that persons with comparably constrained days can be compared:
`n_insertions` only means the same thing for two persons with the same `n_fixed_act`.

`capability_activities.csv.gz` is not a level but the shape of the day the others are relative to. It
carries `soft_start` and `soft_end` per home block: **the block is opaque outside that range and
transparent inside it**, which is how the elastic home is drawn. Consecutive home activities are merged
into one block first, so an agent who never performs a fixed activity is one bar across the whole day
rather than three with meaningless boundaries between them. A fixed activity leaves both columns empty -
its times are known.

`capability_fundamental_plans.csv` is not a level either: it records **how each plan was reduced**, which
is the step every level is computed on top of.

Each day is spelled twice, once by class and once by name, so the rows can be grouped by pattern and
still be read:

| column | what it is |
|---|---|
| `person_id` | |
| `fundamental_activity_chain_class` | the fundamental plan by class, e.g. `home-mandatory-home`. By construction it only ever holds `home` and `mandatory`, which makes it the column to count day patterns over |
| `fundamental_activity_chain` | the same day by name, i.e. what is left after the discretionary activities are dropped, e.g. `home-work-home` |
| `original_activity_chain_class` | the original day by class, e.g. `home-mandatory-discretionary-home` - this is the column that shows *why* the reduction came out the way it did |
| `original_activity_chain` | the original day as the scenario spells it, e.g. `home-work-leisure-home` |

Activity types are normalised so that chains of different agents can be compared and counted: the
typical duration MATSim appends is stripped off (`work_7200` becomes `work`) and every sub-type of home
collapses into plain `home` (`home_morning` and `home_28200` both become `home`), because the morning and
the evening stay at home are the same activity as far as the measure is concerned. Other sub-types are
kept - `educ_primary` says something `educ` does not.

The two **fundamental** chains have their consecutive home stays merged, so a person with no mandatory
activity outside home reads `home` rather than `home-home`: the discretionary activity that sat between
the two home stays is gone, so they never left. It is the same merge `capability_activities.csv.gz` does
to the drawn day, so the chain and the drawing agree. The original chains are left as they are - there,
two home stays are separated by whatever the person actually did in between.

The table has **a row for every person the filter accepted**, including those the analysis then had to
skip because their fundamental plan cannot be analysed - no usable times, a fixed activity past midnight,
no free period left. Those persons have no row in `capability_persons.csv`, so this is the table that
explains the difference between the two row counts.

### How much choice is worth: the two betas

`u*` is a two level nested logsum over the reachable places:

```
u'_k = (1 / b_1) * ln sum_{f in k} exp(b_1 * u_f)      within one sub-type, e.g. one brand
u*   = (1 / b_2) * ln sum_k       exp(b_2 * u'_k)      across the sub-types
```

Every logsum of this shape interpolates between two extremes: `b -> infinity` makes it the plain
**maximum** (a further alternative adds nothing), while `b -> 0` makes it **diverge** like
`mean(u) + ln(N)/b` - the count of alternatives taking over. Note that small `b` is *not* a sum of
utilities; a plain sum is not in this family at all.

The useful reading is the middle one: **`N` identical alternatives are worth `u + ln(N)/b`**. So a beta
is chosen by deciding what a duplicate should be worth:

| `b` | a second identical alternative adds | five identical add |
|---|---|---|
| 0.5 | 1.39 | 3.22 |
| 1 | 0.69 | 1.61 |
| 2 | 0.35 | 0.80 |
| 4 | 0.17 | 0.40 |
| 8 | 0.09 | 0.20 |

Defaults: **`b_1 = 8`, `b_2 = 2`**. A second branch of a chain the person can already reach adds 0.09
utils, a second distinct brand 0.35, and a realistic urban choice set of four brands with two or three
branches each lands around 0.8 - roughly a quarter of the default `u_0 = 3`. Those numbers only hold at
the default utility scale (`u_0 = 3`, `k = 1`, VoT 6/h, so one util is about ten minutes of detour);
re-derive them from `ln(N)/b` if the scale changes.

`b_1 > b_2` is not just intuition - it is the condition for the nested logit to be consistent with
random utility maximisation (McFadden 1978, Daly and Zachary 1978): the dissimilarity parameter
`b_2 / b_1` has to lie in `(0, 1]`. The deeper nest holds the closer substitutes, so it gets the larger
scale. A nest with a single member returns that member's utility exactly, so nothing is inflated by
being alone, and **facilities whose sub-type is unknown share one nest** - missing data must not look
like variety.

### Known limitations

Recorded here because they bound what the numbers mean:

1. **No vehicle continuity.** Each mode is analysed on its own, so a `walk` row means "if this person
   walked the whole detour", not "if they walked while keeping their car chain intact". The realistic
   pattern for a driver - drive home, park, walk to the shop round the corner - is not covered, so
   **walk understates capability for commuters and should be read as a lower bound**. Do not compare walk
   against car head-to-head for commuters until this is addressed. Car is unaffected.
2. **No home-anchored round trips.** Provably no loss for a single mode; a real gap only in combination
   with point 1, which is why the two belong together.
3. **No car availability or reduced mobility.** Every analysed person is assumed able to use every
   configured mode. Age *is* filtered on (`minAge`, default 18), but only to decide who is analysed at
   all - it does not restrict which modes they may use. `CapabilityPersonFilter` is the hook for more.
4. **Car at free speed only**, no congestion. Public transport is not implemented; bike is (see *Modes*).
5. **`maxExtraTravelTime` bounds the detour, not the total trip**, so a place exactly on the way to work is
   never pruned however far away it is.
6. **Several occasions do not add up.** Because insertions → facility is a maximum, a person who could go
   in the morning *or* in the evening scores the same as one who can only go in the evening; `sum_slack`
   is the only trace of the difference. Letting occasions count would mean a third logsum, over the
   insertions of a facility, with a scale larger than both others - the maximum is its limiting case.

A fuller write-up of the definitions and equations lives with the Dresden data, in
`dresden/data/capability-analysis/method.md`.

### Declaring the activity types to analyse

**The types that have a `discretionaryActivity` parameter set are exactly the types the analysis reports
on.** There is deliberately **no default type**: a default would be a type nobody asked for, and because
every analysed person gets a row whether or not they can reach anything, it would fill the output with
plausible-looking zeros instead of saying that nothing was configured. So a config that declares none is
an error, not an empty run:

```xml
<module name="capability">
    <parameterset type="discretionaryActivity">
        <param name="type" value="media" />
        <!-- every other parameter has a default, see DiscretionaryActivityParams -->
    </parameterset>
</module>
```

`type` has to match the **`type` column of the places csv**. Two mistakes are worth knowing about,
because both used to pass unnoticed and now fail fast:

- **a misspelled parameterset type** (`discretionalActivity` for `discretionaryActivity`, say). MATSim
  reads an unknown parameterset as a nameless config group without complaint, so the type it declared
  would simply never be analysed. `CapabilityConfigGroup.createParameterSet` now rejects it by name.
- **a `type` that no facility offers.** This one is legitimate - a type with no places is a result, not a
  mistake - so it stays a warning: *"No facility offers 'x', so no person will have any capability for
  it."* If every row of your output has `n_reachable_facilities = 0`, check that warning and the `type`
  column of the csv first.

### Where discretionary activities can be performed

`DiscretionaryActivityFacility` is the other half of the input: an opportunity the insertion step can try to
place into a gap of a fundamental plan. It carries

| item | where it comes from |
|---|---|
| **type** — shop, leisure, restaurant, bar, ... | facility attribute `capabilityType`, else the types of the facility's `<activity>` options |
| **sub-type** — brand of the supermarket, kind of restaurant | facility attribute `capabilitySubType`, optional |
| **location** — coord and link | the facility itself |
| **opening and closing time** | the `<opentime>` of the matching `<activity>` option, else the `openingTime` / `closingTime` attributes, else always open |

This is a *view* on a MATSim `ActivityFacility`, not a replacement for it — a standard facilities file
already expresses all four items, so the facilities stay readable and writable with
`MatsimFacilitiesReader` / `FacilitiesWriter` and usable by the rest of MATSim:

```xml
<facility id="super-7" x="4621000" y="5820000" linkId="123">
  <attributes>
    <attribute name="capabilityType" class="java.lang.String">shop</attribute>
    <attribute name="capabilitySubType" class="java.lang.String">Konsum</attribute>
  </attributes>
  <activity type="shop">
    <opentime day="wkday" start_time="08:00:00" end_time="20:00:00"/>
  </activity>
</facility>
```

```java
DiscretionaryActivityFacilities places = new DiscretionaryActivityFacilityExtractor(capabilityConfig)
    .extract(scenario.getActivityFacilities());

places.getTypes();                    // [restaurant, shop]
places.getByType("shop");             // every place one can shop at
places.getSubTypes("shop");           // [Konsum, Edeka, ...] - the variety to choose from
```

The **type vocabulary is yours**: it is read from an attribute and does not have to match the MATSim
activity types of the scenario. A bar whose activity option is called `leisure` can be typed `bar`.
Both attribute names are configurable (`facilityTypeAttribute`, `facilitySubTypeAttribute`) so the
extractor can be pointed at annotations a facilities pipeline already produces. A facility that offers
several types — a mall one can both shop and eat at — yields one `DiscretionaryActivityFacility` per type,
each with that type's own opening hours, all sharing one facility id.

The question the insertion step asks is `isOpenDuring(from, to)`: does one single opening window cover
the whole interval? An agent cannot sit in a restaurant across its afternoon closure, even though the
place is open before and after it. `DiscretionaryActivityFacility` also implements MATSim's `Facility`, so
it can be handed to a router directly as a trip end.

### Generating the facilities

The places come from a csv — one row per place and type:

```
id,name,type,sub_type,x,y,opening_time,closing_time
super-7,Konsum Striesen,shop,Konsum,4621000,5820000,08:00:00,20:00:00
super-8,Edeka Blasewitz,shop,Edeka,4622000,5821000,07:00:00,22:00:00
gasthaus-2,Gasthaus Am Markt,restaurant,German,4622500,5821500,11:00:00,23:00:00
bar-3,Nachtcafé,bar,cocktail,4622600,5821600,18:00:00,27:00:00
park-1,Großer Garten,leisure,,4623000,5822000,,
```

```
java -cp matsim-msp.jar org.matsim.contrib.msp.capability.RunCreateDiscretionaryActivityFacilities \
    --input places.csv --output discretionary_activity_facilities.xml.gz --crs EPSG:25833
```

Columns are addressed by name, so their order does not matter. `id` and `type` are required, and so is a
location — `x`/`y`, or `link_id`, or both. Everything else is optional and an empty cell means "not
known": two empty time cells mean open around the clock, an empty `closing_time` means open ended.
Times are a MATSim time string or a number of seconds, and MATSim counts on past midnight, so the bar
above closes at 3 in the morning. A place offering several types gets one row per type, sharing the `id`.
Unreadable cells are refused with the row number, since the file is usually maintained by hand.

Pass `--summary places-out.csv` to have the places written back out after reading, to check what the
command made of the file. `--delimiter` sets the column separator, `--config` points at a config whose
`capability` group names the facility attributes to write.

**The csv is the single entry point on purpose.** A future reader for a different source — an
OpenStreetMap extract, as `contribs/accessibility`'s `AmenityReader` and `CombinedOsmReader` read one —
should map its tags to a `type`, a brand or cuisine to a `sub_type` and `opening_hours` to
`opening_time`/`closing_time`, write *this* csv, and then go through the same command:

```
osm.pbf  --[future OSM step]-->  places.csv  --[RunCreateDiscretionaryActivityFacilities]-->  facilities.xml.gz
```

That way the mapping from a source to the capability vocabulary stays separate from, and inspectable
before, the facility generation — and the hand maintained and the generated case share one code path.
`DiscretionaryActivityFacilityCsv.write(...)` is the target for such a step. Any column the format does not
know is kept as a facility attribute, so a reader can carry more of its source through without the
format having to change.

Two limits of the csv: a row holds one opening window, so a restaurant with a separate lunch and dinner
service needs the facilities file written directly; and a sub-type belongs to the place, so a mall
modelled under one `id` cannot have one brand for its supermarket and another for its restaurant — give
distinct places distinct ids, which is also how OSM models them.

One MATSim quirk worth knowing: `OpeningTimeImpl.compareTo` is an interval relation rather than a
natural ordering, and it reports the earlier of two disjoint windows as the *greater* one — so a
`TreeSet<OpeningTime>` (which is what `ActivityOption.getOpeningTimes()` returns) comes out in reverse,
and its `first()` is the *last* window of the day. `DiscretionaryActivityFacility.getOpeningTimes()`
therefore returns a list sorted by start time.

Please put your code into a sub-package of `org.matsim.contrib.msp` (e.g. one package per project or
per person) and add tests for everything that is meant to be used by someone else.

## Dependencies

Only the dependencies that are actually needed are declared in [pom.xml](pom.xml). MATSim core,
`matsim-examples` (test scope) and JUnit 5 are inherited from the contrib parent pom. If you need
another contrib, add it to `pom.xml` rather than copying code.
