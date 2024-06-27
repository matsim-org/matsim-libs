package ch.sbb.matsim.routing.pt.raptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A number of unit tests related to restricted boarding and alighting at stops,
 * i.e. when a TransitRouteStop has either allowBoarding or allowAlighting set to <code>false</code>.
 *
 * @author mrieser / Simunto
 */
public class SwissRailRaptorRestrictedBoardingAlightingTest {

	private static SwissRailRaptor createTransitRouter(TransitSchedule schedule, Config config, Network network) {
		SwissRailRaptorData data = SwissRailRaptorData.create(schedule, null, RaptorUtils.createStaticConfig(config), network, null);
		SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, config).build();
		return raptor;
	}

	@Test
	public void testMustUseSlowerGreenLine() {
		Fixture f = new Fixture();
		TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);

		Coord act0Coord = new Coord(0, 10);
		Coord act1Coord = new Coord(10000, 10);
		Coord act2Coord = new Coord(20000, 10);
		Coord act3Coord = new Coord(30000, 10);

		// travelling from node 0 to node 1: blue line is faster, but one must not exit at stop 1, so green line must be used
		List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(act0Coord), new FakeFacility(act1Coord), 5.95*3600, null));
		assertEquals(3, legs.size());
		assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
		assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
		assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

		Leg leg1 = (Leg) legs.get(1);
		assertInstanceOf(TransitPassengerRoute.class, leg1.getRoute());
		TransitPassengerRoute paxRoute = (TransitPassengerRoute) leg1.getRoute();
		assertEquals("Green", paxRoute.getLineId().toString());
		assertEquals(6*3600 + 900.0, leg1.getDepartureTime().seconds() + leg1.getTravelTime().seconds());

		// travelling from node 0 to node 2: blue line is faster and can be used
		legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(act0Coord), new FakeFacility(act2Coord), 5.95*3600, null));
		assertEquals(3, legs.size());
		assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
		assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
		assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

		leg1 = (Leg) legs.get(1);
		assertInstanceOf(TransitPassengerRoute.class, leg1.getRoute());
		paxRoute = (TransitPassengerRoute) leg1.getRoute();
		assertEquals("Blue", paxRoute.getLineId().toString());
		assertEquals(6*3600 + 600.0, leg1.getDepartureTime().seconds() + leg1.getTravelTime().seconds());

		// travelling from node 0 to node 3: blue line is faster and can be used
		legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(act0Coord), new FakeFacility(act3Coord), 5.95*3600, null));
		assertEquals(3, legs.size());
		assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
		assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
		assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

		leg1 = (Leg) legs.get(1);
		assertInstanceOf(TransitPassengerRoute.class, leg1.getRoute());
		paxRoute = (TransitPassengerRoute) leg1.getRoute();
		assertEquals("Blue", paxRoute.getLineId().toString());
		assertEquals(6*3600 + 900.0, leg1.getDepartureTime().seconds() + leg1.getTravelTime().seconds());

		// travelling from node 1 to node 3: blue line is faster and can be used
		legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(act1Coord), new FakeFacility(act3Coord), 5.95*3600, null));
		assertEquals(3, legs.size());
		assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
		assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
		assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

		leg1 = (Leg) legs.get(1);
		assertInstanceOf(TransitPassengerRoute.class, leg1.getRoute());
		paxRoute = (TransitPassengerRoute) leg1.getRoute();
		assertEquals("Blue", paxRoute.getLineId().toString());
		assertEquals(6*3600 + 900.0, leg1.getDepartureTime().seconds() + leg1.getTravelTime().seconds());

		// travelling from node 2 to node 3: blue line is faster, but one cannot enter at stop 2, so use slower green one
		legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(act2Coord), new FakeFacility(act3Coord), 5.95*3600, null));
		assertEquals(3, legs.size());
		assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
		assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
		assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

		leg1 = (Leg) legs.get(1);
		assertInstanceOf(TransitPassengerRoute.class, leg1.getRoute());
		paxRoute = (TransitPassengerRoute) leg1.getRoute();
		assertEquals("Green", paxRoute.getLineId().toString());
		assertEquals(6*3600 + 2700.0, leg1.getDepartureTime().seconds() + leg1.getTravelTime().seconds());
	}

	@Test
	public void testMustUseGreenToTransfer() {
		Fixture f = new Fixture();
		TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);

		Coord act0Coord = new Coord(0, 10);
		Coord act5Coord = new Coord(15000, 5010);

		// travelling from node 0 to node 5: blue line is faster to stop 1, but one must not exit at stop 1, so green line must be used
		List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(act0Coord), new FakeFacility(act5Coord), 5.95*3600, null));
		assertEquals(5, legs.size());
		assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
		assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
		assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
		assertEquals(TransportMode.pt, ((Leg)legs.get(3)).getMode());
		assertEquals(TransportMode.walk, ((Leg)legs.get(4)).getMode());

		Leg leg1 = (Leg) legs.get(1);
		assertInstanceOf(TransitPassengerRoute.class, leg1.getRoute());
		TransitPassengerRoute paxRoute = (TransitPassengerRoute) leg1.getRoute();
		assertEquals("Green", paxRoute.getLineId().toString());
		assertEquals(6*3600 + 900.0, leg1.getDepartureTime().seconds() + leg1.getTravelTime().seconds());

		// transfer at stop 1 6:15 - 6:20
		var leg3 = (Leg) legs.get(3);
		assertInstanceOf(TransitPassengerRoute.class, leg3.getRoute());
		paxRoute = (TransitPassengerRoute) leg3.getRoute();
		assertEquals("Red", paxRoute.getLineId().toString());
		assertEquals(6*3600 + 1800.0, leg3.getDepartureTime().seconds() + leg3.getTravelTime().seconds());
	}

	@Test
	public void testCanUseBlueToTransfer() {
		Fixture f = new Fixture();
		TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);

		Coord act3Coord = new Coord(30000, 10);
		Coord act4Coord = new Coord(5000, -5010);

		// travelling from node 4 to node 3: blue line is faster from stop 1 and can be used
		List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(act4Coord), new FakeFacility(act3Coord), 5.95*3600, null));
		assertEquals(5, legs.size());
		assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
		assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
		assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
		assertEquals(TransportMode.pt, ((Leg)legs.get(3)).getMode());
		assertEquals(TransportMode.walk, ((Leg)legs.get(4)).getMode());

		Leg leg1 = (Leg) legs.get(1);
		assertInstanceOf(TransitPassengerRoute.class, leg1.getRoute());
		TransitPassengerRoute paxRoute = (TransitPassengerRoute) leg1.getRoute();
		assertEquals("Red", paxRoute.getLineId().toString());
		assertEquals(6*3600 + 600.0, leg1.getDepartureTime().seconds() + leg1.getTravelTime().seconds());

		// transfer at stop 1 6:10 - 6:15
		var leg3 = (Leg) legs.get(3);
		assertInstanceOf(TransitPassengerRoute.class, leg3.getRoute());
		paxRoute = (TransitPassengerRoute) leg3.getRoute();
		assertEquals("Blue", paxRoute.getLineId().toString());
		assertEquals(6*3600 + 25*60.0, leg3.getDepartureTime().seconds() + leg3.getTravelTime().seconds());
	}

	@Test
	public void testCalculateTree() {
		Fixture f = new Fixture();

		RaptorStaticConfig config = RaptorUtils.createStaticConfig(f.config);
		config.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
		SwissRailRaptorData data = SwissRailRaptorData.create(f.schedule, null, config, f.network, null);
		SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, f.config).build();

		RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

		// start with a stop on the green line
		TransitStopFacility fromStop = f.schedule.getFacilities().get(Id.create(0, TransitStopFacility.class));
		double depTime = 7*3600;
		Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> map = raptor.calcTree(fromStop, depTime, raptorParams, null);

		Assertions.assertEquals(5, map.size(), "wrong number of reached stops.");
		assertNotNull(map.get(Id.create("0", TransitStopFacility.class)));
		assertNotNull(map.get(Id.create("1", TransitStopFacility.class)));
		assertNotNull(map.get(Id.create("2", TransitStopFacility.class)));
		assertNotNull(map.get(Id.create("3", TransitStopFacility.class)));
		assertNull(map.get(Id.create("4", TransitStopFacility.class)));
		assertNotNull(map.get(Id.create("5", TransitStopFacility.class)));

		SwissRailRaptorCore.TravelInfo infoTo5 = map.get(Id.create("5", TransitStopFacility.class));
		RaptorRoute raptorRoute = infoTo5.getRaptorRoute();
		assertEquals(4, raptorRoute.parts.size());
		// part 0 and part 2 are access walk and transfer
		assertEquals("Green", raptorRoute.parts.get(1).line.getId().toString());
		assertEquals("Red", raptorRoute.parts.get(3).line.getId().toString());
	}

	@Test
	public void testCalculateTreeObservaable() {
		Fixture f = new Fixture();

		RaptorStaticConfig config = RaptorUtils.createStaticConfig(f.config);
		config.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
		SwissRailRaptorData data = SwissRailRaptorData.create(f.schedule, null, config, f.network, null);
		SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, f.config).build();

		RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

		// start with a stop on the green line
		TransitStopFacility fromStop = f.schedule.getFacilities().get(Id.create(0, TransitStopFacility.class));
		Map<Id<TransitStopFacility>, List<RaptorRoute>> foundConnections = new HashMap<>();
		raptor.calcTreesObservable(fromStop, 6*3600.0, 6.01*3600, raptorParams, null,
			(departureTime, stopFacility, arrivalTime, transferCount, route) -> {
			foundConnections.computeIfAbsent(stopFacility.getId(), k -> new ArrayList<>(3)).add(route.get());
			});

		Assertions.assertEquals(4, foundConnections.size(), "wrong number of reached stops.");
		assertNull(foundConnections.get(Id.create("0", TransitStopFacility.class)));
		assertNotNull(foundConnections.get(Id.create("1", TransitStopFacility.class)));
		assertNotNull(foundConnections.get(Id.create("2", TransitStopFacility.class)));
		assertNotNull(foundConnections.get(Id.create("3", TransitStopFacility.class)));
		assertNull(foundConnections.get(Id.create("4", TransitStopFacility.class)));
		assertNotNull(foundConnections.get(Id.create("5", TransitStopFacility.class)));

		List<RaptorRoute> routesTo1 = foundConnections.get(Id.create("1", TransitStopFacility.class));

		assertEquals(1, routesTo1.size(), "stop 1 should only be reachable by green line");
		assertEquals("Green", routesTo1.get(0).parts.get(1).line.getId().toString());

		List<RaptorRoute> routesTo2 = foundConnections.get(Id.create("2", TransitStopFacility.class));
		assertEquals(1, routesTo2.size(), "stop 2 should be reachable by blue line faster");
		assertEquals("Blue", routesTo2.get(0).parts.get(1).line.getId().toString());

		List<RaptorRoute> routesTo5 = foundConnections.get(Id.create("5", TransitStopFacility.class));
		assertEquals(1, routesTo5.size(), "stop 1 should only be reachable by green line");
		assertEquals("Green", routesTo5.get(0).parts.get(1).line.getId().toString());
		assertEquals("Red", routesTo5.get(0).parts.get(3).line.getId().toString());
	}

	/**
	 * Generates the following network for testing:
	 * <pre>
	 * (n) Node
	 * [s] Stop Facility
	 *  l  Link
	 *                 (5)[5]
	 *                 /
	 *                /
	 *               5
	 *              /
	 *             /
	 * (0)---1---(1)---2---(2)---3---(3)
	 * [0]       [1]       [2]       [3]
	 *           /
	 *          /
	 *         4
	 *        /
	 *       /
	 *      (4)[4]
	 *
	 * There are three transit lines: the Blue and Green lines from 0 to 3, and the Red line from 4 to 5.
	 * Travel times between stops are 15 minutes on the Green line, 5 minutes on the Blue line, and 10 minutes on the Red line.
	 * Vehicles depart every 10 minutes between 6am and 8am.
	 * </pre>
	 */
	private static class Fixture {

		private final Config config;
		private final Network network;
		private final TransitSchedule schedule;
		private final TransitStopFacility[] stops = new TransitStopFacility[6];

		Fixture() {
			Id<Link> linkId1 = Id.create(1, Link.class);
			Id<Link> linkId2 = Id.create(2, Link.class);
			Id<Link> linkId3 = Id.create(3, Link.class);
			Id<Link> linkId4 = Id.create(4, Link.class);
			Id<Link> linkId5 = Id.create(5, Link.class);

			this.config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);

			this.network = scenario.getNetwork();
			NetworkFactory nf = this.network.getFactory();

			Node[] nodes = new Node[6];
			nodes[0] = nf.createNode(Id.create(0, Node.class), new Coord(0, 0));
			nodes[1] = nf.createNode(Id.create(1, Node.class), new Coord(10000, 0));
			nodes[2] = nf.createNode(Id.create(2, Node.class), new Coord(20000, 0));
			nodes[3] = nf.createNode(Id.create(3, Node.class), new Coord(30000, 0));
			nodes[4] = nf.createNode(Id.create(4, Node.class), new Coord(5000, -5000));
			nodes[5] = nf.createNode(Id.create(5, Node.class), new Coord(15000, 5000));
			for (Node node : nodes) {
				this.network.addNode(node);
			}
			Link link1 = nf.createLink(linkId1, nodes[0], nodes[1]);
			Link link2 = nf.createLink(linkId2, nodes[1], nodes[2]);
			Link link3 = nf.createLink(linkId3, nodes[2], nodes[3]);
			Link link4 = nf.createLink(linkId4, nodes[4], nodes[1]);
			Link link5 = nf.createLink(linkId5, nodes[1], nodes[5]);

			this.network.addLink(link1);
			this.network.addLink(link2);
			this.network.addLink(link3);
			this.network.addLink(link4);
			this.network.addLink(link5);

			this.schedule = scenario.getTransitSchedule();
			TransitScheduleFactory f = schedule.getFactory();

			this.stops[0] = f.createTransitStopFacility(Id.create(0, TransitStopFacility.class), nodes[0].getCoord(), false);
			this.stops[1] = f.createTransitStopFacility(Id.create(1, TransitStopFacility.class), nodes[1].getCoord(), false);
			this.stops[2] = f.createTransitStopFacility(Id.create(2, TransitStopFacility.class), nodes[2].getCoord(), false);
			this.stops[3] = f.createTransitStopFacility(Id.create(3, TransitStopFacility.class), nodes[3].getCoord(), false);
			this.stops[4] = f.createTransitStopFacility(Id.create(4, TransitStopFacility.class), nodes[4].getCoord(), false);
			this.stops[5] = f.createTransitStopFacility(Id.create(5, TransitStopFacility.class), nodes[5].getCoord(), false);
			this.stops[0].setLinkId(linkId1);
			this.stops[1].setLinkId(linkId1);
			this.stops[2].setLinkId(linkId2);
			this.stops[3].setLinkId(linkId3);
			this.stops[4].setLinkId(linkId4);
			this.stops[5].setLinkId(linkId5);

			for (TransitStopFacility stop : this.stops) {
				schedule.addStopFacility(stop);
			}

			{
				TransitLine blueLine = f.createTransitLine(Id.create("Blue", TransitLine.class));
				NetworkRoute blueNetRoute = RouteUtils.createLinkNetworkRouteImpl(linkId1, List.of(linkId2), linkId3);
				List<TransitRouteStop> blueStops = new ArrayList<>();
				TransitRouteStop rStop0 = f.createTransitRouteStopBuilder(this.stops[0]).departureOffset(0).build();
				TransitRouteStop rStop1 = f.createTransitRouteStopBuilder(this.stops[1]).arrivalOffset(300).allowAlighting(false).departureOffset(300).build();
				TransitRouteStop rStop2 = f.createTransitRouteStopBuilder(this.stops[2]).arrivalOffset(600).allowBoarding(false).departureOffset(600).build();
				TransitRouteStop rStop3 = f.createTransitRouteStopBuilder(this.stops[3]).arrivalOffset(900).departureOffset(900).build();
				blueStops.add(rStop0);
				blueStops.add(rStop1);
				blueStops.add(rStop2);
				blueStops.add(rStop3);
				TransitRoute blueRoute = f.createTransitRoute(Id.create("Blue", TransitRoute.class), blueNetRoute, blueStops, "train");
				for (int i = 0; i < 13; i++) {
					blueRoute.addDeparture(f.createDeparture(Id.create("blue" + i, Departure.class), 6 * 3600 + i * 600));
				}
				blueLine.addRoute(blueRoute);
				schedule.addTransitLine(blueLine);
			}

			{
				TransitLine greenLine = f.createTransitLine(Id.create("Green", TransitLine.class));
				NetworkRoute greenNetRoute = RouteUtils.createLinkNetworkRouteImpl(linkId1, List.of(linkId2), linkId3);
				List<TransitRouteStop> greenStops = new ArrayList<>();
				TransitRouteStop rStop0 = f.createTransitRouteStopBuilder(this.stops[0]).departureOffset(0).build();
				TransitRouteStop rStop1 = f.createTransitRouteStopBuilder(this.stops[1]).arrivalOffset(900).departureOffset(900).build();
				TransitRouteStop rStop2 = f.createTransitRouteStopBuilder(this.stops[2]).arrivalOffset(1800).departureOffset(1800).build();
				TransitRouteStop rStop3 = f.createTransitRouteStopBuilder(this.stops[3]).arrivalOffset(2700).departureOffset(2700).build();
				greenStops.add(rStop0);
				greenStops.add(rStop1);
				greenStops.add(rStop2);
				greenStops.add(rStop3);
				TransitRoute greenRoute = f.createTransitRoute(Id.create("Green", TransitRoute.class), greenNetRoute, greenStops, "train");
				for (int i = 0; i < 13; i++) {
					greenRoute.addDeparture(f.createDeparture(Id.create("green" + i, Departure.class), 6 * 3600 + i * 600));
				}
				greenLine.addRoute(greenRoute);
				schedule.addTransitLine(greenLine);
			}
			{
				TransitLine redLine = f.createTransitLine(Id.create("Red", TransitLine.class));
				NetworkRoute redNetRoute = RouteUtils.createLinkNetworkRouteImpl(linkId4, linkId5);
				List<TransitRouteStop> redStops = new ArrayList<>();
				TransitRouteStop rStop4 = f.createTransitRouteStopBuilder(this.stops[4]).departureOffset(0).build();
				TransitRouteStop rStop1 = f.createTransitRouteStop(this.stops[1], 600, 600);
				TransitRouteStop rStop5 = f.createTransitRouteStop(this.stops[5], 1200, 1200);
				redStops.add(rStop4);
				redStops.add(rStop1);
				redStops.add(rStop5);
				TransitRoute redRoute = f.createTransitRoute(Id.create("Red", TransitRoute.class), redNetRoute, redStops, "train");
				for (int i = 0; i < 13; i++) {
					redRoute.addDeparture(f.createDeparture(Id.create("red" + i, Departure.class), 6 * 3600 + i * 600));
				}
				redLine.addRoute(redRoute);
				schedule.addTransitLine(redLine);
			}
		}
	}

}
