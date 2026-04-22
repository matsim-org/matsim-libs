package org.matsim.contrib.ev.dsim;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.NetworkDecomposition;
import org.matsim.vehicles.*;

import java.util.Collections;
import java.util.List;

/**
 * Shared test fixture for EV + DSim integration tests.
 * <p>
 * Uses the same three-link network topology as {@code ThreeLinkTestFixture} in
 * the distributed-simulation contrib, adapted for EV: car mode on all links,
 * an electric vehicle type, and a simple home → car → work plan that crosses
 * all three partition boundaries.
 */
public final class EvDSimTestFixture {

	/**
	 * Vehicle range in metres with a 1 J/m drive consumption model.
	 * Equals the energy capacity in Joules. Must exceed the total route length (1200 m).
	 */
	static final double BATTERY_CAPACITY = 10_000.0;

	/** Simple energy model: consume 1 Joule per metre of link length. */
	static final DriveEnergyConsumption.Factory DRIVE_CONSUMPTION =
		vehicle -> (link, travelTime, linkEnterTime) -> link.getLength();

	static final AuxEnergyConsumption.Factory AUX_CONSUMPTION =
		vehicle -> (beginTime, duration, linkId) -> 0.0;

	static final String PERSON_ID = "ev-test-person";
	static final String EV_ID = PERSON_ID + "-ev";

	private EvDSimTestFixture() {}

	// -------------------------------------------------------------------------
	// Network
	// -------------------------------------------------------------------------

