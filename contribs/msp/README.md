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
```

`CI` is the **excess travel time index** — the volume-weighted ratio of excess to free-speed travel
time, so `CI = 0.2` means "the average vehicle takes 20% longer than on an empty network". That is
exactly the quantity a multiplier needs, which is why this definition and not a speed performance index
is the one used. `beta` is a calibration factor, 1 by default.

There used to be a **level 1**, weighting one index per road class by the share of the pair's free-speed
travel time spent on each. It was removed, and the reason is the finding of the second round of
experiments: splitting the index by road class moves the estimate closer to the travel times the
*simulation recorded*, while the problem it was meant to solve is that the simulation records **no
congestion at all** on the links an accessibility analysis mostly asks about. No re-weighting of what was
measured can fix that — see [Why `TRIPS` is the default](#evaluating-it). The road *classification*
survives as a diagnostic on the index itself (`--road-classes`), which is cheap and informative; the
per-pair shares cost 3.2 GB on Dresden and informed nothing.

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
         v
   --[TimeVaryingTravelTimeMatrix]-->  TT(o,d,t)       (RunComputeTravelTimeMatrix: build + cache)
         |
  step 5 v                                                   (evaluation only, see below)
   the questions:  [ODSample] generated pairs | [ExperiencedLegs] the trips of the run
   the references: [RoutedTravelTimes] free speed | a time-dependent router | what those trips took
         |
   --[MatrixEvaluation]-->  errors.csv, queries.csv           (RunEvaluateTravelTimeMatrix)
```

