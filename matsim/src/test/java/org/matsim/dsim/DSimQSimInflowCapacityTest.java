package org.matsim.dsim;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces <a href="https://github.com/matsim-org/matsim-libs/issues/5239">#5239</a>. With kinematic waves, a qsim link refills its
 * inflow capacity only while it is active ({@code QueueWithBuffer#doSimStep}), and {@code QueueWithBuffer#isActive} does not keep the link
 * active for that. A vehicle that consumes more inflow capacity than the link refills while the vehicle is on it, such as a truck on a short
 * link, leaves the link inactive with a negative inflow capacity. The link then rejects the next vehicle until that vehicle is stuck,
 * however much later it comes. dsim refills its inflow capacity from the elapsed time ({@code FlowCapacity#update}).
 * <p>
 * The scenario is the one from the issue:
 * <ul>
 *     <li>A → S → B at 50 km/h;</li>
 *     <li>A and B are 100 m long, with 2 lanes and 7200 veh/h;</li>
 *     <li>S has 1 lane and 3600 veh/h;</li>
 *     <li>kinematic waves, with a stuck time of 30 s.</li>
 * </ul>
 * Each test checks when the vehicles enter S. The expected times are those of a run that processes every link in every time step: a vehicle
 * departing from A enters S one second later. qsim fails these tests with both the fast and the slow capacity update until the issue is
 * fixed.
 */
class DSimQSimInflowCapacityTest {

	private static final double STUCK_TIME = 30;

	private static final double END_TIME = 3 * 3600;

	private static final double FREESPEED = 50 / 3.6;

	// Ids are created where they are used, because AutoResetIdCaches resets the Id caches between tests
	private static final String A = "A";
	private static final String S = "S";
	private static final String B = "B";

	private static final String CAR = "car";
	private static final String TRUCK = "truck";

	private static final double TRUCK_PCU = 3.5;

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	enum Mobsim {
		QSIM_FAST_UPDATE("qsim", true),
		QSIM_SLOW_UPDATE("qsim", false),
		DSIM("dsim", true);

		private final String name;
		/** Only for qsim. dsim always catches up the flow capacity from the elapsed time. */
		private final boolean usingFastCapacityUpdate;

		Mobsim(String name, boolean usingFastCapacityUpdate) {
			this.name = name;
			this.usingFastCapacityUpdate = usingFastCapacityUpdate;
		}
	}

	private record Departure(String vehicleId, String vehicleType, double time) {
	}

	static Stream<Arguments> truckThenCarArguments() {
		return Arrays.stream(Mobsim.values())
			.flatMap(mobsim -> Stream.of(Arguments.of(mobsim, 600.), Arguments.of(mobsim, 7200.)));
	}

	/**
	 * S is 10 m long. A truck departs at midnight and a car 10 minutes or 2 hours later. The truck leaves S with a negative inflow capacity.
	 * On main, qsim lets the car enter S only after the stuck time, at 631 or 7231.
	 */
	@ParameterizedTest(name = "{0}, car departs at {1}")
	@MethodSource("truckThenCarArguments")
	void truckThenCar(Mobsim mobsim, double carDeparture) {
		Map<Id<Vehicle>, Double> enterTimes = run(mobsim, 10, List.of(
			new Departure("truck", TRUCK, 0),
			new Departure("car", CAR, carDeparture)));

		assertThat(enterTimes)
			.as("time at which each vehicle enters S")
			.isEqualTo(Map.of(
				Id.createVehicleId("truck"), 1.,
				Id.createVehicleId("car"), carDeparture + 1));
	}

	/**
	 * S is 3 m long. 8 cars depart, one every 10 minutes. Even a car consumes more inflow capacity than S refills while the car is on it,
	 * so the deficit adds up from car to car. On main, qsim lets cars 5 to 8 enter S only after the stuck time, at departure + 31.
	 */
	@ParameterizedTest
	@EnumSource(Mobsim.class)
	void carsEveryTenMinutesOnA3mLink(Mobsim mobsim) {
		List<Departure> departures = IntStream.range(0, 8)
			.mapToObj(i -> new Departure("car-" + (i + 1), CAR, i * 600.))
			.toList();

		Map<Id<Vehicle>, Double> enterTimes = run(mobsim, 3, departures);

		assertThat(enterTimes)
			.as("time at which each vehicle enters S")
			.isEqualTo(departures.stream()
				.collect(Collectors.toMap(d -> Id.createVehicleId(d.vehicleId()), d -> d.time() + 1)));
	}

	/**
	 * @return the time at which each vehicle enters S, in the order in which they enter it
	 */
	private Map<Id<Vehicle>, Double> run(Mobsim mobsim, double lengthOfS, List<Departure> departures) {

		Config config = ConfigUtils.createConfig();
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);
		config.controller().setOutputDirectory(utils.getOutputDirectory(utils.getParameterizedTestInputString()));
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.controller().setWriteEventsInterval(0);
		config.controller().setMobsim(mobsim.name);

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("h").setTypicalDuration(12 * 3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("w").setTypicalDuration(8 * 3600));

		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		config.qsim().setUsingFastCapacityUpdate(mobsim.usingFastCapacityUpdate);
		config.qsim().setStuckTime(STUCK_TIME);
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(END_TIME);
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);

		config.dsim().setThreads(1);
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.bisect);
		config.dsim().setLinkDynamics(config.qsim().getLinkDynamics());
		config.dsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		config.dsim().setStuckTime(STUCK_TIME);
		config.dsim().setNetworkModes(Set.of(TransportMode.car));
		config.dsim().setStartTime(0);
		config.dsim().setEndTime(END_TIME);
		config.dsim().setVehicleBehavior(config.qsim().getVehicleBehavior());

		Scenario scenario = createScenario(config, lengthOfS, departures);

		EventsCollector collector = new EventsCollector();
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(collector);
			}
		});
		controler.run();

		Id<Link> s = Id.createLinkId(S);
		return collector.getEvents().stream()
			.filter(e -> e instanceof LinkEnterEvent enter && enter.getLinkId().equals(s))
			.map(e -> (LinkEnterEvent) e)
			.collect(Collectors.toMap(LinkEnterEvent::getVehicleId, LinkEnterEvent::getTime, (a, b) -> {
				throw new IllegalStateException("a vehicle entered S twice");
			}, LinkedHashMap::new));
	}

	private static Scenario createScenario(Config config, double lengthOfS, List<Departure> departures) {

		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();

		Node n0 = network.getFactory().createNode(Id.createNodeId("n0"), new Coord(0, 0));
		Node n1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(100, 0));
		Node n2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(100 + lengthOfS, 0));
		Node n3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(200 + lengthOfS, 0));
		for (Node node : List.of(n0, n1, n2, n3)) {
			network.addNode(node);
		}
		Id<Link> a = Id.createLinkId(A);
		Id<Link> s = Id.createLinkId(S);
		Id<Link> b = Id.createLinkId(B);
		addLink(network, a, n0, n1, 100, 7200, 2);
		addLink(network, s, n1, n2, lengthOfS, 3600, 1);
		addLink(network, b, n2, n3, 100, 7200, 2);
		// unused, but the router needs a strongly connected network
		addLink(network, Id.createLinkId("back"), n3, n0, 200 + lengthOfS, 7200, 2);

		VehicleType car = VehicleUtils.createVehicleType(Id.create(CAR, VehicleType.class), TransportMode.car);
		VehicleType truck = VehicleUtils.createVehicleType(Id.create(TRUCK, VehicleType.class), TransportMode.car)
			.setPcuEquivalents(TRUCK_PCU);
		scenario.getVehicles().addVehicleType(car);
		scenario.getVehicles().addVehicleType(truck);
		Map<String, VehicleType> types = Map.of(CAR, car, TRUCK, truck);

		PopulationFactory factory = scenario.getPopulation().getFactory();
		for (Departure departure : departures) {
			Id<Vehicle> vehicleId = Id.createVehicleId(departure.vehicleId());
			scenario.getVehicles().addVehicle(VehicleUtils.createVehicle(vehicleId, types.get(departure.vehicleType())));

			Person person = factory.createPerson(Id.createPersonId(departure.vehicleId()));
			VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, Map.of(TransportMode.car, vehicleId));

			Plan plan = factory.createPlan();
			Activity home = factory.createActivityFromLinkId("h", a);
			home.setEndTime(departure.time());
			plan.addActivity(home);

			Leg leg = factory.createLeg(TransportMode.car);
			TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			leg.setRoute(RouteUtils.createLinkNetworkRouteImpl(a, List.of(s), b));
			plan.addLeg(leg);

			plan.addActivity(factory.createActivityFromLinkId("w", b));

			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}

		return scenario;
	}

	private static void addLink(Network network, Id<Link> id, Node from, Node to, double length, double capacity, double lanes) {
		Link link = network.getFactory().createLink(id, from, to);
		link.setLength(length);
		link.setFreespeed(FREESPEED);
		link.setCapacity(capacity);
		link.setNumberOfLanes(lanes);
		link.setAllowedModes(Set.of(TransportMode.car));
		network.addLink(link);
	}
}
