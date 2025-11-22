# ATAP - Agent-based Traffic Assignment Problem

The `se.vti.atap` package implements a solution heuristic for the agent-based traffic assignment problem. It contains a MATSim contrib (subpackage `matsim`) and a stand-alone implementation (subpackage `minimalframework`). Both are described further below. A detailed description of the method can be found in the following working paper (until publication only available from the author, email below): *G. Flötteröd (2025). A simulation heuristic for traveler- and vehicle-discrete dynamic traffic assignment. Linköping University and Swedish National Road and Transport Research Institute.*

If you need help getting this to run, please let me know: gunnar.flotterod@{vti,liu}.se

---

## Exploring ATAP functionality without MATSim

The `se.vti.atap.minimalframework` package is meant for lightweight standalone experimentation. At the package's top level, there are only interfaces and a single [Runner.java](./../src/main/java/se/vti/atap/minimalframework/Runner.java) class. The interfaces correspond to the terminology introduced in Flötteröd (2025). [Runner.java](./../src/main/java/se/vti/atap/minimalframework/Runner.java) combines these interfaces in an ATAP assignment logic. This functions as a blueprint; to evaluate a concrete model, the corresponding interfaces need to be instantiated.

The `defaults.replannerselection` subpackage contains default implementations of all assignment methods explored in Flötteröd (2025). The class [ProposedMethodWithLocalSearchPlanSelection.java](./../src/main/java/se/vti/atap/minimalframework/defaults/replannerselection/proposed/ProposedMethodWithLocalSearchPlanSelection.java) in the subpackage `defaults.replannerselection.proposed` implements the "proposed method" of that article.

The package `se.vti.atap.minimalframework.examples.parallel_links` contains code for testing on parallel link networks. the subpackage `random_network` implents the test network usind in Section 4 of Flötteröd (2025). The [ExampleRunner.java](./../src/main/java/se/vti/atap/minimalframework/examples/parallel_links/random_network/ExampleRunner.java) class contains:
- a small agent-based example (`runSmallTripMakerExample()`),
- a small OD flow-based example (`runSmallODExample()`),
- and the original example from the article (`runArticleExample()`).
Each function creates a (indvidually named) `.tsv` file with the same convergence statistics as used in the article.

---

## Using the ATAP MATSim extension

The ATAP extension replaces MATSim's standard solver (a coevolutionary algorithm). Experience so far indicates that solutions computed with ATAP exhibit very little variability (high reproducibility) and reach much lower equilibrium gaps than the standard solver. The MATSim extension is located in the `se.vti.atap.matsim` package. 

A **small example** is available in `se.vti.atap.matsim.examples.parallel_links`. This is a MATSim version of the example in `se.vti.atap.minimalframework`. [ScenarioCreator.java](./../src/main/java/se/vti/atap/matsim/examples/parallel_links/ScenarioCreator.java) builds a network of parallel links and a corresponding population. The number of parallel links is configurable, so are their outflow capacities. The population is built such that travel occurs from upstream origin links to downstream destination links that are connected to individually configurable parallel network links. The links connecting origins and destinations to the parallel links network are automatically configured such that all origins reach the parallel links at the same time. If the demand/network configuration is such there is a chance that congestion spills back into upstream diverges, an exception is thrown and recommendations for redimensioning the system are given. This attempts to approximate point queue traffic dynamics, for simplicity only. 
[ExampleRunner.java](./../src/main/java/se/vti/atap/matsim/examples/parallel_links/ExampleRunner.java) instantiates minimalistic examples (with two parallel routes only):
- for the uniform method (`runSmallExampleWithUniform()`),
- for the sorting method (`runSmallExampleWithSorting()`),
- and the proposed method (`runSmalExampleWithProposed()`).

These examples are only for illustration. Running an example creates an output folder `.small-example/[method name]`, which contains MATSim xml files of the used network, population, and configuration. The `output` subfolder contains the usual matsim logs, plus a file `ATAP.log` with assignment specific statistics. 

