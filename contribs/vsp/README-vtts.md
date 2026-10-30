# Single-run VTTS dashboard

Install the calculation in the scenario's controller, alongside SimWrapper:

```java
controler.addOverridingModule(new SimWrapperModule());
controler.addOverridingModule(new VttsModule());
```

`VttsModule` is in `org.matsim.application.analysis.population` in the VSP contrib.
`VttsDashboard` is in `org.matsim.simwrapper.dashboard` in the SimWrapper contrib.
The module installs no scoring overrides: MUSE uses the run's injected `ScoringFunctionFactory`.
The calculation runs at the final iteration end, after core scoring, using in-memory experienced
plans. It does not require the regular experienced-plans XML output to be enabled.
The initial integration supports single-process runs; distributed collection is not implemented.

The default selection includes agents without a subpopulation and those in subpopulation `person`,
using the VSP constant `SubpopulationDefaultNames.SUBPOP_PERSON` from the common contrib.
Other named populations, including the commercial-traffic conventions in that class, are excluded
by default. The absent-attribute case supports scenarios that do not assign subpopulation names.
A scenario can supply its own predicate, for example:

```java
controler.addOverridingModule(new VttsModule(person ->
    "berlin_person".equals(PopulationUtils.getSubpopulation(person))));
```

Filtering never removes agents from the scenario population or recomputes the run's income
normalization. Stuck agents, missing or incomplete plans, and mismatches between planned and
experienced activity sequences receive coverage statuses and do not enter the distributions.
Missing income alone is not an exclusion criterion.

## Calculation and scoring support

MUSE uses the original KN incremental calculation: two scoring functions accumulate scores for
main activities at their original times and with destination starts one second earlier. For each
destination, the difference between their score increments is scaled to score/hour. The morning
activity is deferred until just before the evening activity, and both scorers are finished after
each destination. The calculation is linear in the number of main activities. The original person,
selected plan, and activity times are not replaced. Planned activity attributes missing from
experienced activities are copied by matching the main-activity sequences.

The scorer must support intermediate scores, repeated `finish()` calls followed by further
activities, and the deferred morning callback. Scorers that require chronological alignment with
selected-plan activities, a single final `finish()`, additional events, legs, or external simulation
state may be incompatible. These are existing KN assumptions; using the run's factory does not
remove them. Input validation and coverage handling belong to `VttsAnalysis`, outside MUSE itself.

For linear money and travel-time scoring:

```text
VSE  [money/hour] = MUSE / marginal_utility_of_money
VTTS [money/hour] = (MUSE - travel_utility_per_hour) / marginal_utility_of_money
```

VTTS assumes one second less traveling in the realized trip's main mode, as classified by the run's
injected `AnalysisMainModeIdentifier`. A requested routing mode is not substituted for the actual mode.
All other trip components
are held fixed. For PT, this describes in-vehicle time, not waiting or access/egress time.
Units are the run's money units; euros are not assumed. Finite zero and negative values are retained.
Undefined or nonpositive money utility prevents monetary conversion but retains valid MUSE.
Missing mode coefficients retain MUSE/VSE and mark VTTS as unavailable.

The default `VttsParameters` implementation reads the run's injected `ScoringParametersForPerson`.
If a custom scoring factory uses different time or money coefficients, install a matching
`VttsParameters` binding in a scenario overriding module after `VttsModule`, for example:

```java
controler.addOverridingModule(new AbstractModule() {
    @Override public void install() {
        bind(VttsParameters.class).to(MyScenarioVttsParameters.class);
    }
});
```

Such a conversion must have the stated linear interpretation; a nonlinear monetary utility
requires an explicitly defined local conversion rather than assuming a config coefficient.

## Outputs

All outputs are written under `analysis/vtts/` without sample expansion:

| File | Contents |
| --- | --- |
| `vtts_persons.csv.gz` | IDs, subpopulation, income, valid-destination counts, person mean MUSE/VSE/VTTS, money utility, status |
| `vtts_trips.csv.gz` | Zero-based trip index, destination type, saving mode, MUSE, direct travel coefficient, money utility, VSE/VTTS, status |
| `vtts_summary.csv` | Counts, mean, quartiles, median and p95, separately weighted by persons and destinations |
| `vtts_coverage.csv` | Number of persons in each coverage status |
| `vtts_overview.csv` | Tile data |
| `vtts_experienced_plans.xml.gz` | Copies of available experienced plans with activity/plan results and original person attributes |

Non-finite numbers are empty CSV cells. Summary statistics exclude them independently per metric.
Quantiles use linear interpolation between sorted observations. Person means weight valid
destinations equally; population destination summaries weight each valid destination equally.
Partially covered persons have a mean over their available values and are identified by status.

The dashboard only reads these files; it does not reconstruct a scorer or invoke a new scoring
command. Existing exports can therefore be displayed later with `new VttsDashboard()` or with
`new VttsDashboard("another/relative/directory")`. The analysis and CSV exporter are separate
classes, so already annotated results can be exported without recomputing MUSE.

## Preserved KN code and regression tests

The earlier comparison and related tools are preserved in the `population.kn` package. Older
event-based workflows and manual examples are deprecated. Legacy MUSE accessors delegate to
`VttsAttributes`, preserving the attribute key and the older activity-key fallback.
The original nine characterization tests remain; their package/imports changed where
needed by the move. Additional tests cover scorer lifecycle, monetary conversion, CSV/XML
round trips, weighting, dashboard YAML, and controller integration with a custom scoring factory.

From the repository root, using JDK 25:

```bash
mvn -pl contribs/vsp -am \
  -Dtest=AgentWiseComparisonKNTest,MuseComputationTest,VTTSHandlerPerActivityTypicalDurationTest,VttsAnalysisTest,VttsCsvWriterTest,VttsModuleTest,VttsDashboardTest \
  -Dsurefire.failIfNoSpecifiedTests=false -DskipITs verify
```
