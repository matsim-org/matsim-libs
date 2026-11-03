package org.matsim.dsim;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * dsim is meant to replace qsim, so the same scenario must give the same traffic in both. Each test here runs one corridor
 * l0 → l1 → l2 → l3 through both mobsims and compares the number of vehicles that enter one link within one hour. Each test
 * isolates one property of the link dynamics, so that a difference can be traced to it.
 * <p>
 * The corridor is sampled like a scenario: {@code sample * VEHICLES} vehicles depart from l0 at midnight, with flow and storage
 * capacity factors of {@code sample}. qsim scales the links with these factors, dsim scales the vehicles instead, to a pce of
 * 1 / flowCapacityFactor ({@link org.matsim.dsim.simulation.ScaledQVehicleFactory}). 0.1 and 0.25 stand for sampled scenarios, 4 for a
 * scaled-up capacity as in the cadyts calibration of Open Berlin (25 % sample, five copies of each car driver, capacity factor 4).
 */
class DSimQSimLinkDynamicsTest {

	/** Vehicles departing at midnight, before sampling. */
	private static final int VEHICLES = 4000;

	private static final double END_TIME = 2 * 3600;

	/** A stuck time that is never reached, for the tests in which vehicles must not be pushed onto full links. */
	private static final double NEVER_STUCK = 100 * 3600;

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * The control: an outflow bottleneck, without inflow capacity and without stuck vehicles. l1 lets 1800 veh/h out, so about
	 * {@code 1800 * sample} vehicles enter l2 in the first hour.
	 */
	@ParameterizedTest
	@ValueSource(doubles = {0.1, 0.25, 1, 4})
	void sameOutflowCapacity(double sample) {
		assertSameVolume(sample, TrafficDynamics.queue, NEVER_STUCK, network -> link(network, "l1").setCapacity(1800),
			"l2", 0, 3600);
	}

	/**
	 * The inflow capacity of {@code kinematicWaves}, without stuck vehicles. l1 has one lane at 36 km/h, which the fundamental
	 * diagram limits to an inflow of 1412 veh/h, below its 3600 veh/h flow capacity.
	 * <p>
	 * qsim does not scale the inflow capacity with the flow capacity factor (see the comment on maxFlowFromFdiag in
	 * {@code QueueWithBuffer}, and matsim-libs #3770 and #3845), dsim counts it in pce, which scales it. So the two agree only at 1.
	 */
	@ParameterizedTest
	@ValueSource(doubles = {1})
	void sameInflowCapacity(double sample) {
		assertSameVolume(sample, TrafficDynamics.kinematicWaves, NEVER_STUCK, network -> {
		}, "l1", 0, 3600);
	}

	/**
	 * {@link #sameInflowCapacity} with sampled and scaled-up capacities. Below 1, qsim lets in as many vehicles as leave l1, at 4 it
	 * lets in 1412 veh/h. dsim lets in {@code 1412 * sample}. Measured at 0.1, 0.25 and 4: qsim 358, 895 and 1412, dsim 142, 353 and
	 * 5646.
	 */
	@Disabled("known difference: qsim does not scale the kinematicWaves inflow capacity with the flow capacity factor, dsim does")
	@ParameterizedTest
	@ValueSource(doubles = {0.1, 0.25, 4})
	void sameInflowCapacityWhenSampled(double sample) {
		sameInflowCapacity(sample);
	}

	/**
	 * Vehicles pushed onto a full link once they are stuck, without inflow capacity. l2 is 75 m long and lets 10 veh/h out, so it is
	 * full at once and the queue reaches back over l1. From then on, vehicles enter l2 only because the vehicle at the head of l1 has
	 * waited for the stuck time of 30 s, and l2 holds more vehicles than its storage capacity.
	 * <p>
	 * Both mobsims restart the stuck time whenever a vehicle leaves the buffer ({@code bufferLastMovedTime} in qsim's
	 * {@code QueueWithBuffer}, {@code lastMovedTime} in {@code SimBuffer}), so they move one vehicle per stuck time, also at 4, where
	 * the buffer of l1 holds four vehicles. And in both, the vehicles in the buffer hold on to the flow capacity they consumed until
	 * they leave, so below 1 the next vehicle enters the buffer and starts its stuck time 1 / sample seconds later. qsim moves a vehicle
	 * after more than the stuck time, dsim after the stuck time.
	 */
	@ParameterizedTest
	@ValueSource(doubles = {0.1, 0.25, 1, 4})
	void sameStuckVehiclesOverAFullLink(double sample) {
		assertSameVolume(sample, TrafficDynamics.queue, 30, network -> {
			link(network, "l2").setCapacity(10);
			link(network, "l2").setLength(75);
		}, "l2", 3600, 7200);
	}

