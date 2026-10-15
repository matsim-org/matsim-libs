package org.matsim.contrib.drt.extension.operations.remoteoperations;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.DrtOperationsControlerCreator;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.remoteoperations.config.IncidentParams;
import org.matsim.contrib.drt.extension.operations.remoteoperations.config.IncidentSeverityParams;
import org.matsim.contrib.drt.extension.operations.remoteoperations.config.RejectionActivationParams;
import org.matsim.contrib.drt.extension.operations.remoteoperations.config.RemoteGuidanceParams;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentAssignedToOperatorEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentResolvedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentStartedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleActivatedForRemoteGuidanceEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleDeactivatedForRemoteGuidanceEvent;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesParams;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.examples.ExamplesUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for remote guidance: four operators (a morning pair and an afternoon/evening pair, staggered so at
 * most three overlap at once) with capacity 5 each supervise the 20-vehicle fleet across the operating day. Asserts that
 * vehicles get activated under operators and that the concurrent number of supervised vehicles never exceeds the
 * combined capacity of the simultaneously on-duty operators.
 *
 * @author nkuehnel / MOIA
 */
public class RunRemoteGuidanceDrtScenarioIT {

	private static final int OPERATOR_CAPACITY = 5;
	// peak number of operator shifts that overlap at once (see holzkirchenRemoteGuidanceShifts.xml: a morning pair and
	// an afternoon/evening pair, staggered so at the hand-over edges up to three operators are on duty simultaneously).
	// The concurrency invariants below are bounded by the SIMULTANEOUS operators, not the total shift count.
	private static final int MAX_CONCURRENT_OPERATORS = 3;

	@Test
	void test() {
		Trackers trackers = runScenario(1e-4, "test/output/RunRemoteGuidanceDrtScenarioIT");

		// at least some vehicles must have been activated under operators
		assertThat(trackers.tracker.totalAssignments).isPositive();
		// concurrent supervised vehicles must never exceed the combined capacity of the simultaneously on-duty operators
		assertThat(trackers.tracker.maxConcurrent).isLessThanOrEqualTo(MAX_CONCURRENT_OPERATORS * OPERATOR_CAPACITY);

		// incidents must be generated, assigned and resolved
		assertThat(trackers.incidentTracker.started).isPositive();
		assertThat(trackers.incidentTracker.resolved).isPositive();
		// lifecycle ordering: assigned ⊆ started, resolved ⊆ assigned (a few may still be queued/in-service at run end)
		assertThat(trackers.incidentTracker.assigned).isLessThanOrEqualTo(trackers.incidentTracker.started);
		assertThat(trackers.incidentTracker.resolved).isLessThanOrEqualTo(trackers.incidentTracker.assigned);
		// at most MAX_CONCURRENT_OPERATORS incidents in service at once (one incident occupies exactly one operator)
		assertThat(trackers.incidentTracker.maxConcurrentInService).isLessThanOrEqualTo(MAX_CONCURRENT_OPERATORS);
	}

	/**
	 * Regression guard for an insertion crash at high incident rates: an
	 * {@link org.matsim.contrib.drt.extension.operations.remoteoperations.schedule.IncidentHoldTask} spliced mid-drive became a
	 * regular insertion waypoint, and {@code DefaultRequestInsertionScheduler.insertDropoff}/{@code removeBetween} threw
	 * "Invalid schedule structure: expected WAIT or DRIVE task". Dormant at the low λ of {@link #test()}; a 10× rate makes
	 * incidents frequent enough that a hold reliably coincides with an insertion pass. With
	 * {@link org.matsim.contrib.drt.extension.operations.remoteoperations.optimizer.IncidentAwareVehicleDataEntryFactory} in place
	 * the run must simply complete without throwing.
	 */
	@Test
	void highLambdaDoesNotCrashInsertion() {
		// completing the mobsim without an insertion-scheduler exception IS the assertion; the trackers only confirm the
		// high rate actually produced (and worked through) plenty of incidents.
		Trackers trackers = runScenario(1e-3, "test/output/RunRemoteGuidanceDrtScenarioIT_highLambda");

		assertThat(trackers.incidentTracker.started).isPositive();
		assertThat(trackers.incidentTracker.resolved).isPositive();
	}

	private record Trackers(ConcurrencyTracker tracker, IncidentTracker incidentTracker) {
	}

	private Trackers runScenario(double lambdaPerMeter, String outputDirectory) {
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new);

		String fleetFile = "holzkirchenFleet.xml";
		String plansFile = "holzkirchenPlans.xml.gz";
		String networkFile = "holzkirchenNetwork.xml.gz";
		String opFacilitiesFile = "holzkirchenOperationFacilities.xml";
		String shiftsFile = "holzkirchenRemoteGuidanceShifts.xml";

