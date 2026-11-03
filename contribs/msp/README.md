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

## Time-varying travel time matrix

`org.matsim.contrib.msp.traveltimematrix` — a car travel time matrix that depends on the time of day
without storing a matrix per time bin. A free-speed matrix is computed once, a congestion index curve is
measured on a finished run, and a query multiplies the two:

```
TT(o, d, t) = TT_ff(o, d) * (1 + beta * CI(t))                            level 0
TT(o, d, t) = TT_ff(o, d) * (1 + beta * sum_k s_k(o,d) * CI_k(t))         level 1
```

`CI` is the **excess travel time index** — the volume-weighted ratio of excess to free-speed travel
time, so `CI = 0.2` means "the average vehicle takes 20% longer than on an empty network". That is
exactly the quantity a multiplier needs, which is why this definition and not a speed performance index
is the one used. `s_k(o,d)` are the **road class shares** of level 1: how the free-speed travel time of
that origin-destination pair splits over the road classes, so that a motorway trip is not slowed down by
congestion that happens in residential streets. `beta` is a calibration factor, 1 by default.

The point is what is *not* stored. There is one travel time per origin-destination pair, not one per
pair and time bin, so the time of day costs a lookup in a curve of a few dozen numbers and a
multiplication. A matrix per 15 minute bin is 96 times the memory and 96 times the routing.

### Drop-in

`TimeVaryingTravelTimeMatrix implements TravelTimeMatrix` of `contribs/dvrp`
(`org.matsim.contrib.zone.skims`), whose `getTravelTime(fromNode, toNode, departureTime)` already takes
a departure time. So it goes wherever a matrix goes already, and nothing on the other side has to know
that the time dimension is now real:

```java
TravelTimeMatrixConfigGroup config = ConfigUtils.addOrGetModule(scenario.getConfig(),
    TravelTimeMatrixConfigGroup.class);
Network carNetwork = StudyAreaNetwork.cutCarNetwork(scenario.getNetwork(), studyArea);

TravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.create(carNetwork, config,
    scenario.getConfig().qsim().getTimeStepSize());

int travelTime = matrix.getTravelTime(fromNode, toNode, 8 * 3600);
```

`create` builds the free-speed base matrix (from the cache file if `matrixCacheFile` names one that
exists), reads the congestion index from `congestionIndexFile`, and computes the road class shares if
the variant needs them. The three pieces can also be passed in by hand —
`TimeVaryingTravelTimeMatrix.global(...)` and `.byRoadClass(...)` take any `TravelTimeMatrix` as the
base, so a zone system from a shapefile or an H3 grid is a `DvrpTravelTimeMatrixParams` away.

### Pipeline

```
run output (network, events)
         |
  step 1 v
   --[StudyAreaNetwork]-->  car subnetwork of the study area, link ids kept
         |
  step 2 v
   --[MeasuredTraffic]-->  congested link TravelTime + VolumesAnalyzer, events read once
         |
  step 3 v
   --[CongestionIndexCalculator]-->  CI per time bin and road class
         |                           -> congestion_index.csv  (RunComputeCongestionIndex)
         v
  step 4  --[FreeSpeedTravelTimeMatrix]-->  TT_ff(o,d), zonal + sparse, cacheable  (contribs/dvrp)
         |
  step 5  --[RoadClassSharesCalculator]-->  s_k(o,d), one byte per class and zone pair   (level 1 only)
         |
         v
   --[TimeVaryingTravelTimeMatrix]-->  TT(o,d,t)
         |
  step 6 v                                                   (evaluation only, see below)
   --[RoutedTravelTimes]-->  the same queries by time-dependent routing
         |
   --[MatrixEvaluation]-->  errors.csv, queries.csv           (RunEvaluateTravelTimeMatrix)
```

Step 2 is a command of its own, because the index is the only thing the matrix needs from a run and it
is tiny:

```
java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.RunComputeCongestionIndex \
    --network output_network.xml.gz --events output_events.xml.gz \
    --shp dresden-with-buffer.shp --output congestion_index.csv
```

The csv has one row per road class and time bin, so it can be read by hand, plotted and put in a paper —
which matters, because the curve *is* the model:

```
road_class,bin_start,congestion_index
all,0,0.004
all,900,0.006
motorway,0,0.002
```

`bin_start` is seconds after midnight, so the bin size can be read off the file and a profile is a
complete input on its own.