	private void assertSameVolume(double sample, TrafficDynamics dynamics, double stuckTime, Consumer<Network> bottleneck,
								  String linkId, double from, double to) {

		long qsim = run("qsim", sample, dynamics, stuckTime, bottleneck, linkId, from, to);
		long dsim = run("dsim", sample, dynamics, stuckTime, bottleneck, linkId, from, to);

		System.out.printf("sample %s, %s, stuck time %s: vehicles entering %s between %s and %s: qsim %d, dsim %d%n",
			sample, dynamics, stuckTime, linkId, from, to, qsim, dsim);

		assertThat(qsim).as("the scenario must produce traffic on %s to be a meaningful test", linkId).isPositive();
		assertThat(dsim).as("vehicles entering %s between %s and %s, at sample %s", linkId, from, to, sample)
			.isCloseTo(qsim, withinPercentage(5));
	}

	private long run(String mobsim, double sample, TrafficDynamics dynamics, double stuckTime, Consumer<Network> bottleneck,
					 String linkId, double from, double to) {

		Config config = ConfigUtils.createConfig();
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);
		config.controller().setOutputDirectory(utils.getOutputDirectory() + mobsim + "-" + sample);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.controller().setWriteEventsInterval(0);
		config.controller().setMobsim(mobsim);

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("h").setTypicalDuration(12 * 3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("w").setTypicalDuration(8 * 3600));

		config.qsim().setFlowCapFactor(sample);
		config.qsim().setStorageCapFactor(sample);
		config.qsim().setTrafficDynamics(dynamics);
		config.qsim().setStuckTime(stuckTime);
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(END_TIME);

		config.dsim().setThreads(1);
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.bisect);
		config.dsim().setLinkDynamics(config.qsim().getLinkDynamics());
		config.dsim().setTrafficDynamics(dynamics);
		config.dsim().setStuckTime(stuckTime);
		config.dsim().setNetworkModes(Set.of(TransportMode.car));
		config.dsim().setStartTime(0);
		config.dsim().setEndTime(END_TIME);
		config.dsim().setVehicleBehavior(config.qsim().getVehicleBehavior());

		Scenario scenario = createScenario(config, sample);
		bottleneck.accept(scenario.getNetwork());

		EventsCollector collector = new EventsCollector();
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(collector);
			}
		});
		controler.run();

		Id<Link> id = Id.createLinkId(linkId);
		return collector.getEvents().stream()
			.filter(e -> e instanceof LinkEnterEvent enter && enter.getLinkId().equals(id))
			.filter(e -> e.getTime() >= from && e.getTime() < to)
			.count();
	}

	/**
	 * The corridor, 36 km/h throughout. l0, where the vehicles depart, and l3, where they arrive, never limit the flow. l1 and l2 have
	 * one lane and 3600 veh/h, so they do not either, until a test makes one of them a bottleneck.
	 */
	private static Scenario createScenario(Config config, double sample) {

		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();

		Node n0 = network.getFactory().createNode(Id.createNodeId("n0"), new Coord(0, 0));
		Node n1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(100, 0));
		Node n2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(1100, 0));
		Node n3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(2100, 0));
		Node n4 = network.getFactory().createNode(Id.createNodeId("n4"), new Coord(2200, 0));
		for (Node node : List.of(n0, n1, n2, n3, n4)) {
			network.addNode(node);
		}
		addLink(network, "l0", n0, n1, 100, 36000, 10);
		addLink(network, "l1", n1, n2, 1000, 3600, 1);
		addLink(network, "l2", n2, n3, 1000, 3600, 1);
		addLink(network, "l3", n3, n4, 100, 36000, 10);
		// unused, but the router needs a strongly connected network
		addLink(network, "back", n4, n0, 2200, 36000, 10);

		PopulationFactory factory = scenario.getPopulation().getFactory();
		for (int i = 0; i < Math.round(VEHICLES * sample); i++) {
			Person person = factory.createPerson(Id.createPersonId(i));
			Plan plan = factory.createPlan();

			Activity home = factory.createActivityFromLinkId("h", Id.createLinkId("l0"));
			home.setEndTime(0);
			plan.addActivity(home);

			Leg leg = factory.createLeg(TransportMode.car);
			TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			leg.setRoute(RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("l0"),
				List.of(Id.createLinkId("l1"), Id.createLinkId("l2")), Id.createLinkId("l3")));
			plan.addLeg(leg);

			plan.addActivity(factory.createActivityFromLinkId("w", Id.createLinkId("l3")));

			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}

		return scenario;
	}

	private static void addLink(Network network, String id, Node from, Node to, double length, double capacity, double lanes) {
		Link link = network.getFactory().createLink(Id.createLinkId(id), from, to);
		link.setLength(length);
		link.setFreespeed(10);
		link.setCapacity(capacity);
		link.setNumberOfLanes(lanes);
		link.setAllowedModes(Set.of(TransportMode.car));
		network.addLink(link);
	}

	private static Link link(Network network, String id) {
		return network.getLinks().get(Id.createLinkId(id));
	}
}