`RunMatrixCostExperiment` is a loop around steps 4 and 5 with the congestion model switched off — see
[What a matrix costs](#what-a-matrix-costs-parts-1-and-2).

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
| | `MatrixOptions` / `RoutingOptions` | the command-line options that shape a matrix, shared by every command |
| | `TimeVaryingTravelTimeMatrix` | the matrix: free speed times one plus the index |
| | `RoadClassification` | which road classes are distinguished, and which one a link is |
| | `RunComputeCongestionIndex` | measures the index on a finished run |
| | `RunComputeTravelTimeMatrix` | builds and caches the matrix of a study area |
| `traveltimematrix.congestion` | `CongestionIndex` | the interface the matrix reads: `CI(t, road class)` |
| | `CongestionIndexProfile` | the binned curve, and its csv |
| | `CongestionIndexCalculator` | measures it from a `TravelTime` and a `VolumesAnalyzer` |
| `traveltimematrix.prepare` | `StudyAreaNetwork` | cuts the car subnetwork of the study area |
| | `StudyAreaOptions` | `--network`, `--shp`, `--modes`: one place that reads the network |
| | `MeasuredTraffic` | reads the events once: congested travel times and volumes |
| `traveltimematrix.evaluation` | `ODSample` | the queries an evaluation runs on, grouped by origin |
| | `RoutedTravelTimes` | free speed, variant 4 and level 3, by routing |
| | `TravelTimeErrorStats` | MAPE, bias, RMSE, accumulated per group |
| | `ExperiencedLegs` | the car legs a run drove, as node-to-node travel times |
| | `TravelTimeQuery` | the pair and time every comparison is made of, what is known about it, and its csv |
| | `MatrixEvaluation` | compares every variant against every reference, and writes the tables |
| | `CongestionExposure` | how much congestion the queries carry against how much the index applies |
| | `RunEvaluateTravelTimeMatrix` | the error of one matrix, as one command |
| `traveltimematrix.experiments` | `RunMatrixCostExperiment` | parts 1 and 2: study area size × cell size × radius against accuracy, runtime and memory |
| | `RoutingBaseline` | the router the matrix has to beat, timed isolated and amortised by origin |
| | `AccuracyByDistance` | the error of one answerer, overall and per distance band |
| | `MatrixFootprint` | how much memory a configuration needs, before building it |
| | `QueryBenchmark` | query rates at a ladder of counts, under a wall-clock budget |

Dependencies run from the root package into the sub packages and never the other way round, and nothing
in here knows about any particular *use* of the matrix — see [Using it in an
analysis](#using-it-in-an-analysis).

### Building it for a study area

There is no matrix file to produce — the matrix is a library object, built in a few lines wherever it is
used (see [Drop-in](#drop-in)). What there is to do beforehand is to compute the expensive part once and
to find out whether the thresholds fit the network:

```
# 1. the congestion index, off the run
java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.RunComputeCongestionIndex \
    --network output_network.xml.gz --events output_events.xml.gz \
    --shp dresden-with-buffer.shp --output congestion_index.csv

# 2. the matrix: build it, cache the free-speed part, report what it cost
java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.RunComputeTravelTimeMatrix \
    --network output_network.xml.gz --shp dresden-with-buffer.shp \
    --congestion-index congestion_index.csv --output matrix \
    --cell-size 1000 --max-neighbor-distance 5000
```

The free-speed base matrix is the only slow step and does not depend on the index, so it is cached:
every later run — an evaluation, a parameter sweep, the analysis itself — reads it back instead of
routing the zone system again. `matrix_summary.csv` gets one row per invocation, so a sweep is a loop in
the shell and the result is the table that decides `cellSize` and `maxNeighborDistance`:

```
variant,cell_size,max_neighbor_distance,links,nodes,zones,zonal_matrix_bytes,road_class_shares_bytes,cache_file_bytes,build_seconds,microseconds_per_query
GLOBAL_INDEX,2000,5000,23,15,14,392,0,1264,0.0,1.527
```

**The cache carries its thresholds in its file name** (`free_speed_matrix_cell1000_nb5000.bin`), and that
is not cosmetic. A cache is only valid for the zone system and the neighbourhood it was built with:
change `cellSize` and `FreeSpeedTravelTimeMatrix` throws a bare `VerifyException` from inside itself,
change `maxNeighborDistance` and **nothing complains at all** — the sparse part is read back as it was
written, so short trips would quietly be answered with a previous run's threshold. Use
`config.setMatrixCacheDirectory(dir)` rather than naming the file by hand and the two cannot be
confused.

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

#### "Undefined Time. Reason could be that the dvrp network is not fully connected"

If the matrix ever throws this, the cut network is not strongly connected — a zonal matrix refuses a pair
it finds no path for, and it says so only once it reaches that pair, after computing everything else.
**Cleaning the network for the mode does not prevent it**, for two reasons, and `StudyAreaNetwork.cut`
deals with both:

1. **The cleaner takes the mode away from a link; it does not remove the link.**
   `MultimodalNetworkCleaner` deletes a link outside the connected cluster only if *no* allowed mode is
   left on it. A car dead-end that also allows `ride` — on a network from the usual OSM pipeline, nearly
   every car dead-end there is — therefore stays as a `ride` link, with its node. And the router does not
   care: `SpeedyGraphBuilder` builds its graph from **all** links "without caring about allowedModes". So
   the matrix routes over a graph that still has the dead-end, picks the node behind it as a zone's
   central node, and finds no way out. The cut therefore removes every link that no longer allows one of
   the requested modes, and then the nodes left without links.
2. **A turn restriction of another mode.** `cleanNetwork(network, modes)` keeps only the restrictions that
   do not disconnect the network *for those modes*, while the matrix's router merges the restrictions of
   **every** mode (`SpeedyGraphBuilder.build(network)` passes no mode, and its own javadoc notes this
   "could result in a network which is more restrictive than necessary"). A truck-only restriction can
   thus cut a node off in the matrix's graph while the links look perfectly connected and the network is
   properly cleaned for car. The cut drops all turn restrictions when that happens, and says so in the
   log: it costs a few paths a few seconds of detour, which is far inside the error budget of a matrix
   scaled by a network-wide index, and the network of the run is untouched because the cut is a copy.

What is left is checked rather than assumed: `StudyAreaNetwork.verifyRoutable` runs two shortest-path
searches — forwards and backwards from one node, over `SpeedyGraphBuilder` and `LeastCostPathTree`, which
is the very graph the matrix will route — and refuses the network naming the nodes that are cut off. Two
searches, instead of finding out thousands of searches into the matrix. It is public, so a network built
some other way can be checked the same way before handing it to `TimeVaryingTravelTimeMatrix.create`.

### The variants

`variant` of the config group, in increasing order of what they know:

| variant | what the index depends on | extra cost |
|---|---|---|
| `FREE_SPEED` | nothing — no congestion at all | none. The null case of an experiment: the error of *this* is what the others have to beat |
| `GLOBAL_INDEX` | the time of day | one lookup and one multiplication per query |

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
number that decides whether it is usable is the error. One command measures it,
`RunEvaluateTravelTimeMatrix`, and it has exactly two choices to make: **which questions to ask**
(`--od-pairs`) and **what to compare against** (`--references`, a list).

**`--od-pairs`: which questions to ask.** `TRIPS` — **the default** — takes the car trips the run really
made and weights the error by what actually happens. `RANDOM` generates pairs, one destination per
distance band and origin, and covers the matrix evenly — including the pairs and hours nobody drove in
this scenario, which for an analysis asking *"could this person have reached that shop at 19:00"* is most
of them. `FILE` reads back the `od_queries.csv` of an earlier run, so two experiments can be compared on
exactly the same questions.

**Why `TRIPS` is the default, and this is a result and not a preference.** The congestion index is
*volume-weighted*: it answers "how much longer does the average **vehicle** take", so an empty
residential street does not dilute it. The matrix then applies it to whatever pair it is asked about, and
a pair is not a vehicle. A generated pair is mostly made of links the run never put a vehicle on; those
links were never slowed down, so they carry no excess travel time in any reference measured on that run —
while the index, measured where the traffic was, still says the trip should be several percent slower.
Applied to such a sample the index **overestimates**, and on a uniform sample of node pairs it can score
*worse than using no congestion model at all*, which is exactly what the first round of experiments
found. That is a property of the questions, not of the method, and it is the reason the default pairs are
the trips that happened.

It is also the reason splitting the index by road class — the level 1 that used to be here — is not the
fix, and why it was removed rather than tuned. Grouping the roads
moves the estimate closer to the travel times the run recorded; it does not add the congestion the run
never recorded in the first place. The honest fix for the pairs nobody drove is an external travel time —
see [Against the real world](#against-the-real-world---references-api).

`congestion_exposure.csv` is the measurement of all of this, written next to the error table:

| column | what it is |
|---|---|
| `realized_index` | the excess travel time the **reference** carries, `sum(TT_ref − TT_ff) / sum(TT_ff)` — the same ratio-of-sums form as the congestion index itself, weighted by the queries of this evaluation instead of by links and volumes. The index these questions *deserve*. |
| `applied_index` | `beta · CI` as the matrix read it, over the same denominator: `sum((factor − 1) · TT_ff) / sum(TT_ff)`. The index these questions *got*. |
| `index_bias` | `applied − realized`, in index points — the share of the travel time the congestion model adds in error. |
| `share_without_excess` | how many queries carry no excess travel time at all. The mechanism, counted: small on real trips, most of the sample on generated pairs. |

The rows are grouped exactly like `errors.csv` (overall, by hour, by distance band), so the two files
join on `reference,group,group_value` and a plot can put the error of a group next to the reason for it.
Against `FREE_SPEED_ROUTING` the realized index is 0 by construction, which makes that row the sanity
check of the table.

**`--references`: what to compare against — and it is a list, not a choice.** The matrix is wrong in two
separable ways, the zone system and the congestion model, and no single reference can tell them apart.
So every variant is reported against every chosen reference, and the result is a grid. Left out, it
follows from the pairs: all three with `--od-pairs TRIPS`, and the two routed ones otherwise, `EXPERIENCED`
being impossible for a pair nobody drove.

```
                       │ FREE_SPEED_ROUTING  │ CONGESTED_ROUTING   │ EXPERIENCED
                       │ (no congestion)     │ (router on the run) │ (what trips took)
───────────────────────┼─────────────────────┼─────────────────────┼─────────────────────
 CONGESTED_ROUTING     │          ·          │ self-check, = 0     │ router vs reality
 LEVEL_3_FIXED_PATH    │          ·          │ cost of a fixed path│          ·
 FREE_SPEED  (matrix)  │ the zone system     │ no congestion model │ no congestion model
 GLOBAL_INDEX          │          ·          │ level 0             │ level 0
```

The rows are the variants (the approximations under test), the columns the references (what "right"
means). Neither is privileged — the free-speed matrix is nearly exact in the first column and badly
wrong in the last, and those are two different questions. Four cells are worth naming:

1. **Is the comparison wired up?** `CONGESTED_ROUTING` against the `CONGESTED_ROUTING` *variant*. The
   same router on the same travel times appears on both sides of the subtraction, so the only error that
   may show up is the rounding of a travel time to whole seconds. It costs nothing — the variant is a
   lookup in what was routed anyway — and it is the row to glance at before believing any of the others.
   Against `EXPERIENCED` that same row stops being a tautology and becomes the most informative one in
   the table: it is **the router against reality**, i.e. everything that is already wrong before the
   matrix is involved at all.
2. **What do the zones cost?** `FREE_SPEED_ROUTING` against the `FREE_SPEED` variant. No congestion on
   either side, so the only thing left to be wrong about is the matrix's own discretisation: the zonal
   part answers a long pair with the travel time between the *central nodes* of two zones. This is the
   **error floor** of every other variant in the table, and `--cell-size` and
   `--max-neighbor-distance` are what move it. It needs no events and no run — just a network.
3. **What does the congestion model cost?** `CONGESTED_ROUTING` (or `EXPERIENCED`) against the matrix
   variants, up the ladder: `FREE_SPEED` is the null case with no congestion model at all, `GLOBAL_INDEX`
   is level 0.
4. **Is the time variation what helps, or just the scaling?** `--with-constant-index` adds
   `CONSTANT_INDEX`, the daily mean index as a constant factor — what `DetourTimeEstimator` of
   `contribs/drt` does today. `FREE_SPEED` → `CONSTANT_INDEX` is what *modelling congestion at all*
   buys; `CONSTANT_INDEX` → `GLOBAL_INDEX` is what *the time of day* buys. It is off by default because
   it is a variation of `FREE_SPEED` with a different factor, and on a scenario with little congestion it
   can beat the time-varying curve.

### The exact matrix: `--max-neighbor-distance inf`

How coarse the matrix is (`--cell-size`, `--max-neighbor-distance`) is not one of the two choices above
but the thing being judged — and one setting of it deserves naming, because it changes what the
evaluation *measures* and not merely how well it scores.

With `--max-neighbor-distance inf` every pair is stored node to node. No zone system is built at all,
`--cell-size` is ignored, and the matrix's discretisation error is **zero by construction** — cell 2 of
the table above comes out at a MAPE of 0 and an MAE under a second, that second being the truncation of a
travel time to a whole number. What is then left against `CONGESTED_ROUTING` is the error of the
**congestion model and of nothing else**, which is the quantity the method is about.

That separation is not available from a zonal run, and this is the reason the configuration exists. The
discretisation error is *scatter*, not bias — 5 to 8 % MAPE at a 500 m to 1 km cell on Dresden, with a
bias of a few seconds — so it does not add to the congestion error in any way one can subtract back out.
One matrix corrected by one index, measured exactly, is a different claim from the same thing measured
through a 7 % blur.

```
java -Xmx12g -cp matsim-msp.jar \
    org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix \
    --network output_network.xml.gz --events output_events.xml.gz --shp dresden-with-buffer.shp \
    --congestion-index congestion_index.csv --output-directory evaluation-exact \
    --max-neighbor-distance inf \
    --od-pairs TRIPS --references FREE_SPEED_ROUTING,CONGESTED_ROUTING,EXPERIENCED --with-constant-index
```

Swap in `--od-pairs RANDOM` to see the other half of the story: the same matrix on pairs nobody drove,
where `congestion_exposure.csv` shows the index being applied to trips that were never slowed down.

What it costs, and what it gives up:

- **One shortest-path tree per node**, fully relaxed. On the 23,000 node Dresden study area that is under
  a minute on a few threads.
- **About 4.7 GB** on Dresden, and not the 3.2 GB the pair count alone suggests: every sparse row also
  keeps a `BitSet` indexed by the *global* node id index, which cutting a study area out of a
  Germany-wide network leaves spread over half a million, so the bit sets alone are 1.5 GB.
  `MatrixFootprint` estimates this before anything is built — exactly, in this corner, there being no
  sampling and no zonal half to guess at. Give the JVM 12 GB, the evaluation also holding the congested
  travel times and the routed reference. Dropping the zonal half saves *build time*, not memory: at a
  2 km cell it was 378² × 2 bytes.
- **No cache.** The format of `contribs/dvrp` starts with the zone system this configuration has none of,
  and a travel time per node pair would be a 2 GB file to spare under a minute of routing.

`NodeToNodeTravelTimeMatrix` is its own class because `FreeSpeedTravelTimeMatrix` of `contribs/dvrp`
cannot express this: that class always builds the zonal half and consults the sparse half only as a fast
path in front of it. Hand it an unbounded radius and it does answer every pair node to node — after
routing a complete zone-to-zone matrix that no query can ever reach.

The references typically have **opposite signs**: against a router the matrix comes out too slow,
because it holds the free-speed path fixed, and against the experienced travel times it can come out too
fast, because a network-wide index does not see the queue a particular trip sat in. Both go in the same
table, with the same columns, which is the point of them being a list.

```
java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix \
    --network output_network.xml.gz --events output_events.xml.gz --shp dresden-with-buffer.shp \
    --congestion-index congestion_index.csv --output-directory evaluation \
    --od-pairs TRIPS --references FREE_SPEED_ROUTING,CONGESTED_ROUTING,EXPERIENCED
```

Both of those are the defaults, so the two last lines can be left off.

The one combination that cannot work is refused before anything is read: `EXPERIENCED` needs
`--od-pairs TRIPS`, because a pair nobody drove has no experienced travel time.

Five files come out of `--output-directory`: `errors.csv` (MAPE, bias and RMSE per reference and
variant, overall and by hour and distance band — this is the table), `congestion_exposure.csv` (how much
congestion the queries carry against how much the index applies to them — this is *why* the table reads
the way it does), `od_queries.csv` (the questions and what is known about them, for `--od-pairs FILE`),
`congestion_index.csv` (the index that was used, next to the results it produced) and, with
`--write-queries`, one row per query and variant — with one column per reference — for plots.

#### The ladder, and what each rung costs

| rung | what it is | cost |
|---|---|---|
| `CONGESTED_ROUTING` | **variant 4**: time-dependent shortest paths over the congested travel times of the run | one shortest-path tree per *distinct origin and departure time* |
| `LEVEL_3_FIXED_PATH` | **level 3**: the free-speed path, timed link by link under congestion | one free-speed tree per origin, plus a walk along the path |
| `FREE_SPEED` | no congestion at all | none |
| `CONSTANT_INDEX` | the daily mean index as a constant factor (`--with-constant-index`) | one multiplication |
| `GLOBAL_INDEX` | level 0 | one lookup |

Every one of them is a `TravelTimeMatrix`, the routed ones included, so `MatrixEvaluation` asks all of
them the same three-argument question and knows nothing about which of them routes. Adding a rung is a
map entry. The top two appear only when something was routed under congestion, i.e. when
`CONGESTED_ROUTING` is among the references.

On `equil`, with the real trips as the questions and all three references:

```
reference,variant,group,group_value,n,mean_reference,mean_estimate,bias,relative_bias,mape
EXPERIENCED,CONGESTED_ROUTING,all,all,20,1272.4,1188.0,-84.4,-0.1147,0.1147
EXPERIENCED,LEVEL_3_FIXED_PATH,all,all,20,1272.4,1277.7,5.3,0.0167,0.0609
EXPERIENCED,FREE_SPEED,all,all,20,1272.4,1188.0,-84.4,-0.1147,0.1147
EXPERIENCED,GLOBAL_INDEX,all,all,20,1272.4,1232.0,-40.4,-0.0502,0.0662
CONGESTED_ROUTING,CONGESTED_ROUTING,all,all,20,1188.0,1188.0,0.0,0.0000,0.0000
CONGESTED_ROUTING,LEVEL_3_FIXED_PATH,all,all,20,1188.0,1277.7,89.7,0.1661,0.1661
CONGESTED_ROUTING,GLOBAL_INDEX,all,all,20,1188.0,1232.0,44.0,0.0815,0.0815
FREE_SPEED_ROUTING,FREE_SPEED,all,all,20,1188.0,1188.0,0.0,0.0000,0.0000
```

Read it as: the self-check is 0, so the comparison is wired up. The zone check is 0 too — `equil` has
fifteen nodes spread far apart, so every one of them is the central node of its own 1000 m cell and the
zonal matrix is exact; on a real network this row is where `cellSize` shows up. A time-dependent router
is itself 11% faster than the trips really were, so most of the gap to `EXPERIENCED` is not the matrix at
all but route choice and queueing; timing the *fixed* path is within 2% of the trips, because agents do
not re-optimise either; and the matrix recovers half of what `FREE_SPEED` gets wrong.

Note what `CONGESTED_ROUTING` costs with `--od-pairs TRIPS`: real trips depart at distinct seconds, so it
is about one shortest-path tree per trip, where generated pairs share their departure times and cost one
tree per origin and hour. `--max-trips` is the knob — a reservoir sample over the whole day, so capping
it does not turn the sample into "the morning peak". `FREE_SPEED_ROUTING` has no such cost: a free-speed
path does not depend on when the trip starts, so it is one tree per origin however many departure times
are asked about.

#### What is compared, exactly

**A trip is cut at the nodes, not taken as the agent experienced it.** The qsim inserts a vehicle at the
end of its first link and ends the leg once it has driven its last one, so a leg's own travel time
contains a first-link insertion delay and a full last link, neither of which is in a node-to-node
matrix. `ExperiencedLegs` therefore starts the clock when the vehicle **leaves its first link** (passing
`toNode(startLink)`) and stops it when it **enters its last** (passing `fromNode(endLink)`) — precisely
the interval `getTravelTime(fromNode, toNode, t)` is about, and the departure time it is asked about is
the moment the first node was passed. No correction term, no last-link convention, nothing to get wrong.
This is also why the comparison is not made against `output_trips.csv`, whose travel time includes the
access and egress walk, nor against a leg's `trav_time`. Experienced plans are the same story: they carry
a leg's travel time but not the instants it crossed those two nodes, so the events are the input that can
answer the question exactly.

Only trips that **stayed inside the study area** count: for one that left the area and came back, the
matrix would answer about a path through the area that the vehicle never drove, and scoring that as an
error of the congestion model would be wrong. How many were dropped for it is logged, and a large share
is the sign that the buffer around the city is too small.

A `TravelTimeQuery` carries nothing called "the" reference, and that is deliberate: it carries a
**measured** travel time (`observed_travel_time` — what the trip took, or what someone fetched from an
API) and a **free-speed** one, and routing a query that came from a real trip adds the second without
touching the first. That is what lets one run compare the same trip against what it took *and* against a
router.

#### Against the real world (`--references API`)

No implemented reference is reality. They are all measured against one run, and the congestion index was
measured from the same events, so a run that is wrong about Dresden's congestion gives a whole grid of
happy numbers. Validating the **run** needs external data, and `API` is where that will plug in — one
method, because everything else about the command is independent of where a reference travel time comes
from.

It can be done today without any new code, which is why it is not urgent: run with
`--references FREE_SPEED_ROUTING`, take the `od_queries.csv`, fetch the travel times of those pairs with
`SampleValidationRoutes` of `contribs/application` (Google, HERE, TomTom, Mapbox per hour — it reads
`from_node,to_node`, which is the shape of that file), write them into its `observed_travel_time`
column, and evaluate with `--od-pairs FILE --references EXPERIENCED`. That is the same comparison, by
hand. `TravelTimeValidationRunner` of `contribs/analysis` is the other tool in that area.

### What a matrix costs (parts 1 and 2)

`org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixCostExperiment` — **one** sweep over three
axes, because two separate questions share every mechanism: *how far does a full matrix scale and from
how many queries does it pay off*, and *what do zones cost in accuracy to get past that wall*.

```
# part 1: the full matrix against the size of the study area
java -Xmx60g -cp matsim-msp.jar \
    org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixCostExperiment \
    --network germany.xml.gz --shp dresden-with-buffer.shp --output network-size \
    --radii 5000,10000,20000,30000,40000,50000,75000,100000,200000

# part 2: the resolution grid over one study area
java -Xmx12g -cp matsim-msp.jar (same class) \
    --network output_network.xml.gz --shp dresden-with-buffer.shp --output resolution \
    --cell-sizes 2000,1000,750,500,250,100 --max-neighbor-distances 5000,10000,15000,20000,inf \
    --origins 500
```

| axis | option | what it is for |
|---|---|---|
| study area size | `--radii` | the same network cut to a disc of each radius around `--centre`, so the node count is swept without swapping scenarios. Left out, the area is whatever `--shp` says. |
| zonal resolution | `--cell-sizes` | left out, no zonal half is built and only the exact matrix is measured — which is what part 1 wants |
| exact radius | `--max-neighbor-distances` | `inf` by default: every pair node to node, so the matrix is exact |

**The network is read once and cut repeatedly.** A Germany-wide file is tens of megabytes of gzipped xml,
and reading it per radius would be most of the runtime of the small areas. The areas are cut smallest
first, so an interrupted sweep has finished the ones that fit. **No shape file per radius is needed**:
`StudyArea.circle(centre, radius)` is the same predicate `StudyArea.fromShapeFile(..)` is, behind one
interface, and `--centre` defaults to the centroid of `--shp`.

**Memory is the binding constraint, not time.** A full matrix costs about `nodes² × 6.1` bytes — 4.7 GB at
Dresden's 22,948 nodes, 61 GB at 100,000 — so `--memory-budget` (default 64 GB, capped at 60 % of `-Xmx`)
is what decides how far the sweep gets. A configuration over it is **refused rather than attempted**: the
sweep writes a `skipped_over_memory_budget` row and goes on, instead of running out of heap on
configuration 28 of 31 and losing the 27 that worked. For part 1 those rows are not a failure but *the
result* — the node count at which a full matrix stops fitting is the wall that makes zoning necessary at
all, which is exactly where part 2 begins. `--build-timeout` (default 90 min) is a backstop only.

> **One caveat about memory when sweeping radii in a single JVM.** Every sparse row keeps a `BitSet`
> indexed by the *global* node id index, which on a Germany-wide network is spread over half a million
> however small the cut is — so the bit sets cost `nodes × idSpace / 8` rather than `nodes² / 8`.
> `MatrixFootprint` estimates and logs exactly that, so the budget check is honest, but the memory curve
> of a single-JVM sweep carries a linear term that is an artifact of the id space. For the memory figures
> that go in a paper, run one radius per JVM over a written-out cut network.

The **zonal** half answers a pair with the travel time between the central nodes of two square cells, so
`cellSize` trades accuracy for the square of the memory. The **sparse** half answers a pair exactly, node
to node, as long as the two are within `maxNeighborDistance`, so that radius trades memory for exactness
on short trips. Everything is at **free speed**, which is what makes the experiment clean rather than
confounded: with no congestion model in play, the only thing a variant can be wrong about is its own
spatial discretisation. The reference is therefore exact — a free-speed shortest path per sampled pair —
and it is computed **once** and reused by every configuration, because the free-speed travel time of a
pair cannot depend on how the matrix is cut up.

**The two degenerate corners of the resolution grid are different things**, and both are in the table:

- `maxNeighborDistance = inf` is a real configuration: every pair is stored node to node, so the matrix
  is *exact* and `cellSize` stops mattering — no zone system is built at all, and the row's `cell_size`
  and `zones` are reported empty to say so. It is the most expensive corner, the row that says at what
  radius the approximation disappears, and the configuration in which the congestion model can be
  measured on its own. Exactly **one** row, however many cell sizes are swept.
- **No matrix at all** is not a cell of the grid but a baseline row. Its accuracy *is* the reference, so
  that row coming out at zero is the self-check of the experiment; its interest is the query time, which
  is the number the matrix exists to beat.

Routing is timed **twice**, because there are two honest ways to count it. `DIRECT_ROUTING` runs one
shortest-path search per query — what an analysis that asks about pairs in no particular order pays.
`DIRECT_ROUTING_BY_ORIGIN` sorts the queries by origin first, so one search answers every destination of
that origin — what a careful analysis pays, and a far harder target. On Dresden the two differ by a
factor of 87, so quoting only the first would flatter the matrix by two orders of magnitude. The baseline
is measured **per area**, before the matrices of that area — both it and the build get more expensive as
the network grows, the whole question of part 1 is which grows faster, and running it first is what lets
every matrix row carry its own `break_even_queries_isolated` and `break_even_queries_by_origin`: the
query count at which the build time has paid for itself.

#### What comes out

Three tables, appended row by row as the sweep runs, so an interrupted sweep is still usable:

Three tables, every row starting with `area, radius, links, nodes, cell_size, max_neighbor_distance,
zones` so that all three join on the same key:

| file | one row per | holds |
|---|---|---|
| `matrix_cost.csv` | area × configuration (and the two baselines) | `zonal_bytes`, `sparse_bytes`, `total_bytes`, `build_seconds`, `microseconds_per_query`, `break_even_queries_isolated`, `break_even_queries_by_origin`, `status` |
| `matrix_accuracy.csv` | area × configuration × distance band | `bias`, `error_std_dev`, `mean_absolute_error`, `relative_bias`, `mape`, `rmse`. Not written at all when `--origins` is 0, which is the default — an exact matrix has no error worth a table |
| `matrix_query_scaling.csv` | area × configuration × query count | the rate at 10k, 100k, 1M, 10M — so a rate that has not settled is visible rather than averaged away |

A first run over the Dresden study area (54,094 links, 22,948 nodes), two configurations and a 15 s query
budget:

```
answerer,cell_size,max_neighbor_distance,zones,build_seconds,queries,microseconds_per_query
MATRIX,2000,5000,378,10.09,2000000,0.0451
MATRIX,1000,5000,1183,9.73,2000000,0.0305
DIRECT_ROUTING,,,,,5760,2614.5522
DIRECT_ROUTING_BY_ORIGIN,,,,,498944,30.0644

answerer,cell_size,max_neighbor_distance,distance_band,n,bias,error_std_dev,mean_absolute_error,mape
MATRIX,2000,5000,all,240,-2.5,159.6,95.5,0.1092
MATRIX,2000,5000,0-1000,22,0.0,0.0,0.0,0.0000
MATRIX,2000,5000,1000-2000,28,0.0,0.0,0.0,0.0000
MATRIX,2000,5000,2000-5000,37,47.2,115.3,76.5,0.1967
MATRIX,1000,5000,all,240,-4.5,132.8,61.8,0.0734
MATRIX,1000,5000,2000-5000,37,17.7,61.6,36.2,0.0898
DIRECT_ROUTING,,,all,240,0.0,0.0,0.0,0.0000
```

Three things to read out of it, and the first two are the reason the table has the columns it has:

1. **The error is all scatter and no bias.** At `cellSize 1000` the bias is −4.5 s against a standard
   deviation of 133 s. A calibration factor can remove a bias and can do nothing whatever about a
   standard deviation, so the zone error is not something a fitted coefficient will rescue — which is
   exactly why `bias` and `error_std_dev` are reported separately and why `rmse² = bias² + sd²`.
2. **The sparse half is exact where it reaches, and its reach is shorter than the number suggests.** The
   0–1 km and 1–2 km bands are at exactly zero error, as they must be. But the 2–5 km band is *not*,
   although 5 km is the nominal `maxNeighborDistance` — the sparse rows are built by a search ordered by
   travel *time* that stops once the accumulated *distance* reaches the threshold, so nodes near the
   cut-off are not fully relaxed. The effective exactness radius is therefore meaningfully smaller than
   `maxNeighborDistance`, which is a thing to say in the paper rather than a thing to work around.
3. **The matrix is ~1000× faster than a well-used router, not ~60,000×.** 0.03 µs against 30 µs
   amortised by origin; the 2.6 ms figure is the same router used badly.

#### What keeps the sweep alive

The expensive corner of the grid is real: at `cellSize 100` the zone count approaches the node count, so
the zonal half approaches `nodes² × 2` bytes, and an unbounded neighbourhood stores `nodes²` pairs at six
bytes each. On a 23,000 node area that is a few GB, and finding out on configuration 28 of 31 is how a
sweep is lost. So `MatrixFootprint` estimates both halves **before** building — the zone count exactly,
the neighbourhood by sampling straight-line distances, which over-estimates because network distance is
never shorter — and a configuration over `--memory-budget` (60% of `-Xmx` by default) gets a row saying
`skipped_over_memory_budget` rather than an `OutOfMemoryError`. Configurations run cheapest first for the
same reason.

**The sparse half has one cost that is easy to estimate wrongly**, and it is worth knowing about because
it bit this code once. Every sparse row keeps a `BitSet` to reject a non-neighbour in constant time, and
that bit set is indexed by the **global node-id index**, not by a position inside the study area. Node id
indices are handed out as ids are created and are never compacted, so cutting a 23,000 node study area
out of a 450,000 node Germany-wide network leaves the indices spread over all 450,000 — and each row's
bit set is then twenty times what the study area alone suggests: 1.3 GB rather than 66 MB on Dresden.
`MatrixFootprint` therefore estimates against `Id.getNumberOfIds(Node.class)` and logs the index space it
used. If it ever matters, writing the cut subnetwork to a file and running over *that* in a fresh JVM
collapses the index space to the study area.

`--query-budget` (60 s by default) is the same idea for time: a benchmark stops at the query target or
the budget, whichever comes first, and says which in `stopped_on_budget`. Without it the direct-routing
baseline would need about 18 days to reach 10 million queries.

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
3. **Two nodes in the same pair of zones get the same answer.** Above `maxNeighborDistance` the base
   matrix answers with the travel time between the *central nodes* of the two zones, so the error has a
   floor that has nothing to do with congestion and does not go away at any level of the ladder. It is
   measured on its own by the free-speed check of the
   [evaluation](#evaluating-it) — `--references FREE_SPEED_ROUTING` against the `FREE_SPEED` variant —
   and `cellSize` and `maxNeighborDistance` are what trade it against memory. It is the one limitation
   here that can be removed outright rather than traded: `maxNeighborDistance = inf` stores every pair
   node to node and the floor goes to zero, at about 4.7 GB on Dresden. See
   [the exact matrix](#the-exact-matrix---max-neighbor-distance-inf).
4. **A road class with no traffic has no index.** The index is volume-weighted, so a class that carried
   nothing in a time bin falls back to the network-wide value; the log says how often that happened. A
   class that is empty most of the day is too small to be worth splitting off.
5. **Car only.** The index is measured from car travel times and volumes. Public transport needs a
   different model altogether — a frequency-based one — and is not here.
6. **One day.** The profile is the average day of one run, so it carries no day-to-day variability and no
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