The code is split by step of the method:

| package | holds | role |
|---|---|---|
| `traveltimematrix` | `TravelTimeMatrixConfigGroup` | the parameters, including the variant |
| | `TimeVaryingTravelTimeMatrix` | the matrix: free speed times one plus the index |
| | `RoadClassification` | which road classes are distinguished, and which one a link is |
| | `RoadClassShares` | `s_k(o,d)`, one byte per class and zone pair |
| | `RoadClassSharesCalculator` | computes them, by walking each free-speed path back |
| | `RunComputeCongestionIndex` | measures the index on a finished run |
| `traveltimematrix.congestion` | `CongestionIndex` | the interface the matrix reads: `CI(t, road class)` |
| | `CongestionIndexProfile` | the binned curve, and its csv |
| | `CongestionIndexCalculator` | measures it from a `TravelTime` and a `VolumesAnalyzer` |
| `traveltimematrix.prepare` | `StudyAreaNetwork` | cuts the car subnetwork of the study area |
| | `MeasuredTraffic` | reads the events once: congested travel times and volumes |
| `traveltimematrix.evaluation` | `ODSample` | the queries an evaluation runs on, grouped by origin |
| | `RoutedTravelTimes` | the reference (variant 4) and level 3, by routing |
| | `TravelTimeErrorStats` | MAPE, bias, RMSE, accumulated per group |
| | `MatrixEvaluation` | compares the rungs and writes the two csvs |
| | `RunEvaluateTravelTimeMatrix` | the whole experiment as one command |