Two **large examples** are available in `se.vti.atap.matsim.examples`.
- [RunOsloExample.java](./../src/main/java/se/vti/atap/matsim/examples/RunOsloExample.java) demonstrates how ATAP assigns routes, departure times, and modes.
- [RunBerlinExample.java](./../src/main/java/se/vti/atap/matsim/examples/RunBerlinExample.java) assigns routes only.

Both examples are based on the same scenario files as used in Flötteröd (2025).

---

## ATAP Configuration

Add an ATAP module to your MATSim config file (and include it with `ConfigUtils.loadConfig(configFileName, new ATAPConfigGroup());`). Example, using default values:

```xml
<module name="atap">
    <!-- ASSIGNMENT METHOD. OPTIONS: DO_NOTHING, UNIFORM, SORTING, ATAP_APPROXIMATE_DISTANCE, ATAP_EXACT_DISTANCE -->
    <param name="replannerIdentifier" value="ATAP_APPROXIMATE_DISTANCE" />

    <!-- STEPSIZE = initialStepSizeFactor * (1.0 + iteration)^replanningRateIterationExponent -->
    <param name="initialStepSizeFactor" value="1.0" />
    <param name="replanningRateIterationExponent" value="-0.5" />

    <!-- NUMBER OF ITERATIONS USED TO FILTER OUT DNL NOISE -->
    <param name="maxMemory" value="1" />

	<!-- REDUCED LOGGING ONLY MONITORS STEP SIZES AND EQUILIBRIUM GAPS -->
	param name="reduceLogging" value="false" />

    <!-- NETWORK FLOW SMOOTHING. DEFAULTS WORK FOR "STANDARD MATSIM" -->
    <param name="kernelHalftime_s" value="300.0" />
    <param name="kernelThreshold" value="0.01" />

    <!-- SPECIFY COMPUTATIONAL CHEAP AND HEAVY MATSIM STRATEGIES. FOR INTERNAL PERFORMANCE TUNING. -->
    <param name="cheapStrategies" value="TimeAllocationMutator" />
    <param name="expensiveStrategies" value="ReRoute,TimeAllocationMutator_ReRoute,ChangeSingleTripMode,SubtourModeChoice,ChangeTripMode,ChangeLegMode,ChangeSingleLegMode,TripSubtourModeChoice" />

    <!-- DISTANCE TRANSFORMATIONS. OTHER THAN DEFAULTS MAY NOT WORK. -->
    <param name="useLinearDistance" value="true" />
    <param name="useQuadraticDistance" value="true" />
    <param name="useExponentialDistance" value="false" />
    <param name="useLogarithmicDistance" value="false" />
    <param name="normalizeDistance" value="false" />

    <!-- TESTING AND/OR DEBUGGING ONLY. BETTER NOT CHANGE. -->
    <param name="checkDistance" value="false" />
    <param name="shuffleBeforeReplannerSelection" value="true" />
    <param name="useFilteredTravelTimesForEmulation" value="false" />
    <param name="useFilteredTravelTimesForReplanning" value="false" />
    <param name="linkShareInDistance" value="1.0" />
    <param name="checkEmulatedAgentsCnt" value="0" />
</module>
```

---

## ATAP Emulation

ATAP needs to anticipate the scores of not-yet-executed plans. This functionality is provided by the `emulation` module of this repository. The `se.vti.emulation` package moves agents according to exogenously specified travel times through the system and generates an event stream as if the agents were moved by a mobsim. Emulation is preconfigured for cars (as a congested network mode) and for teleported modes. Other transport modes require corresponding emulation functionality. For instance, the emulation of "pt submodes" can be configured as follows:

```java
ATAP atap = new ATAP();

atap.setEmulator(TransportMode.pt, ScheduleBasedTransitLegEmulator.class);
atap.setEmulator("busPassenger", ScheduleBasedTransitLegEmulator.class);
atap.setEmulator("subwayPassenger", ScheduleBasedTransitLegEmulator.class);
atap.setEmulator("railPassenger", ScheduleBasedTransitLegEmulator.class);

Config config = ConfigUtils.loadConfig(configFileName);        
atap.configure(config);

Scenario scenario = ScenarioUtils.loadScenario(config);        
Controler controler = new Controler(scenario);
atap.configure(controler);

controler.run();
```

There is also (likely outdated) functionality for emulating road pricing.
