package org.matsim.contrib.taxi.rides;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.dvrp.analysis.ExecutedScheduleCollector;
import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarks;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.etaxi.run.ETaxiConfigGroups;
import org.matsim.contrib.taxi.analysis.TaxiEventSequenceCollector;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.benchmark.TaxiBenchmarkStats;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.rides.util.PartialEvent;
import org.matsim.contrib.taxi.rides.util.Utils;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

public class ExpireOrderTest {
	private final Logger logger = Logger.getLogger(ExpireOrderTest.class);

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	class GridNetworkGenerator {
		final Network network;
		final double linkLength = 100;
		final double linkFreeSpeed = 15;
		final double linkCapacity = 100;
		final double linkLanes = 1;
		GridNetworkGenerator(Network network, int xNodes, int yNodes) {
			this.network = network;
			this.build(xNodes, yNodes);
		}
		Id<Node> nodeId(int x, int y) {
			return Id.create(String.format("%d_%d", x, y), Node.class);
		}
		Node node(int x, int y) {
			return network.getNodes().get(nodeId(x, y));
		}
		Id<Link> linkId(Node from, Node to) {
			return Id.create(from.getId() + "-" + to.getId(), Link.class);
		}
		Id<Link> linkId(int fromX, int fromY, int toX, int toY) {
			return linkId(node(fromX, fromY), node(toX, toY));
		}
		Coord nodeCoord(int x, int y) {
			return new Coord(x * 1000, y * 1000);
		}
		private void addLink(Node a, Node b) {
			NetworkUtils.createAndAddLink(network, linkId(a, b), a, b, linkLength, linkFreeSpeed, linkCapacity,  linkLanes);
		}
		private void addDoubleLink(Node a, Node b) {
			addLink(a, b);
			addLink(b, a);
		}
		void build(int xNodes, int yNodes) {
			for (int x = 0; x < xNodes; ++x) {
				for (int y = 0; y < yNodes; ++y) {
					NetworkUtils.createAndAddNode(network, nodeId(x, y), nodeCoord(x, y));
				}
			}
			for (int x = 0; x < xNodes; ++x) {
				for (int y = 0; y < yNodes; ++y) {
					Node crtNode = node(x, y);
					Node leftNode = node(x-1, y);
					Node downNode = node(x, y-1);
					if (leftNode != null) {
						addDoubleLink(leftNode, crtNode);
					}
					if (downNode != null) {
						addDoubleLink(downNode, crtNode);
					}
				}
			}
		}
	}

