package org.matsim.contrib.ev.discharging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup.DriveEnergyConsumption;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.EvUtils;
import org.matsim.contrib.ev.example.RunEvExample;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author sebhoerl
 */
public class DriveDischargingHandlerTest {
	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testEnterAndLeaveNetworkInSameSecond() {
		// Regression: consecutive same-second trips corrupt the EvDrive map. The second
		// VehicleEntersTrafficEvent (t=1) creates a fresh EvDrive, overwriting the entry before
		// the first VehicleLeavesTrafficEvent (also t=1) can remove it — leaving the second
		// VehicleLeavesTrafficEvent (t=2) without a valid EvDrive, causing an NPE.

		int retries = 1000; // switch to a high number (1000) to stress-test the race condition

		for (int k = 0; k < retries; k++) {
			Config config = createBaseConfig(utils.getOutputDirectory(), 5.0);
			config.scoring().addActivityParams(new ActivityParams("generic").setScoringThisActivityAtAll(false));

			Scenario scenario = ScenarioUtils.createScenario(config);
			createTwoLinkNetwork(scenario);
			Vehicle vehicle = addElectricVehicle(scenario);

			Population population = scenario.getPopulation();
			PopulationFactory populationFactory = population.getFactory();

			Person person = populationFactory.createPerson(Id.createPersonId("person"));
			population.addPerson(person);

			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);

			PopulationUtils.insertVehicleIdsIntoPersonAttributes(person,
				Collections.singletonMap("car", vehicle.getId()));

			// Two l1→l1 legs (same start/end link = zero travel time = enter and leave in same second).
			Activity firstActivity = populationFactory.createActivityFromLinkId("generic", Id.createLinkId("l1"));
			firstActivity.setEndTime(0.0);
			plan.addActivity(firstActivity);

			Leg firstLeg = populationFactory.createLeg("car");
			firstLeg.setRoute(RouteUtils.createLinkNetworkRouteImpl(
				Id.createLinkId("l1"), Collections.emptyList(), Id.createLinkId("l1")));
			plan.addLeg(firstLeg);

			Activity secondActivity = populationFactory.createActivityFromLinkId("generic", Id.createLinkId("l1"));
			secondActivity.setEndTime(0.0);
			plan.addActivity(secondActivity);

			Leg secondLeg = populationFactory.createLeg("car");
			secondLeg.setRoute(RouteUtils.createLinkNetworkRouteImpl(
				Id.createLinkId("l1"), Collections.emptyList(), Id.createLinkId("l1")));
			plan.addLeg(secondLeg);

			Activity thirdActivity = populationFactory.createActivityFromLinkId("generic", Id.createLinkId("l1"));
			plan.addActivity(thirdActivity);

			createController(scenario).run();

			// Reaching here means no exception was thrown — the regression is covered.

			/*
			 * What happens in this simulation:
			 * - agent starts at 0.0 (VehicleEntersNetwork)
			 * - agent arrives at 0.0 (VehicleLeavesNetwork)
			 * - agent starts at 1.0 (VehicleEntersNetwork)
			 * - agent arrives at 1.0 (VehicleLeavesNetwork)
			 *
			 * This leads to a NPE in the current version of DriveDischargingHandler:
			 *
			 * In second 0.0:
			 *
			 * - VehicleEntersNetwork is processed at 0.0 -> this creates an energy tracking
			 * object
			 *
			 * - VehicleLeavesNetwork is tracked at 0.0 to be processed later
			 *
			 * In second 1.0:
			 *
			 * - VehicleEntersNetwork is processed at 1.0 -> this creates an energy tracking
			 * object (but there is already one, it is overwritten - FIRST ISSUE)
			 *
			 * - VehicleLeavesNetwork from second 0.0 is processed -> this first works with
			 * the energy tracking object, then removes it
			 *
			 * NOTE: The order here is important. The event handler that processes the
			 * VehicleEntersNetwork event runs in parallel with the onSimStep. If the order
			 * is inversed, everything will work without a problem.
			 *
			 * In second 2.0:
			 *
			 * - VehicleLeavesNetwork from second 1.0 is processed -> this tries to access
			 * the energy tracking object, but it has already been deleted! This gives an
			 * NPE.
			 *
			 */
		}
	}

	@Test
	public void ohdeSlaskiTest() {
		String[] args = {
			RunEvExample.DEFAULT_CONFIG_FILE,
			"--config:controler.outputDirectory", utils.getOutputDirectory()
		};

		SummedEnergy energy = new SummedEnergy();

		new RunEvExample().run(args, _ -> {}, _ -> {}, energy::install);

		assertEquals(926.23973, energy.energy_kWh, 1e-3);
	}

	@Test
	public void attributeBasedTest() {
		String[] args = {
			RunEvExample.DEFAULT_CONFIG_FILE,
			"--config:controler.outputDirectory", utils.getOutputDirectory()
		};

		SummedEnergy energy = new SummedEnergy();

		new RunEvExample().run(args, config -> EvConfigGroup.get(config).setDriveEnergyConsumption(DriveEnergyConsumption.AttributeBased), scenario -> {
			for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
				AttributeBasedDriveEnergyConsumption.assign(vehicle, 120.0);
			}
		}, energy::install);

		assertEquals(628.464897, energy.energy_kWh, 1e-3);
	}

	/**
	 * Tests that DriveDischargingHandler processes events from the last simulation timestep.
	 * <p>
	 * The afterMobsim hook calls doSimStep(lastTime + 1). Without the +1, the event-processing
	 * logic would skip events whose time equals the current sim time, leaving VehicleLeavesTrafficEvent
	 * at the final timestep unprocessed — and no energy would be discharged for the last link.
	 */
	@Test
	public void testLastTimestepEventsProcessedInAfterMobsim() {
		Config config = createBaseConfig(utils.getOutputDirectory(), 3600.0);
		config.scoring().addActivityParams(new ActivityParams("home").setScoringThisActivityAtAll(false));
		config.scoring().addActivityParams(new ActivityParams("work").setScoringThisActivityAtAll(false));

		Scenario scenario = ScenarioUtils.createScenario(config);

		// n0 -[l0]-> n1 -[l1]-> n2, each link 1000 m at 1000 m/s = 1 second traversal.
		// Agent departs from l0 at t=3599. Vehicle immediately leaves l0 (departure link) and
		// traverses l1 for 1 second, so VehicleLeavesTrafficEvent fires at t=3600 (last sim step).
		createTwoLinkNetwork(scenario);

		Vehicle vehicle = addElectricVehicle(scenario);

		// POPULATION
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		Person person = populationFactory.createPerson(Id.createPersonId("person"));
		population.addPerson(person);

		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);

		PopulationUtils.insertVehicleIdsIntoPersonAttributes(person,
			Collections.singletonMap("car", vehicle.getId()));

		Activity homeActivity = populationFactory.createActivityFromLinkId("home", Id.createLinkId("l0"));
		homeActivity.setEndTime(3599.0);
		plan.addActivity(homeActivity);

		Leg carLeg = populationFactory.createLeg("car");
		carLeg.setRoute(RouteUtils.createLinkNetworkRouteImpl(
			Id.createLinkId("l0"),
			Collections.emptyList(),
			Id.createLinkId("l1")));
		plan.addLeg(carLeg);

		Activity workActivity = populationFactory.createActivityFromLinkId("work", Id.createLinkId("l1"));
		plan.addActivity(workActivity);

		// CONTROLLER
		Controler controller = createController(scenario);

		// Simple 1 J/m drive model so we get deterministic, expected energy consumption.
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(org.matsim.contrib.ev.discharging.DriveEnergyConsumption.Factory.class)
					.toInstance(_ -> (link, _, _) -> link.getLength());
				bind(org.matsim.contrib.ev.discharging.AuxEnergyConsumption.Factory.class)
					.toInstance(_ -> (_, _, _) -> 0.0);
			}
		});

		SummedEnergy energy = new SummedEnergy();
		energy.install(controller);

		controller.run();

		// The vehicle traverses l1 (1000 m, 1 J/m = 1000 J). l0 is the departure (first) link
		// and is skipped by DriveDischargingHandler. The VehicleLeavesTrafficEvent for l1 fires
		// at t=3600 (the last sim step) and is only processed by afterMobsim via doSimStep(3601).
		// Without the +1 fix in afterMobsim, energy would be 0.
		assertEquals(EvUnits.J_to_kWh(1000.0), energy.energy_kWh, 1e-9);
	}

	private static Config createBaseConfig(String outputDir, double endTime) {
		Config config = ConfigUtils.createConfig();
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(outputDir);
		config.controller().setLastIteration(0);
		config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);
		config.qsim().setSimEndtimeInterpretation(EndtimeInterpretation.onlyUseEndtime);
		config.qsim().setEndTime(endTime);
		config.addModule(new EvConfigGroup());
		// Routes are set explicitly on legs — disable access/egress routing and consistency checks.
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		return config;
	}

	/** Creates a 3-node, 2-link linear network: n0 -[l0]-> n1 -[l1]-> n2, each link 1000 m at 1000 m/s. */
	private static void createTwoLinkNetwork(Scenario scenario) {
		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();

		Node n0 = factory.createNode(Id.createNodeId("n0"), new Coord(0.0, 0.0));
		Node n1 = factory.createNode(Id.createNodeId("n1"), new Coord(1000.0, 0.0));
		Node n2 = factory.createNode(Id.createNodeId("n2"), new Coord(2000.0, 0.0));
		network.addNode(n0);
		network.addNode(n1);
		network.addNode(n2);

		Link l0 = factory.createLink(Id.createLinkId("l0"), n0, n1);
		l0.setLength(1000.0);
		l0.setFreespeed(1000.0);
		l0.setCapacity(3600.0);
		l0.setAllowedModes(Collections.singleton("car"));
		network.addLink(l0);

		Link l1 = factory.createLink(Id.createLinkId("l1"), n1, n2);
		l1.setLength(1000.0);
		l1.setFreespeed(1000.0);
		l1.setCapacity(3600.0);
		l1.setAllowedModes(Collections.singleton("car"));
		network.addLink(l1);
	}

	private static Vehicle addElectricVehicle(Scenario scenario) {
		VehiclesFactory vehiclesFactory = scenario.getVehicles().getFactory();
		VehicleType vehicleType = vehiclesFactory.createVehicleType(Id.createVehicleTypeId("electric"));
		vehicleType.setNetworkMode("car");
		VehicleUtils.setHbefaTechnology(vehicleType.getEngineInformation(), ElectricFleetUtils.EV_ENGINE_HBEFA_TECHNOLOGY);
		VehicleUtils.setEnergyCapacity(vehicleType.getEngineInformation(), 50.0);
		scenario.getVehicles().addVehicleType(vehicleType);
		Vehicle vehicle = vehiclesFactory.createVehicle(Id.createVehicleId("vehicle"), vehicleType);
		ElectricFleetUtils.setInitialSoc(vehicle, 1.0);
		scenario.getVehicles().addVehicle(vehicle);
		return vehicle;
	}

	private static Controler createController(Scenario scenario) {
		Controler controller = new Controler(scenario);
		controller.addOverridingModule(new EvModule());
		EvUtils.registerInfrastructure(controller, new ChargingInfrastructureSpecificationDefaultImpl());
		return controller;
	}

	static public class SummedEnergy implements DrivingEnergyConsumptionEventHandler {
		public double energy_kWh = 0.0;

		@Override
		public void handleEvent(DrivingEnergyConsumptionEvent event) {
			energy_kWh += EvUnits.J_to_kWh(event.getEnergy());
		}

		public void install(Controler controller) {
			SummedEnergy self = this;

			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(self);
				}
			});
		}
	}
}
