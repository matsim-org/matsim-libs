package org.matsim.contrib.drt.extension.fiss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.DrtOperationsControlerCreator;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesParams;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class RunFissDrtScenarioIT {

	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test void test() {

		Config config = createDrtScenarioConfig();

		final Controler controler = DrtOperationsControlerCreator.createControler(config, false);
		configureFiss(controler, config);

		// for testing:
		LinkCounter linkCounter = new LinkCounter();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(linkCounter);
			}
		});

		controler.run();
		{
			String expected = utils.getInputDirectory() + "0.events.xml.gz";
			String actual = utils.getOutputDirectory() + "ITERS/it.0/0.events.xml.gz";
			ComparisonResult result = EventsUtils.compareEventsFiles(expected, actual);
			assertEquals(ComparisonResult.FILES_ARE_EQUAL, result);
		}
		{
			String expected = utils.getInputDirectory() + "output_events.xml.gz";
			String actual = utils.getOutputDirectory() + "output_events.xml.gz";
			ComparisonResult result = EventsUtils.compareEventsFiles(expected, actual);
			assertEquals(ComparisonResult.FILES_ARE_EQUAL, result);
		}
		Assertions.assertEquals(20000, linkCounter.getLinkLeaveCount(), 2000);// yy why a delta of 2000? kai, jan'25

	}

	/**
	 * Same as {@link #test()} but with
	 * {@link QSimConfigGroup.VehicleBehavior#wait}.
	 * Verifies that FISS correctly teleports vehicles alongside agents so that no
	 * agent gets stuck
	 * waiting for a vehicle that never arrives.
	 */
	@Test
	void testWithVehicleBehaviorWait() {

		Config config = createDrtScenarioConfig();
		config.qsim().setVehicleBehavior(QSimConfigGroup.VehicleBehavior.wait);

		final Controler controler = DrtOperationsControlerCreator.createControler(config, false);
		configureFiss(controler, config);

		StuckCounter stuckCounter = new StuckCounter();
		LinkCounter linkCounter = new LinkCounter();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(linkCounter);
				addEventHandlerBinding().toInstance(stuckCounter);
			}
		});

		controler.run();

		assertEquals(0, stuckCounter.getStuckCount(),
				"No agents should get stuck with VehicleBehavior.wait + FISS vehicle teleportation");
		Assertions.assertEquals(20000, linkCounter.getLinkLeaveCount(), 2000);
	}

	/**
	 * Minimal programmatic scenario: one agent, two car trips (home-->work-->home),
	 * sampleFactor near zero so the agent is always teleported. Verifies:
	 * <ul>
	 * <li>No stuck agents (vehicle arrives at destination and is available for the
	 * return trip)</li>
	 * <li>Agent completes both trips (2 PersonArrivalEvents)</li>
	 * <li>No car LinkLeaveEvents (all car traffic is teleported)</li>
	 * <li>Agent arrives at the correct time (travel time matches network
	 * free-speed, not instant)</li>
	 * <li>Vehicle arrives at the correct time (probed via
	 * MobsimAfterSimStepListener)</li>
	 * </ul>
	 *
	 * Network: 4 nodes, links 1000m at 10 m/s (100s per link).
	 * 
	 * <pre>
	 * n1 --l1--> n2 --l2--> n3 --l3--> n4
	 * n1 <--l4-- n2 <--l5-- n3 <--l6-- n4
	 * </pre>
	 * 
	 * Trip 1: depart l1 at t=100, route [l2], arrive l3. Travel time = 202s
	 * (l2 100s + l3 100s + 2 node transitions). Vehicle arrives at l3 at t=302.
	 * Activity at l3 ends at t=400 (vehicle already parked).
	 * Trip 2: depart l3 at t=400, route [l6, l5], arrive l4. Travel time = 303s
	 * (l6 100s + l5 100s + l4 100s + 3 node transitions).
	 */
	@Test
	void testVehicleTeleportationMinimalScenario() {
		runMinimalTeleportationScenario(400); // activity ends at t=400, after vehicle arrival at t=302
	}

	/**
	 * Same network and plan as {@link #testVehicleTeleportationMinimalScenario()},
	 * but the activity ends at t=150, BEFORE the vehicle arrives at t=302. With
	 * {@link QSimConfigGroup.VehicleBehavior#wait}, the agent calls
	 * {@code registerDriverAgentWaitingForCar} and waits. When the vehicle arrives
	 * at t=302 via FISS doSimStep, {@code makeVehicleAvailableToNextDriver} wakes
	 * the agent and trip 2 starts right after t=302.
	 */
	@Test
	void testVehicleTeleportationAgentWaitsForVehicle() {
		runMinimalTeleportationScenario(150); // activity ends at t=150, before vehicle arrival at t=302
	}

	/**
	 * Shared implementation for the minimal teleportation tests.
	 *
	 * @param activityEndTime when the intermediate activity ends. If before
	 *                        vehicle arrival (t=302), the agent waits for the
	 *                        vehicle.
	 */
	private void runMinimalTeleportationScenario(double activityEndTime) {

		Config config = ConfigUtils.createConfig();
		config.qsim().setVehicleBehavior(QSimConfigGroup.VehicleBehavior.wait);
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.qsim().setMainModes(List.of(TransportMode.car));
		config.qsim().setEndTime(24 * 3600);
		config.routing().setNetworkModes(List.of(TransportMode.car));
		config.scoring().addActivityParams(new ActivityParams("home").setTypicalDuration(8 * 3600));
		config.scoring().addActivityParams(new ActivityParams("work").setTypicalDuration(8 * 3600));
		config.scoring().addModeParams(new ModeParams(TransportMode.car));
		config.replanning().addStrategySettings(new StrategySettings().setStrategyName("ChangeExpBeta").setWeight(1));
		config.controller().setLastIteration(0);
		config.controller()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Scenario scenario = ScenarioUtils.createScenario(config);

		// Network: 4 nodes, all links 1000m at 10 m/s = 100s per link
		Network network = scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node1 = nf.createNode(Id.createNodeId("1"), new Coord(0, 0));
		Node node2 = nf.createNode(Id.createNodeId("2"), new Coord(1000, 0));
		Node node3 = nf.createNode(Id.createNodeId("3"), new Coord(2000, 0));
		Node node4 = nf.createNode(Id.createNodeId("4"), new Coord(3000, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		Link l1 = nf.createLink(Id.createLinkId("1"), node1, node2);
		Link l2 = nf.createLink(Id.createLinkId("2"), node2, node3);
		Link l3 = nf.createLink(Id.createLinkId("3"), node3, node4);
		Link l4 = nf.createLink(Id.createLinkId("4"), node2, node1);
		Link l5 = nf.createLink(Id.createLinkId("5"), node3, node2);
		Link l6 = nf.createLink(Id.createLinkId("6"), node4, node3);
		for (Link link : List.of(l1, l2, l3, l4, l5, l6)) {
			link.setLength(1000);
			link.setFreespeed(10);
			link.setCapacity(1000);
			link.setNumberOfLanes(1);
			link.setAllowedModes(Set.of(TransportMode.car));
			network.addLink(link);
		}

		// Vehicle type for car mode
		Vehicles vehicles = scenario.getVehicles();
		VehicleType carType = VehicleUtils.createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		carType.setNetworkMode(TransportMode.car);
		vehicles.addVehicleType(carType);

		PopulationFactory pf = scenario.getPopulation().getFactory();

		// Trip 1: depart l1 at t=100, arrive l3. TT = l2(100) + l3(100) + 2 nodes =
		// 202.
		// Trip 2: depart l3 at max(activityEndTime, vehicleArrivalTime), arrive l4.
		// TT = l6(100) + l5(100) + l4(100) + 3 nodes = 303.
		double departTrip1 = 100;
		double expectedTravelTime1 = 202; // l2 + l3(arrival) + 2 node transitions
		double vehicleArrivalTime = departTrip1 + expectedTravelTime1; // t=302
		double expectedDepartTrip2 = Math.max(activityEndTime, vehicleArrivalTime);
		double expectedTravelTime2 = 303; // l6 + l5 + l4(arrival) + 3 node transitions

		Person agent = pf.createPerson(Id.createPersonId("agent1"));
		Plan plan = pf.createPlan();

		Activity home1 = pf.createActivityFromLinkId("home", l1.getId());
		home1.setEndTime(departTrip1);
		plan.addActivity(home1);

		Leg leg1 = pf.createLeg(TransportMode.car);
		TripStructureUtils.setRoutingMode(leg1, TransportMode.car);
		NetworkRoute route1 = RouteUtils.createLinkNetworkRouteImpl(l1.getId(), List.of(l2.getId()), l3.getId());
		leg1.setRoute(route1);
		plan.addLeg(leg1);

		Activity work = pf.createActivityFromLinkId("work", l3.getId());
		work.setEndTime(activityEndTime);
		plan.addActivity(work);

		Leg leg2 = pf.createLeg(TransportMode.car);
		TripStructureUtils.setRoutingMode(leg2, TransportMode.car);
		NetworkRoute route2 = RouteUtils.createLinkNetworkRouteImpl(l3.getId(), List.of(l6.getId(), l5.getId()),
				l4.getId());
		leg2.setRoute(route2);
		plan.addLeg(leg2);

		Activity home2 = pf.createActivityFromLinkId("home", l4.getId());
		plan.addActivity(home2);
		agent.addPlan(plan);
		scenario.getPopulation().addPerson(agent);

		// FISS: sampleFactor near zero, so all car agents are (almost certainly)
		// teleported.
		// Using Double.MIN_VALUE instead of 0.0 because FISSConfigGroup requires
		// sampleFactor > 0.
		FISSConfigGroup fissConfigGroup = ConfigUtils.addOrGetModule(config, FISSConfigGroup.class);
		fissConfigGroup.setSampleFactor(Double.MIN_VALUE);
		fissConfigGroup.setSampledModes(Set.of(TransportMode.car));
		fissConfigGroup.setSwitchOffFISSLastIteration(false);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new FISSModule());

		StuckCounter stuckCounter = new StuckCounter();
		ArrivalTracker arrivalTracker = new ArrivalTracker();
		LinkCounter linkCounter = new LinkCounter();
		// The parking probe only works when the vehicle stays parked (activity ends
		// after vehicle arrival). When the agent is already waiting, the vehicle is
		// immediately taken in the same sim step and never appears as "parked".
		boolean vehicleStaysParked = activityEndTime >= vehicleArrivalTime;
		VehicleParkingProbe vehicleParkingProbe = vehicleStaysParked
				? new VehicleParkingProbe(Id.createVehicleId("agent1"), l3.getId())
				: null;
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(stuckCounter);
				addEventHandlerBinding().toInstance(arrivalTracker);
				addEventHandlerBinding().toInstance(linkCounter);
				if (vehicleParkingProbe != null) {
					addMobsimListenerBinding().toInstance(vehicleParkingProbe);
				}
			}
		});

		controler.run();

		Id<Person> agentId = Id.createPersonId("agent1");

		assertEquals(0, stuckCounter.getStuckCount(),
				"No agents should get stuck -- vehicle must be available for the return trip");
		assertEquals(0, linkCounter.getLinkLeaveCount(),
				"No car LinkLeaveEvents expected -- all car traffic is teleported");

		// Verify correct agent arrival times (not instant teleportation)
		List<Double> arrivalTimes = arrivalTracker.getArrivalTimes(agentId);
		assertEquals(2, arrivalTimes.size(), "Agent should complete both trips (2 arrivals)");
		assertEquals(departTrip1 + expectedTravelTime1, arrivalTimes.get(0), 0,
				"Trip 1 arrival time should reflect network travel time, not instant teleportation");
		assertEquals(expectedDepartTrip2 + expectedTravelTime2, arrivalTimes.get(1), 0,
				"Trip 2 arrival time should reflect departure + network travel time");

		if (vehicleParkingProbe != null) {
			// Verify vehicle arrives at the correct time on the destination link.
			// The probe checks at every time step when the vehicle first appears on l3.
			// It must appear at t=200 (departure + travel time), not at t=100 (instant).
			assertFalse(Double.isNaN(vehicleParkingProbe.getFirstSeenTime()),
					"Vehicle should have been detected on the destination link");
			assertEquals(vehicleArrivalTime, vehicleParkingProbe.getFirstSeenTime(), 0,
					"Vehicle should arrive at t=" + vehicleArrivalTime + " (not instantly teleported)");
		}
	}

	// ==================== Helper methods ====================

	private Config createDrtScenarioConfig() {
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new);

		String fleetFile = "holzkirchenFleet.xml";
		String plansFile = "holzkirchenPlans_car_drt.xml.gz";
		String networkFile = "holzkirchenNetwork.xml.gz";
		String opFacilitiesFile = "holzkirchenOperationFacilities.xml";
		String shiftsFile = "holzkirchenShifts.xml";

		DrtWithExtensionsConfigGroup drtWithShiftsConfigGroup = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.createParameterSet("drt");

		DrtConfigGroup drtConfigGroup = drtWithShiftsConfigGroup;
		drtConfigGroup.setMode(TransportMode.drt);
		drtConfigGroup.setStopDuration(30.);
		DrtOptimizationConstraintsSetImpl defaultConstraintsSet =
				drtConfigGroup.addOrGetDrtOptimizationConstraintsParams()
						.addOrGetDefaultDrtOptimizationConstraintsSet();
		defaultConstraintsSet.setMaxTravelTimeAlpha(1.5);
		defaultConstraintsSet.setMaxTravelTimeBeta(10. * 60.);
		defaultConstraintsSet.setMaxWaitTime(600.);
		defaultConstraintsSet.setRejectRequestIfMaxWaitOrTravelTimeViolated(true);
		defaultConstraintsSet.setMaxWalkDistance(1000.);
		drtConfigGroup.setUseModeFilteredSubnetwork(false);
		drtConfigGroup.setVehiclesFile(fleetFile);
		drtConfigGroup.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door);
		drtConfigGroup.setPlotDetailedCustomerStats(true);
		drtConfigGroup.setIdleVehiclesReturnToDepots(false);

		drtConfigGroup.addParameterSet(new ExtensiveInsertionSearchParams());

		ConfigGroup rebalancing = drtConfigGroup.createParameterSet("rebalancing");
		drtConfigGroup.addParameterSet(rebalancing);
		((RebalancingParams) rebalancing).setInterval(600);

		MinCostFlowRebalancingStrategyParams strategyParams = new MinCostFlowRebalancingStrategyParams();
		strategyParams.setTargetAlpha(0.3);
		strategyParams.setTargetBeta(0.3);

		RebalancingParams rebalancingParams = drtConfigGroup.getRebalancingParams().get();
		rebalancingParams.addParameterSet(strategyParams);

		SquareGridZoneSystemParams zoneSystemParams = (SquareGridZoneSystemParams) rebalancingParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME);
		zoneSystemParams.setCellSize(500.);
		rebalancingParams.addParameterSet(zoneSystemParams);
		rebalancingParams.setTargetLinkSelection(RebalancingParams.TargetLinkSelection.mostCentral);

		multiModeDrtConfigGroup.addParameterSet(drtWithShiftsConfigGroup);

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		Config config = ConfigUtils.createConfig(multiModeDrtConfigGroup, dvrpConfigGroup);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);
		config.setContext(ExamplesUtils.getTestScenarioURL("holzkirchen"));

		Set<String> modes = new HashSet<>();
		modes.add("drt");
		config.travelTimeCalculator().setAnalyzedModes(modes);

		config.scoring().addModeParams(new ModeParams("drt"));
		config.scoring().addModeParams(new ModeParams("walk"));

		config.plans().setInputFile(plansFile);
		config.network().setInputFile(networkFile);

		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.minOfEndtimeAndMobsimFinished);

		config.scoring().addActivityParams(new ActivityParams("home").setTypicalDuration(8 * 3600));
		config.scoring().addActivityParams(new ActivityParams("other").setTypicalDuration(4 * 3600));
		config.scoring().addActivityParams(new ActivityParams("education").setTypicalDuration(6 * 3600));
		config.scoring().addActivityParams(new ActivityParams("shopping").setTypicalDuration(2 * 3600));
		config.scoring().addActivityParams(new ActivityParams("work").setTypicalDuration(2 * 3600));

		config.replanning().addStrategySettings(new StrategySettings().setStrategyName("ChangeExpBeta").setWeight(1));

		config.controller().setLastIteration(1);
		config.controller().setWriteEventsInterval(1);

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.gzip);

		DrtOperationsParams operationsParams = (DrtOperationsParams) drtWithShiftsConfigGroup.createParameterSet(DrtOperationsParams.SET_NAME);
		ShiftsParams shiftsParams = (ShiftsParams) operationsParams.createParameterSet(ShiftsParams.SET_NAME);
		OperationFacilitiesParams operationFacilitiesParams = (OperationFacilitiesParams) operationsParams.createParameterSet(OperationFacilitiesParams.SET_NAME);
		operationsParams.addParameterSet(shiftsParams);
		operationsParams.addParameterSet(operationFacilitiesParams);

		operationFacilitiesParams.setOperationFacilityInputFile(opFacilitiesFile);
		shiftsParams.setShiftInputFile(shiftsFile);
		shiftsParams.setAllowInFieldChangeover(true);
		shiftsParams.setShiftEndRelocationArrival(ShiftsParams.ShiftEndRelocationArrival.immediate);
		drtWithShiftsConfigGroup.addParameterSet(operationsParams);

		if (!config.qsim().getVehiclesSource().equals(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData)) {
			config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		}

		return config;
	}

	private void configureFiss(Controler controler, Config config) {
		FISSConfigGroup fissConfigGroup = ConfigUtils.addOrGetModule(config, FISSConfigGroup.class);
		fissConfigGroup.setSampleFactor(0.1);
		fissConfigGroup.setSampledModes(Set.of(TransportMode.car));
		fissConfigGroup.setSwitchOffFISSLastIteration(true);

		Vehicles vehiclesContainer = controler.getScenario().getVehicles();
		for (String sampledMode : fissConfigGroup.getSampledModes()) {
			VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create(sampledMode, VehicleType.class));
			vehicleType.setNetworkMode(sampledMode);
			vehiclesContainer.addVehicleType(vehicleType);
		}

		controler.addOverridingModule(new FISSModule());
	}

	// ==================== Event handlers ====================

	static class LinkCounter implements LinkLeaveEventHandler {
		private int counts = 0;

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			this.counts++;
		}

		public int getLinkLeaveCount() {
			return this.counts;
		}
	}

	/**
	 * Verifies that FISS teleportation produces the same arrival times as QSim
	 * physical simulation. Runs the same scenario twice (baseline without FISS,
	 * then with FISS) and asserts exact arrival time equality.
	 *
	 * <p>Network uses different link lengths to distinguish potential mismatch
	 * sources (departure link, intermediate links, node transitions):
	 *
	 * <pre>
	 * n1 --l1(500m)--> n2 --l2(1000m)--> n3 --l3(1500m)--> n4
	 * n1 <--l4(500m)-- n2 <--l5(1000m)-- n3 <--l6(1500m)-- n4
	 * </pre>
	 *
	 * All links at 10 m/s. Trip 1: l1--[l2]-->l3 (depart link 50s + node 1s +
	 * l2 100s + node 1s = 152s). Trip 2: l3--[l6, l5]-->l4 (depart link 150s +
	 * node 1s + l6 150s + node 1s + l5 100s + node 1s = 403s).
	 */
	@Test
	void testFissTeleportationMatchesQSimTiming() {
		runAndCompareTimings(Double.POSITIVE_INFINITY);
	}

	/**
	 * Same as {@link #testFissTeleportationMatchesQSimTiming()} but with a
	 * vehicle whose max speed (5 m/s) is lower than the links' free speed
	 * (10 m/s). Verifies that FISS respects the vehicle speed limit.
	 */
	@Test
	void testFissTeleportationMatchesQSimTimingWithSlowVehicle() {
		runAndCompareTimings(5.0);
	}

	private void runAndCompareTimings(double vehicleMaxSpeed) {
		ArrivalTracker baselineArrivals = runTeleportationTimingScenario(false, vehicleMaxSpeed);
		ArrivalTracker fissArrivals = runTeleportationTimingScenario(true, vehicleMaxSpeed);

		Id<Person> agentId = Id.createPersonId("agent1");
		List<Double> baseline = baselineArrivals.getArrivalTimes(agentId);
		List<Double> fiss = fissArrivals.getArrivalTimes(agentId);

		assertEquals(2, baseline.size(), "Baseline: agent should complete 2 trips");
		assertEquals(2, fiss.size(), "FISS: agent should complete 2 trips");

		assertEquals(baseline.get(0), fiss.get(0), 0,
				"Trip 1 arrival time should match baseline exactly");
		assertEquals(baseline.get(1), fiss.get(1), 0,
				"Trip 2 arrival time should match baseline exactly");
	}

	private ArrivalTracker runTeleportationTimingScenario(boolean enableFiss,
			double vehicleMaxSpeed) {
		Config config = ConfigUtils.createConfig();
		config.qsim().setVehicleBehavior(QSimConfigGroup.VehicleBehavior.teleport);
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.qsim().setMainModes(List.of(TransportMode.car));
		config.qsim().setEndTime(24 * 3600);
		config.routing().setNetworkModes(List.of(TransportMode.car));
		config.scoring().addActivityParams(new ActivityParams("home").setTypicalDuration(8 * 3600));
		config.scoring().addActivityParams(new ActivityParams("work").setTypicalDuration(8 * 3600));
		config.scoring().addModeParams(new ModeParams(TransportMode.car));
		config.replanning().addStrategySettings(
				new StrategySettings().setStrategyName("ChangeExpBeta").setWeight(1));
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(
				utils.getOutputDirectory() + "/" + (enableFiss ? "fiss" : "baseline")
						+ "_vmax" + (int) vehicleMaxSpeed);

		Scenario scenario = ScenarioUtils.createScenario(config);

		// Network with different link lengths to expose mismatch sources.
		// Forward links: l1=500m, l2=1000m, l3=1500m. Reverse: l4=500m, l5=1000m, l6=1500m.
		// All at 10 m/s, so travel times: l1,l4=50s; l2,l5=100s; l3,l6=150s.
		Network network = scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node n1 = nf.createNode(Id.createNodeId("1"), new Coord(0, 0));
		Node n2 = nf.createNode(Id.createNodeId("2"), new Coord(500, 0));
		Node n3 = nf.createNode(Id.createNodeId("3"), new Coord(1500, 0));
		Node n4 = nf.createNode(Id.createNodeId("4"), new Coord(3000, 0));
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);

		Link l1 = createLink(nf, "1", n1, n2, 500);
		Link l2 = createLink(nf, "2", n2, n3, 1000);
		Link l3 = createLink(nf, "3", n3, n4, 1500);
		Link l4 = createLink(nf, "4", n2, n1, 500);
		Link l5 = createLink(nf, "5", n3, n2, 1000);
		Link l6 = createLink(nf, "6", n4, n3, 1500);
		for (Link link : List.of(l1, l2, l3, l4, l5, l6)) {
			network.addLink(link);
		}

		// Vehicle type
		Vehicles vehicles = scenario.getVehicles();
		VehicleType carType = VehicleUtils.createVehicleType(
				Id.create(TransportMode.car, VehicleType.class));
		carType.setNetworkMode(TransportMode.car);
		carType.setMaximumVelocity(vehicleMaxSpeed);
		vehicles.addVehicleType(carType);

		// One agent, two trips:
		// Trip 1: l1--[l2]-->l3 at t=100. Depart link 50s + node 1s + l2 100s + node 1s = 152s.
		// Trip 2: l3--[l6, l5]-->l4 at t=500. Depart link 150s + node 1s + l6 150s + node 1s + l5 100s + node 1s = 403s.
		PopulationFactory pf = scenario.getPopulation().getFactory();
		Person agent = pf.createPerson(Id.createPersonId("agent1"));
		Plan plan = pf.createPlan();

		Activity home1 = pf.createActivityFromLinkId("home", l1.getId());
		home1.setEndTime(100);
		plan.addActivity(home1);

		Leg leg1 = pf.createLeg(TransportMode.car);
		TripStructureUtils.setRoutingMode(leg1, TransportMode.car);
		NetworkRoute route1 = RouteUtils.createLinkNetworkRouteImpl(
				l1.getId(), List.of(l2.getId()), l3.getId());
		leg1.setRoute(route1);
		plan.addLeg(leg1);

		Activity work = pf.createActivityFromLinkId("work", l3.getId());
		// End time must be later than any possible trip-1 arrival so that the
		// agent performs a real activity. A "back-to-back" departure (arriving
		// and departing in the same QSim step) introduces a 1-second delay in
		// the baseline that FISS cannot reproduce because the delay stems from
		// QSim-internal node-processing order, not from link travel times.
		work.setEndTime(800);
		plan.addActivity(work);

		Leg leg2 = pf.createLeg(TransportMode.car);
		TripStructureUtils.setRoutingMode(leg2, TransportMode.car);
		NetworkRoute route2 = RouteUtils.createLinkNetworkRouteImpl(
				l3.getId(), List.of(l6.getId(), l5.getId()), l4.getId());
		leg2.setRoute(route2);
		plan.addLeg(leg2);

		Activity home2 = pf.createActivityFromLinkId("home", l4.getId());
		plan.addActivity(home2);

		agent.addPlan(plan);
		scenario.getPopulation().addPerson(agent);

		if (enableFiss) {
			FISSConfigGroup fissConfigGroup = ConfigUtils.addOrGetModule(
					config, FISSConfigGroup.class);
			fissConfigGroup.setSampleFactor(Double.MIN_VALUE);
			fissConfigGroup.setSampledModes(Set.of(TransportMode.car));
			fissConfigGroup.setSwitchOffFISSLastIteration(false);
		}

		Controler controler = new Controler(scenario);
		if (enableFiss) {
			controler.addOverridingModule(new FISSModule());
		}

		ArrivalTracker arrivalTracker = new ArrivalTracker();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(arrivalTracker);
			}
		});

		controler.run();

		return arrivalTracker;
	}

	private static Link createLink(NetworkFactory nf, String id, Node from, Node to,
			double lengthM) {
		Link link = nf.createLink(Id.createLinkId(id), from, to);
		link.setLength(lengthM);
		link.setFreespeed(10);
		link.setCapacity(1000);
		link.setNumberOfLanes(1);
		link.setAllowedModes(Set.of(TransportMode.car));
		return link;
	}

	static class ArrivalTracker implements PersonArrivalEventHandler {
		private final Map<Id<Person>, List<Double>> arrivalTimes = new HashMap<>();

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			arrivalTimes.computeIfAbsent(event.getPersonId(),
					k -> new java.util.ArrayList<>()).add(event.getTime());
		}

		@Override
		public void reset(int iteration) {
			arrivalTimes.clear();
		}

		public List<Double> getArrivalTimes(Id<Person> personId) {
			return arrivalTimes.getOrDefault(personId, Collections.emptyList());
		}
	}

	static class StuckCounter implements PersonStuckEventHandler {
		private int stuckCount = 0;

		@Override
		public void handleEvent(PersonStuckEvent event) {
			this.stuckCount++;
		}

		@Override
		public void reset(int iteration) {
			this.stuckCount = 0;
		}

		public int getStuckCount() {
			return this.stuckCount;
		}
	}

	static class VehicleEntryTracker implements PersonEntersVehicleEventHandler {
		private final Map<Id<Person>, List<Double>> entryTimes = new HashMap<>();

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			entryTimes.computeIfAbsent(event.getPersonId(), k -> new java.util.ArrayList<>())
					.add(event.getTime());
		}

		@Override
		public void reset(int iteration) {
			entryTimes.clear();
		}

		public List<Double> getEntryTimes(Id<Person> personId) {
			return entryTimes.getOrDefault(personId, Collections.emptyList());
		}

		public Double getFirstEntryTime(Id<Person> personId) {
			List<Double> times = entryTimes.get(personId);
			return times != null && !times.isEmpty() ? times.get(0) : null;
		}
	}

	/**
	 * Probes the QSim at every time step to detect when a vehicle first appears
	 * (parked) on a target link. Used to verify vehicle teleportation timing.
	 */
	static class VehicleParkingProbe implements MobsimInitializedListener, MobsimAfterSimStepListener {
		private final Id<Vehicle> vehicleId;
		private final Id<Link> targetLinkId;
		private QLinkI targetLink;
		private double firstSeenTime = Double.NaN;

		VehicleParkingProbe(Id<Vehicle> vehicleId, Id<Link> targetLinkId) {
			this.vehicleId = vehicleId;
			this.targetLinkId = targetLinkId;
		}

		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			QSim qSim = (QSim) e.getQueueSimulation();
			this.targetLink = (QLinkI) qSim.getNetsimNetwork().getNetsimLink(targetLinkId);
		}

		@Override
		public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
			if (Double.isNaN(firstSeenTime) && targetLink.getParkedVehicle(vehicleId) != null) {
				firstSeenTime = e.getSimulationTime();
			}
		}

		public double getFirstSeenTime() {
			return firstSeenTime;
		}
	}

}