Dependencies run from the root package into the sub packages and never the other way round, and nothing
in here knows about any particular *use* of the matrix — see [Using it in an
analysis](#using-it-in-an-analysis).

### Study area

A matrix is quadratic in the number of zones and the number of nodes, so a Germany-wide network is not
something one builds a matrix over when the question is about one city. `StudyAreaNetwork` cuts the car
subnetwork of a shape, **keeping the link ids** — which is the whole reason to cut rather than
re-generate: the travel times and volumes measured on the *full* run still address the links of the cut
network, so the congestion index of the study area needs no new simulation. The link attributes are kept
too, so the OSM highway type the road classes are read from survives.

**Give the area a buffer.** Around a city the quickest way between two points often leaves the city and
comes back — the motorway ring is outside the administrative boundary almost everywhere. Cut at the
boundary and those paths are gone, so the matrix reports the trip as slower than it is, in a way that
looks like congestion but is not. The log line of the cut is how to check the buffer was enough: a sharp
drop in links against a modest drop in area means the cut went through the ring.

The index has to be measured over **the same network the matrix is built over**, which is what `--shp`
of `RunComputeCongestionIndex` is for. The congestion of a Germany-wide network says nothing about the
congestion in Dresden.

### The variants

`variant` of the config group, in increasing order of what they know:

| variant | what the index depends on | extra cost |
|---|---|---|
| `FREE_SPEED` | nothing — no congestion at all | none. The null case of an experiment: the error of *this* is what the others have to beat |
| `GLOBAL_INDEX` | the time of day | one lookup and one multiplication per query |
| `BY_ROAD_CLASS` | the time of day and the road classes of the pair | the road class shares, `Z * Z * K` bytes, and a second pass over the network when the matrix is built |

`CongestionIndex.constant(...)` is a fourth baseline worth reporting: a time-independent speed factor,
which is what `DetourTimeEstimator.createMatrixBasedEstimator(speedFactor, ...)` of `contribs/drt` does
today.

The one existing `TravelTimeMatrix` that does use its `departureTime` is DVRP's
`AdaptiveTravelTimeMatrixImpl`, and it answers a different question: it keeps an hourly matrix that is
*updated online* from the route estimates of a running simulation (exponential smoothing), so it needs a
matrix per hour and it needs the simulation. This one is measured once, off a finished run, and keeps a
single matrix.

`evaluateIndexAt` decides *when* during the trip the index is read. `MIDPOINT`, the default, reads it at
departure plus half the free-speed travel time, because a long trip is mostly spent somewhere other than
at its origin: a trip leaving at 07:00 and lasting an hour is in the peak, one leaving at 08:30 and
lasting an hour is coming out of it. `DEPARTURE` is the alternative and is what a time-dependent router
does for the first link of a trip.

### Memory

The zonal part of the base matrix is `Z * Z` unsigned shorts, the sparse part one row per node of the
node pairs within `maxNeighborDistance`, and the road class shares `Z * Z * K` bytes — so the default
six road classes roughly triple the zonal part. Both thresholds are therefore worth a look before a
large run: `cellSize` drives `Z`, and `maxNeighborDistance` drives how much of the network ends up in
the sparse part.

The shares deliberately live on the **zone system of the zonal matrix**, so a short trip that the sparse
node-to-node matrix answers exactly still gets the shares of its enclosing zone pair. That asymmetry is
the design: the travel time of a short trip has to be exact, the road class mix of a short trip is
almost always "local streets" anyway, and giving the shares a node-level matrix of their own would cost
more than the travel times do.

### Evaluating it

`org.matsim.contrib.msp.traveltimematrix.evaluation` — the matrix trades accuracy for one lookup, so the
number that decides whether it is usable is the error. The ladder, from the exact answer down:

| rung | what it is | cost |
|---|---|---|
| `REFERENCE_TIME_DEPENDENT` | **variant 4**: time-dependent shortest paths over the congested travel times of the run | one shortest-path tree per origin *and departure time* — the thing the matrix exists to avoid |
| `LEVEL_3_FIXED_PATH` | **level 3**: the free-speed path, timed link by link under congestion | one tree per origin, plus a walk along the path per departure time |
| `BY_ROAD_CLASS` | level 1 | one lookup per road class |
| `GLOBAL_INDEX` | level 0 | one lookup |
| `CONSTANT_INDEX` | the daily mean index as a constant speed factor — what `DetourTimeEstimator` of `contribs/drt` does today | one multiplication |
| `FREE_SPEED` | no congestion at all | none |

Every one of them is a `TravelTimeMatrix`, the reference included (`RoutedTravelTimes.asTimeDependentMatrix()`),
so `MatrixEvaluation` asks all of them the same three-argument question and knows nothing about which of
them routes. Adding a rung is a map entry.

The gap between variant 4 and level 3 is the one error that no congestion index can remove: a real
driver reroutes around congestion, a fixed free-speed path does not, so **level 3 is an upper bound of
variant 4, and so is every rung below it**. `RoutedTravelTimes` logs that gap as a percentage of the
optimum, which splits the error of the matrix into the part a better index could fix and the part that is
built into the method.

```
java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix \
    --network output_network.xml.gz --events output_events.xml.gz --shp dresden-with-buffer.shp \
    --congestion-index congestion_index.csv --output-directory evaluation \
    --origins 200 --departure-hours 0,3,6,7,8,9,12,15,16,17,18,21
```

One command on purpose: the reference, the index and the matrix all have to be built over the same
network and the same run, and splitting them up is how they come to disagree. The network is cut once,
the events are read once (`MeasuredTraffic`), and the base matrix is computed once and cached in the
output directory, where the four result files land:

- `errors.csv` — MAPE, bias and RMSE per rung, overall and by hour of day and distance band. This is the
  table.
- `queries.csv` — one row per query and rung, for plots.
- `od_sample.csv` — the queries, so that `--od-sample` can repeat the experiment on exactly the same
  pairs with other matrix parameters.
- `congestion_index.csv` — the index that was used, copied next to the results it produced.

**The sample.** The reference costs one tree per origin and departure time, so the error is estimated on
a sample rather than on every pair — which is all a MAPE needs. Two decisions in `ODSample` matter for
reading the result. Pairs are **grouped by origin**, because one tree answers every destination of that
origin at once: 200 origins with 6 destinations each cost the same as 200 with one. And destinations are
drawn **one per distance band** rather than uniformly: uniformly random node pairs are overwhelmingly
medium-range trips through the dense part of the network, which would leave the short trips as a handful
of rows in the very table that is supposed to say whether short trips are the problem.

**Two errors in one number.** The query table carries the exact free-speed travel time of each pair
(`tt_free_speed_path`) next to what each rung answered, because the error of the matrix is two things
added together: the congestion model, which cannot know *where* the queue is, and the zone system, which
answers a long pair with the travel time between the *central nodes* of the two zones. The `FREE_SPEED`
row — whose only error is the discretisation plus the absence of congestion — is what anchors the
comparison, and `cellSize` and `maxNeighborDistance` are what move it.

**What the reference is not.** It is the exact answer *under the travel times of one finished run*, not
measured reality: a route the simulation would never have taken is still optimal here, and a run whose
congestion is wrong yields a reference that is wrong in the same way. Validating the run itself against
route APIs is a different job, and `SampleValidationRoutes` of `contribs/application` is where it lives —
it reads `from_node,to_node` pairs, which is the shape `od_sample.csv` is written in.

### Using it in an analysis

The matrix knows nothing about who uses it, and that is deliberate: it is a `TravelTimeMatrix` and a
congestion index, nothing more. An analysis adapts it to its own travel time abstraction in a few lines
rather than the matrix growing a dependency on the analysis.

For the capability analysis the payoff is not speed but the third argument. Its
`CapabilityTravelTimes` has the right shape already —
`travelTime(Facility from, Facility to, double departureTime)` — and every implementation of it today
ignores the departure time, which is its recorded limitation *"car at free speed only, no congestion"*.
Handing it this matrix is what makes that argument mean something, and it changes results rather than
just timings: whether a shop fits into a gap in the evening depends on how long the trip takes at that
hour.

The adapter belongs in `capability.measure`, so that the dependency runs one way, and is about this
long:

```java
// capability.measure.MatrixCapabilityTravelTimes
record MatrixCapabilityTravelTimes(TravelTimeMatrix matrix, Network network) implements CapabilityTravelTimes {
    @Override
    public double travelTime(Facility from, Facility to, double departureTime) {
        Node fromNode = NetworkUtils.getNearestLink(network, from.getCoord()).getToNode();
        Node toNode = NetworkUtils.getNearestLink(network, to.getCoord()).getFromNode();
        return matrix.getTravelTime(fromNode, toNode, departureTime);
    }
}
```

The dependency then runs **capability → traveltimematrix** and never back, so the matrix can be
reviewed, cited and moved into a contrib of its own without touching anything that uses it, and the two
can be developed on branches of their own.

Note what the adapter has to decide and the matrix deliberately does not: a matrix is **node to node**,
while an analysis asks **facility to facility**. Snapping a coordinate to a link, and adding the time to
traverse the last link, are the caller's business — `VrpPaths.FIRST_LINK_TT` of `contribs/dvrp` is the
convention for the latter, and `CapabilityTravelTimes.FreeSpeedNetwork` already caches the snapping the
analysis needs. Those decisions differ per mode and per analysis, which is exactly why they are not in
here.

### What it is not

Recorded here because it bounds what the numbers mean:

1. **A model of when traffic is slow, not of where.** The index is an aggregate over the network and the
   fleet, so at level 0 two pairs of the same free-speed travel time get the same congested travel time
   however differently they are really affected. Level 1 separates them by the kind of road they use, and
   by nothing else.
2. **The path is held fixed.** The free-speed path is slowed down, while a real driver would route around
   the congestion, so the matrix is biased **upwards** under heavy congestion. Level 3 of the ladder —
   evaluating the time-dependent travel time along the free-speed path — has the same bias, which is why
   the two compare against time-dependent routing on the same footing.
3. **A road class with no traffic has no index.** The index is volume-weighted, so a class that carried
   nothing in a time bin falls back to the network-wide value; the log says how often that happened. A
   class that is empty most of the day is too small to be worth splitting off.
4. **Car only.** The index is measured from car travel times and volumes. Public transport needs a
   different model altogether — a frequency-based one — and is not here.
5. **One day.** The profile is the average day of one run, so it carries no day-to-day variability and no
   reliability measure. Percentile profiles over iterations would be the way to get one.

Level 2 of the ladder — the index by space × road type, which is the one rung that would let the matrix
know *where* the congestion is — is not implemented yet. Level 3 and the time-dependent routing reference
are, in [`traveltimematrix.evaluation`](#evaluating-it); they live there rather than next to the matrix
because both need a router and the network at query time, which is exactly what the matrix refuses to
need.

Please put your code into a sub-package of `org.matsim.contrib.msp` (e.g. one package per project or
per person) and add tests for everything that is meant to be used by someone else.

## Dependencies

Only the dependencies that are actually needed are declared in [pom.xml](pom.xml). MATSim core,
`matsim-examples` (test scope) and JUnit 5 are inherited from the contrib parent pom. If you need
another contrib, add it to `pom.xml` rather than copying code.

`dvrp` is declared for the zonal and sparse matrix of `org.matsim.contrib.zone.skims`, and
`application` for `TrafficStatsCalculator`, which the congestion index is the same quantity as.