		DrtWithExtensionsConfigGroup drtCfg = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.createParameterSet("drt");

		drtCfg.setMode(TransportMode.drt);
		DrtOptimizationConstraintsSetImpl defaultConstraintsSet =
				drtCfg.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();
		drtCfg.setStopDuration(30.);
		defaultConstraintsSet.setMaxTravelTimeAlpha(1.5);
		defaultConstraintsSet.setMaxTravelTimeBeta(10. * 60.);
		defaultConstraintsSet.setMaxWaitTime(600.);
		defaultConstraintsSet.setRejectRequestIfMaxWaitOrTravelTimeViolated(true);
		defaultConstraintsSet.setMaxWalkDistance(1000.);
		drtCfg.setUseModeFilteredSubnetwork(false);
		drtCfg.setVehiclesFile(fleetFile);
		drtCfg.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door);
		drtCfg.setPlotDetailedCustomerStats(true);
		drtCfg.setIdleVehiclesReturnToDepots(false);

		drtCfg.addParameterSet(new ExtensiveInsertionSearchParams());

		ConfigGroup rebalancing = drtCfg.createParameterSet("rebalancing");
		drtCfg.addParameterSet(rebalancing);
		((RebalancingParams) rebalancing).setInterval(600);

		MinCostFlowRebalancingStrategyParams strategyParams = new MinCostFlowRebalancingStrategyParams();
		strategyParams.setTargetAlpha(0.3);
		strategyParams.setTargetBeta(0.3);

		RebalancingParams rebalancingParams = drtCfg.getRebalancingParams().get();
		rebalancingParams.addParameterSet(strategyParams);

		SquareGridZoneSystemParams zoneParams = (SquareGridZoneSystemParams) rebalancingParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME);
		zoneParams.setCellSize(500.);
		rebalancingParams.addParameterSet(zoneParams);
		drtCfg.addParameterSet(zoneParams);
		rebalancingParams.setTargetLinkSelection(RebalancingParams.TargetLinkSelection.mostCentral);

		multiModeDrtConfigGroup.addParameterSet(drtCfg);

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		final Config config = ConfigUtils.createConfig(multiModeDrtConfigGroup, dvrpConfigGroup);
		config.setContext(ExamplesUtils.getTestScenarioURL("holzkirchen"));

		Set<String> modes = new HashSet<>();
		modes.add("drt");
		config.travelTimeCalculator().setAnalyzedModes(modes);

		config.scoring().addModeParams(new ScoringConfigGroup.ModeParams("drt"));
		config.scoring().addModeParams(new ScoringConfigGroup.ModeParams("walk"));

		config.plans().setInputFile(plansFile);
		config.network().setInputFile(networkFile);

		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.minOfEndtimeAndMobsimFinished);

		final ScoringConfigGroup.ActivityParams home = new ScoringConfigGroup.ActivityParams("home");
		home.setTypicalDuration(8 * 3600);
		final ScoringConfigGroup.ActivityParams other = new ScoringConfigGroup.ActivityParams("other");
		other.setTypicalDuration(4 * 3600);
		final ScoringConfigGroup.ActivityParams education = new ScoringConfigGroup.ActivityParams("education");
		education.setTypicalDuration(6 * 3600);
		final ScoringConfigGroup.ActivityParams shopping = new ScoringConfigGroup.ActivityParams("shopping");
		shopping.setTypicalDuration(2 * 3600);
		final ScoringConfigGroup.ActivityParams work = new ScoringConfigGroup.ActivityParams("work");
		work.setTypicalDuration(2 * 3600);

		config.scoring().addActivityParams(home);
		config.scoring().addActivityParams(other);
		config.scoring().addActivityParams(education);
		config.scoring().addActivityParams(shopping);
		config.scoring().addActivityParams(work);

		final ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
		stratSets.setWeight(1);
		stratSets.setStrategyName("ChangeExpBeta");
		config.replanning().addStrategySettings(stratSets);

		config.controller().setLastIteration(0);
		config.controller().setWriteEventsInterval(1);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(outputDirectory);

		DrtOperationsParams operationsParams = (DrtOperationsParams) drtCfg.createParameterSet(DrtOperationsParams.SET_NAME);
		ShiftsParams shiftsParams = (ShiftsParams) operationsParams.createParameterSet(ShiftsParams.SET_NAME);
		OperationFacilitiesParams operationFacilitiesParams = (OperationFacilitiesParams) operationsParams.createParameterSet(OperationFacilitiesParams.SET_NAME);
		RemoteGuidanceParams remoteGuidanceParams = (RemoteGuidanceParams) operationsParams.createParameterSet(RemoteGuidanceParams.SET_NAME);
		operationsParams.addParameterSet(shiftsParams);
		operationsParams.addParameterSet(operationFacilitiesParams);
		operationsParams.addParameterSet(remoteGuidanceParams);

		operationFacilitiesParams.setOperationFacilityInputFile(opFacilitiesFile);
		shiftsParams.setShiftInputFile(shiftsFile);
		shiftsParams.setAllowInFieldChangeover(true);

		remoteGuidanceParams.setOperatorShiftType("remoteGuidance");
		remoteGuidanceParams.setDefaultOperatorCapacity(OPERATOR_CAPACITY);
		remoteGuidanceParams.setMinRemainingShiftTimeForActivation(0);

		// stochastic incidents: one severity class, ~5 min median handling time. The per-metre hazard is parameterised so
		// the same scenario can be driven at the default low rate and at a much higher one (high-λ crash regression).
		IncidentParams incidentParams = (IncidentParams) remoteGuidanceParams.createParameterSet(IncidentParams.SET_NAME);
		IncidentSeverityParams severityParams = (IncidentSeverityParams) incidentParams.createParameterSet(IncidentSeverityParams.SET_NAME);
		severityParams.setSeverityName("default");
		severityParams.setLambdaPerMeter(lambdaPerMeter);
		severityParams.setDurationMu(Math.log(300));
		severityParams.setDurationSigma(0.5);
		incidentParams.addParameterSet(severityParams);
		remoteGuidanceParams.addParameterSet(incidentParams);

		// demand-driven activation (opt-in): exercises the RejectionRateActivation trigger + the shared RejectionRateTracker
		// wiring across both margins. A low threshold so any rejection pressure in the small scenario engages the trigger.
		RejectionActivationParams rejectionActivationParams = (RejectionActivationParams) remoteGuidanceParams
				.createParameterSet(RejectionActivationParams.SET_NAME);
		rejectionActivationParams.setWindowSize(900);
		rejectionActivationParams.setRejectionRateThreshold(0.05);
		remoteGuidanceParams.addParameterSet(rejectionActivationParams);

		drtCfg.addParameterSet(operationsParams);

		DrtFareParams drtFareParams = new DrtFareParams();
		drtFareParams.setBaseFare(1.);
		drtFareParams.setDistanceFare_m(1. / 1000);
		drtCfg.addParameterSet(drtFareParams);

		final Controler controler = DrtOperationsControlerCreator.createControler(config, false);

		ConcurrencyTracker tracker = new ConcurrencyTracker();
		IncidentTracker incidentTracker = new IncidentTracker();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(tracker);
				addEventHandlerBinding().toInstance(incidentTracker);
			}
		});

		controler.run();

		return new Trackers(tracker, incidentTracker);
	}

	/**
	 * Tracks global concurrency of the supervised fleet from the activation/deactivation events.
	 */
	private static final class ConcurrencyTracker implements BasicEventHandler {
		private final Set<Id<DvrpVehicle>> supervised = new HashSet<>();
		private int maxConcurrent = 0;
		private int totalAssignments = 0;

		@Override
		public void handleEvent(org.matsim.api.core.v01.events.Event event) {
			if (event instanceof VehicleActivatedForRemoteGuidanceEvent activated) {
				supervised.add(activated.getVehicleId());
				totalAssignments++;
				maxConcurrent = Math.max(maxConcurrent, supervised.size());
			} else if (event instanceof VehicleDeactivatedForRemoteGuidanceEvent deactivated) {
				supervised.remove(deactivated.getVehicleId());
			}
		}

		@Override
		public void reset(int iteration) {
			supervised.clear();
			maxConcurrent = 0;
			totalAssignments = 0;
		}
	}

	/**
	 * Tracks the incident lifecycle (started → assigned → resolved) and the peak number of incidents concurrently in
	 * service (assigned but not yet resolved), which must never exceed the operator server pool.
	 */
	private static final class IncidentTracker implements BasicEventHandler {
		private int started = 0;
		private int assigned = 0;
		private int resolved = 0;
		private int inService = 0;
		private int maxConcurrentInService = 0;

		@Override
		public void handleEvent(org.matsim.api.core.v01.events.Event event) {
			if (event instanceof IncidentStartedEvent) {
				started++;
			} else if (event instanceof IncidentAssignedToOperatorEvent) {
				assigned++;
				inService++;
				maxConcurrentInService = Math.max(maxConcurrentInService, inService);
			} else if (event instanceof IncidentResolvedEvent) {
				resolved++;
				inService--;
			}
		}

		@Override
		public void reset(int iteration) {
			started = 0;
			assigned = 0;
			resolved = 0;
			inService = 0;
			maxConcurrentInService = 0;
		}
	}
}