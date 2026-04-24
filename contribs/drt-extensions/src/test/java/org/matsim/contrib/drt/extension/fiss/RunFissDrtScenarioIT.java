package org.matsim.contrib.drt.extension.fiss;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
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
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import static org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import static org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;

public class RunFissDrtScenarioIT {

	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test void test() {

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
		final Config config = ConfigUtils.createConfig(multiModeDrtConfigGroup, dvrpConfigGroup);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);
		config.setContext(ExamplesUtils.getTestScenarioURL("holzkirchen"));

		Set<String> modes = new HashSet<>();
		modes.add("drt");
		config.travelTimeCalculator().setAnalyzedModes(modes);

		config.scoring().addModeParams( new ModeParams("drt") );
		config.scoring().addModeParams( new ModeParams("walk") );

		config.plans().setInputFile(plansFile);
		config.network().setInputFile(networkFile);

		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.minOfEndtimeAndMobsimFinished);

		config.scoring().addActivityParams( new ActivityParams("home").setTypicalDuration(8 * 3600 ) );
		config.scoring().addActivityParams( new ActivityParams("other").setTypicalDuration(4 * 3600 ) );
		config.scoring().addActivityParams( new ActivityParams("education").setTypicalDuration(6 * 3600 ) );
		config.scoring().addActivityParams( new ActivityParams("shopping").setTypicalDuration(2 * 3600 ) );
		config.scoring().addActivityParams( new ActivityParams("work").setTypicalDuration(2 * 3600 ) );

		config.replanning().addStrategySettings( new StrategySettings().setStrategyName("ChangeExpBeta" ).setWeight(1 ) );

		config.controller().setLastIteration(1);
		config.controller().setWriteEventsInterval(1);

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory( utils.getOutputDirectory() );
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

		// ### controler:

		final Controler controler = DrtOperationsControlerCreator.createControler(config, false);

		//FISS part
		{
			// FISS config:
			FISSConfigGroup fissConfigGroup = ConfigUtils.addOrGetModule(config, FISSConfigGroup.class);
			fissConfigGroup.setSampleFactor(0.1);
			fissConfigGroup.setSampledModes(Set.of(TransportMode.car));
			fissConfigGroup.setSwitchOffFISSLastIteration(true);

			// provide mode vehicle types (in production code, one should set them more diligently):
			Vehicles vehiclesContainer = controler.getScenario().getVehicles();
			for( String sampledMode : fissConfigGroup.getSampledModes()){
				VehicleType vehicleType = VehicleUtils.createVehicleType( Id.create( sampledMode, VehicleType.class ) );
				vehicleType.setNetworkMode(sampledMode);
				vehiclesContainer.addVehicleType(vehicleType);
			}

			// add FISS module:
			controler.addOverridingModule( new FISSModule() );

		}

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
			String expected = utils.getInputDirectory() + "0.events.xml.gz" ;
			String actual = utils.getOutputDirectory() + "ITERS/it.0/0.events.xml.gz" ;
			ComparisonResult result = EventsUtils.compareEventsFiles( expected, actual );
			assertEquals( ComparisonResult.FILES_ARE_EQUAL, result );
		}
		{
			String expected = utils.getInputDirectory() + "output_events.xml.gz" ;
			String actual = utils.getOutputDirectory() + "output_events.xml.gz" ;
			ComparisonResult result = EventsUtils.compareEventsFiles( expected, actual );
			assertEquals( ComparisonResult.FILES_ARE_EQUAL, result );
		}
		Assertions.assertEquals(20000, linkCounter.getLinkLeaveCount(), 2000);// yy why a delta of 2000?  kai, jan'25


	}

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

}