	/**
	 * Creates the three-link network partitioned into {@code numParts} partitions (2 or 3).
	 * <p>
	 * Topology (linear, north-facing):
	 * <pre>
	 *   n1 (0,0) ──[l1, 100 m, p=0]──► n2 (0,100)
	 *   n2       ──[l2, 1000m, p=1]──► n3 (0,1100)
	 *   n3       ──[l3, 100 m, p=2]──► n4 (0,1200)
	 * </pre>
	 */
	public static Network createNetwork(int numParts) {
		if (numParts < 1 || numParts > 3) {
			throw new IllegalArgumentException("numParts must be 1, 2, or 3");
		}

		var network = NetworkUtils.createNetwork();
		var factory = network.getFactory();

		var n1 = factory.createNode(Id.createNodeId("n1"), new Coord(0, 0));
		var n2 = factory.createNode(Id.createNodeId("n2"), new Coord(0, 100));
		var n3 = factory.createNode(Id.createNodeId("n3"), new Coord(0, 1100));
		var n4 = factory.createNode(Id.createNodeId("n4"), new Coord(0, 1200));

		var l1 = factory.createLink(Id.createLinkId("l1"), n1, n2);
		l1.setLength(100);
		l1.setFreespeed(27.78);
		l1.setCapacity(3600);
		l1.setAllowedModes(Collections.singleton(TransportMode.car));

		var l2 = factory.createLink(Id.createLinkId("l2"), n2, n3);
		l2.setLength(1000);
		l2.setFreespeed(27.78);
		l2.setCapacity(3600);
		l2.setAllowedModes(Collections.singleton(TransportMode.car));

		var l3 = factory.createLink(Id.createLinkId("l3"), n3, n4);
		l3.setLength(100);
		l3.setFreespeed(27.78);
		l3.setCapacity(3600);
		l3.setAllowedModes(Collections.singleton(TransportMode.car));

		// All on partition 0 by default; assign remaining partitions below.
		for (var element : List.of(n1, n2, n3, n4, l1, l2, l3)) {
			element.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
		}
		if (numParts >= 2) {
			n3.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 1);
			l2.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 1);
		}
		if (numParts == 3) {
			n4.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 2);
			l3.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 2);
		}

		for (var n : List.of(n1, n2, n3, n4)) network.addNode(n);
		for (var l : List.of(l1, l2, l3)) network.addLink(l);

		return network;
	}

	// -------------------------------------------------------------------------
	// Vehicles
	// -------------------------------------------------------------------------

	/** Creates and registers an electric vehicle type in {@code vehicles}. */
	public static VehicleType createEvVehicleType(Vehicles vehicles) {
		VehiclesFactory factory = vehicles.getFactory();
		VehicleType type = factory.createVehicleType(Id.create("electric", VehicleType.class));
		type.setNetworkMode(TransportMode.car);
		// Capacity in kWh = RANGE_M metres × 1 J/m converted to kWh
		VehicleUtils.setEnergyCapacity(type.getEngineInformation(), EvUnits.J_to_kWh(BATTERY_CAPACITY));
		VehicleUtils.setHbefaTechnology(type.getEngineInformation(), ElectricFleetUtils.EV_ENGINE_HBEFA_TECHNOLOGY);
		vehicles.addVehicleType(type);
		return type;
	}

	// -------------------------------------------------------------------------
	// Population
	// -------------------------------------------------------------------------

	/**
	 * Creates one EV person with a simple home → car → work plan traversing the
	 * full three-link network (l1 → l2 → l3).  No access/egress walk legs —
	 * use {@link RoutingConfigGroup.AccessEgressType#none} in the config.
	 */
	public static Person createPerson(PopulationFactory factory, Vehicles vehicles, VehicleType evType) {
		// Vehicle
		Vehicle vehicle = vehicles.getFactory().createVehicle(Id.createVehicleId(EV_ID), evType);
		vehicles.addVehicle(vehicle);
		ElectricFleetUtils.setInitialSoc(vehicle, 1.0);  // start fully charged

		// Plan: home ─car→ work
		var plan = factory.createPlan();

		Activity home = factory.createActivityFromLinkId("home", Id.createLinkId("l1"));
		home.setEndTime(10.0);
		plan.addActivity(home);

		Leg carLeg = factory.createLeg(TransportMode.car);
		carLeg.setRoutingMode(TransportMode.car);
		carLeg.setRoute(RouteUtils.createLinkNetworkRouteImpl(
			Id.createLinkId("l1"),
			List.of(Id.createLinkId("l2")),
			Id.createLinkId("l3")));
		plan.addLeg(carLeg);

		Activity work = factory.createActivityFromLinkId("work", Id.createLinkId("l3"));
		plan.addActivity(work);

		var person = factory.createPerson(Id.createPersonId(PERSON_ID));
		person.addPlan(plan);

		VehicleUtils.insertVehicleIdsIntoPersonAttributes(person,
			Collections.singletonMap(TransportMode.car, vehicle.getId()));

		return person;
	}

	// -------------------------------------------------------------------------
	// Config
	// -------------------------------------------------------------------------

	/** Returns a base config suitable for both QSim and DSim EV runs. */
	public static Config createConfig(String outputDir) {
		Config config = ConfigUtils.createConfig();

		config.controller().setOutputDirectory(outputDir);
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(
			org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.none);

		// No access/egress routing — plans are pre-built with explicit routes.
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);
		config.routing().setNetworkRouteConsistencyCheck(
			RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		// QSim
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
		config.qsim().setEndTime(3600.0);
		config.qsim().setVehicleBehavior(QSimConfigGroup.VehicleBehavior.exception);

		// Scoring — needed to avoid config validation errors
		ScoringConfigGroup.ActivityParams homeParams = new ScoringConfigGroup.ActivityParams("home");
		homeParams.setTypicalDuration(8 * 3600);
		config.scoring().addActivityParams(homeParams);
		ScoringConfigGroup.ActivityParams workParams = new ScoringConfigGroup.ActivityParams("work");
		workParams.setTypicalDuration(8 * 3600);
		config.scoring().addActivityParams(workParams);

		// EV config — charger file is overridden programmatically
		EvConfigGroup evConfig = new EvConfigGroup();
		evConfig.setChargersFile("none");
		config.addModule(evConfig);

		return config;
	}

	/**
	 * Configures DSim as the mobsim with the given number of partitions.
	 */
	public static void configureDSim(Config config, int threads) {
		config.controller().setMobsim("dsim");
		config.dsim().setThreads(threads);
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.none);
	}

	// -------------------------------------------------------------------------
	// Scenario
	// -------------------------------------------------------------------------

	/** Builds a complete scenario: three-link network + EV vehicle type + one EV agent. */
	public static Scenario createScenario(Config config, int numNetworkPartitions) {
		Scenario scenario = ScenarioUtils.createScenario(config);

		// Network — nodes must be added before links
		Network network = createNetwork(numNetworkPartitions);
		network.getNodes().values().forEach(n -> scenario.getNetwork().addNode(n));
		network.getLinks().values().forEach(l -> scenario.getNetwork().addLink(l));

		// Vehicles
		VehicleType evType = createEvVehicleType(scenario.getVehicles());

		// Population
		Person person = createPerson(scenario.getPopulation().getFactory(),
			scenario.getVehicles(), evType);
		scenario.getPopulation().addPerson(person);

		return scenario;
	}

	// -------------------------------------------------------------------------
	// Controller helpers
	// -------------------------------------------------------------------------

	/**
	 * Adds the EV modules and bindings required by all integration tests in this
	 * package.  Provides an empty charging infrastructure (no chargers).
	 */
	public static void installEvModules(Controler controller) {
		controller.addOverridingModule(new EvModule());
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(DriveEnergyConsumption.Factory.class).toInstance(DRIVE_CONSUMPTION);
				bind(AuxEnergyConsumption.Factory.class).toInstance(AUX_CONSUMPTION);
				// this is necessary to prevent the ev module from loading chargeres from an xml file, which we don't have
				bind(ChargingInfrastructureSpecification.class)
					.toInstance(new ChargingInfrastructureSpecificationDefaultImpl());
			}
		});
	}
}