	void generatePassenger(Scenario scenario, GridNetworkGenerator network, int passengerId, Id<Link> fromLink, Id<Link> toLink, Double departureTime) {
		// TODO(CTudorache): utility method for generating passenger id: "passenger_%d"
		Person person = PopulationUtils.getFactory().createPerson(Id.create("passenger_" + passengerId, Person.class));
		PersonUtils.setEmployed(person, false);

		Plan plan = PopulationUtils.createPlan(person);
		person.addPlan(plan);
		person.setSelectedPlan(plan);

		Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "dummy", fromLink);
		a.setEndTime(departureTime);

		Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.taxi);

		PopulationUtils.createAndAddActivityFromLinkId(plan, "dummy", toLink);
		scenario.getPopulation().addPerson(person);
	}

	void generateVehicle(Scenario scenario, GridNetworkGenerator network, int vehicleId, Id<Link> startLink, Double t0, Double t1) {
		// TODO(CTudorache): vehType needs to be constant
		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("taxiType", VehicleType.class));
		vehType.getCapacity().setSeats(4);

		// TODO(CTudorache): utility method for generating vehicle id: "taxi_vehicle_%d"
		Vehicle v = VehicleUtils.createVehicle(Id.create("taxi_vehicle_" + vehicleId, Vehicle.class), vehType);
		// TODO(CTudorache): do these attributes work? could not find an example of generating vehicle with start_link, t_0, t_1
		v.getAttributes().putAttribute("start_link", startLink.toString());
		v.getAttributes().putAttribute("t_0", t0.toString());
		v.getAttributes().putAttribute("t_1", t1.toString());
		scenario.getVehicles().addVehicle(v);
	}

	@Test
	public void testExpireOrderGen() {
		TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
		taxiCfg.setBreakSimulationIfNotAllRequestsServed(false);
		taxiCfg.setDestinationKnown(false);
		taxiCfg.setVehicleDiversion(false);
		taxiCfg.setPickupDuration(120);
		taxiCfg.setDropoffDuration(60);
		taxiCfg.setTimeProfiles(true);
		taxiCfg.setDetailedStats(true);
		taxiCfg.setMaxSearchDuration(65.0); // order should expire in 65 seconds
		taxiCfg.setRequestAcceptanceDelay(0.0);

		RuleBasedTaxiOptimizerParams ruleParams = new RuleBasedTaxiOptimizerParams();
		//logger.warn("CTudorache taxiCfg.getTaxiOptimizerParams(): " + taxiCfg.getTaxiOptimizerParams());
		//RuleBasedTaxiOptimizerParams ruleParams = ((RuleBasedTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams());
		ruleParams.setReoptimizationTimeStep(1);
		ruleParams.setNearestVehiclesLimit(30);
		//taxiCfg.addOptimizerParamsDefinition(RuleBasedTaxiOptimizerParams.SET_NAME, () -> ruleParams);
		taxiCfg.addParameterSet(ruleParams);

		Config config = ConfigUtils.createConfig(new MultiModeTaxiConfigGroup(() -> taxiCfg), new DvrpConfigGroup());
		logger.warn("CTudorache modules: " + config.getModules().keySet());

		config.controler().setOutputDirectory("test/output/abcdef");
		config.controler().setLastIteration(0);
		config.controler().setDumpDataAtEnd(false);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setCreateGraphs(false);
		DvrpBenchmarks.adjustConfig(config);


		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		network.setCapacityPeriod(Time.parseTime("1:00:00"));

		GridNetworkGenerator gn = new GridNetworkGenerator(network, 3, 3);
		generatePassenger(scenario, gn, 1, gn.linkId(0, 1, 0, 0), gn.linkId(2, 0, 2, 1), 0.0);
		generatePassenger(scenario, gn, 2, gn.linkId(0, 1, 0, 2), gn.linkId(2, 2, 2, 1), 5.0);
		generateVehicle(scenario, gn, 1, gn.linkId(1, 2, 2, 2), 0.0, 1000.0);


		Controler controler = new Controler(scenario);
		DvrpBenchmarks.initController(controler);

		String mode = TaxiConfigGroup.getSingleModeTaxiConfig(config).getMode();
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(mode));

		controler.addOverridingModule(new MultiModeTaxiModule());
		// TODO(CTudorache): probably not required
		controler.addOverridingModule(new AbstractDvrpModeModule(mode) {
			@Override
			public void install() {
				bindModal(TaxiBenchmarkStats.class).toProvider(modalProvider(
						getter -> new TaxiBenchmarkStats(getter.get(OutputDirectoryHierarchy.class),
								getter.getModal(ExecutedScheduleCollector.class),
								getter.getModal(TaxiEventSequenceCollector.class)))).asEagerSingleton();
				addControlerListenerBinding().to(modalKey(TaxiBenchmarkStats.class));
			}
		});

		EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);

		controler.run();

		List<Event> allEvents = collector.getEvents();
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(0.0, PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(1.0, PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(5.0, PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(71.0, PassengerRequestRejectedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1")
		));

	}

	@Test
	public void testExpireOrder() {
		// TODO: create test scenario. Should be reserved for automated tests only.
		//       Grid network, dynamic vehicles, dynamic orders.
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("taxi-rides-test-base"), "config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup());
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);
		taxiCfg.setBreakSimulationIfNotAllRequestsServed(false);
		taxiCfg.setMaxSearchDuration(65.0); // order should expire in 65 seconds
		taxiCfg.setRequestAcceptanceDelay(0.0);
		RuleBasedTaxiOptimizerParams ruleParams = ((RuleBasedTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams());
		ruleParams.setReoptimizationTimeStep(1);

		// NOTE: These are already set in config.xml
		//config.plans().setInputFile("population_1.xml");
		//taxiCfg.setTaxisFile("vehicles_1.xml");

		config.controler().setOutputDirectory("test/output/abcdef");

		Controler controler = RunTaxiBenchmark.createControler(config, 1);

		EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);

		controler.run();

		List<Event> allEvents = collector.getEvents();
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(0.0, PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(1.0, PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(5.0, PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(71.0, PassengerRequestRejectedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1")
		));
	}
}
