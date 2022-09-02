package org.matsim.pt.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class PtNetworkSimplifierTest {

	@Test
	public void testPtNetworkSimplifier() {
		Scenario scenario = buildScenario();

		Set<Integer> nodeTypesToMerge = new TreeSet<>();
		nodeTypesToMerge.add(NetworkCalcTopoType.PASS1WAY);
		nodeTypesToMerge.add(NetworkCalcTopoType.PASS2WAY);

		PtNetworkSimplifier ptSimplifier = new PtNetworkSimplifier(scenario.getNetwork(),
				scenario.getTransitSchedule());
		ptSimplifier.setNodesToMerge(nodeTypesToMerge);
		ptSimplifier.run();

		// remove unused nodes
		Set<Id<Node>> nodeIdsToRemove = new HashSet<>();
		for (Node node : scenario.getNetwork().getNodes().values()) {
			if (node.getInLinks().isEmpty() && node.getOutLinks().isEmpty()) {
				nodeIdsToRemove.add(node.getId());
			}
		}
		nodeIdsToRemove.stream().forEach(scenario.getNetwork()::removeNode);

		// check network and transit route changes
		List<Id<Link>> addedLinkIds = List.of(
				Id.createLinkId("FG-GH"),
				Id.createLinkId("CX-XY"));
		List<Id<Link>> removedLinkIds = List.of(
				Id.createLinkId("FG"),
				Id.createLinkId("GH"),
				Id.createLinkId("CX"),
				Id.createLinkId("XY"));
		List<Id<Node>> removedNodeIds = List.of(
				Id.createNodeId("G"),
				Id.createNodeId("X"));

		// check network
		for (Id<Link> linkId : addedLinkIds) {
			Assert.assertTrue(scenario.getNetwork().getLinks().keySet().contains(linkId));
		}
		for (Id<Link> linkId : removedLinkIds) {
			Assert.assertFalse(scenario.getNetwork().getLinks().keySet().contains(linkId));
		}
		for (Id<Node> nodeId : removedNodeIds) {
			Assert.assertFalse(scenario.getNetwork().getNodes().keySet().contains(nodeId));
		}

		// check transit routes
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Id<Link> linkId : removedLinkIds) {
					Assert.assertFalse(route.getRoute().getLinkIds().contains(linkId));
				}
			}
		}
		Assert.assertTrue(
				scenario.getTransitSchedule().getTransitLines().get(Id.create("ABCDEFGHI", TransitLine.class))
						.getRoutes().get(Id.create("ABCDEFGHI-route", TransitRoute.class))
						.getRoute().getLinkIds().contains(Id.createLinkId("FG-GH")));
		Assert.assertTrue(
				scenario.getTransitSchedule().getTransitLines().get(Id.create("ABCXYZ", TransitLine.class))
						.getRoutes().get(Id.create("ABCXYZ-route", TransitRoute.class))
						.getRoute().getLinkIds().contains(Id.createLinkId("CX-XY")));
	}

	/**
	 * Creates a scenario with a pt network where * are stop facilities used by
	 * transit lines ABCDEFGHI and ABCXYZ.
	 * 
	 * A -*-> B ---> C ---> D -*-> E ---> F ---> G ---> H -*-> I
	 *               |
	 *               |
	 *               |
	 *               v
	 *               X
	 *               |
	 *               |
	 *               |
	 *               v
	 *               Y
	 *               |
	 *               *
	 *               |
	 *               v
	 *               Z
	 * 
	 */
	static Scenario buildScenario() {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network network = scenario.getNetwork();
		Node a = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), CoordUtils.createCoord(0.0, 0.0));
		Node b = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), CoordUtils.createCoord(10.0, 0.0));
		Node c = NetworkUtils.createAndAddNode(network, Id.createNodeId("C"), CoordUtils.createCoord(20.0, 0.0));
		Node d = NetworkUtils.createAndAddNode(network, Id.createNodeId("D"), CoordUtils.createCoord(30.0, 0.0));
		Node e = NetworkUtils.createAndAddNode(network, Id.createNodeId("E"), CoordUtils.createCoord(40.0, 0.0));
		Node f = NetworkUtils.createAndAddNode(network, Id.createNodeId("F"), CoordUtils.createCoord(50.0, 0.0));
		Node g = NetworkUtils.createAndAddNode(network, Id.createNodeId("G"), CoordUtils.createCoord(60.0, 0.0));
		Node h = NetworkUtils.createAndAddNode(network, Id.createNodeId("H"), CoordUtils.createCoord(70.0, 0.0));
		Node i = NetworkUtils.createAndAddNode(network, Id.createNodeId("I"), CoordUtils.createCoord(80.0, 0.0));
		Node x = NetworkUtils.createAndAddNode(network, Id.createNodeId("X"), CoordUtils.createCoord(20.0, -10.0));
		Node y = NetworkUtils.createAndAddNode(network, Id.createNodeId("Y"), CoordUtils.createCoord(20.0, -20.0));
		Node z = NetworkUtils.createAndAddNode(network, Id.createNodeId("Z"), CoordUtils.createCoord(20.0, -30.0));
		Id<Link> linkIdAB = Id.createLinkId("AB");
		Id<Link> linkIdBC = Id.createLinkId("BC");
		Id<Link> linkIdCD = Id.createLinkId("CD");
		Id<Link> linkIdDE = Id.createLinkId("DE");
		Id<Link> linkIdEF = Id.createLinkId("EF");
		Id<Link> linkIdFG = Id.createLinkId("FG");
		Id<Link> linkIdGH = Id.createLinkId("GH");
		Id<Link> linkIdHI = Id.createLinkId("HI");
		Id<Link> linkIdCX = Id.createLinkId("CX");
		Id<Link> linkIdXY = Id.createLinkId("XY");
		Id<Link> linkIdYZ = Id.createLinkId("YZ");
		Link linkAB = NetworkUtils.createAndAddLink(network, linkIdAB, a, b, 10.0, 80.0 / 3.6, 9999.0, 1);
		Link linkBC = NetworkUtils.createAndAddLink(network, linkIdBC, b, c, 10.0, 80.0 / 3.6, 9999.0, 1);
		Link linkCD = NetworkUtils.createAndAddLink(network, linkIdCD, c, d, 10.0, 80.0 / 3.6, 9999.0, 1);
		Link linkDE = NetworkUtils.createAndAddLink(network, linkIdDE, d, e, 10.0, 80.0 / 3.6, 9999.0, 1);
		Link linkEF = NetworkUtils.createAndAddLink(network, linkIdEF, e, f, 10.0, 80.0 / 3.6, 9999.0, 1);
		Link linkFG = NetworkUtils.createAndAddLink(network, linkIdFG, f, g, 10.0, 80.0 / 3.6, 9999.0, 1);
		Link linkGH = NetworkUtils.createAndAddLink(network, linkIdGH, g, h, 10.0, 80.0 / 3.6, 9999.0, 1);
		Link linkHI = NetworkUtils.createAndAddLink(network, linkIdHI, h, i, 10.0, 80.0 / 3.6, 9999.0, 1);
		Link linkCX = NetworkUtils.createAndAddLink(network, linkIdCX, c, x, 10.0, 80.0 / 3.6, 9999.0, 1);
		Link linkXY = NetworkUtils.createAndAddLink(network, linkIdXY, x, y, 10.0, 80.0 / 3.6, 9999.0, 1);
		Link linkYZ = NetworkUtils.createAndAddLink(network, linkIdYZ, y, z, 10.0, 80.0 / 3.6, 9999.0, 1);
		linkAB.setAllowedModes(Set.of(TransportMode.pt));
		linkBC.setAllowedModes(Set.of(TransportMode.pt));
		linkCD.setAllowedModes(Set.of(TransportMode.pt));
		linkDE.setAllowedModes(Set.of(TransportMode.pt));
		linkEF.setAllowedModes(Set.of(TransportMode.pt));
		linkFG.setAllowedModes(Set.of(TransportMode.pt));
		linkGH.setAllowedModes(Set.of(TransportMode.pt));
		linkHI.setAllowedModes(Set.of(TransportMode.pt));
		linkCX.setAllowedModes(Set.of(TransportMode.pt));
		linkXY.setAllowedModes(Set.of(TransportMode.pt));
		linkYZ.setAllowedModes(Set.of(TransportMode.pt));

		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		TransitStopFacility stopAB = scheduleFactory
				.createTransitStopFacility(Id.create("AB", TransitStopFacility.class), new Coord(5., 0.), false);
		stopAB.setLinkId(linkIdAB);
		schedule.addStopFacility(stopAB);
		TransitStopFacility stopDE = scheduleFactory
				.createTransitStopFacility(Id.create("DE", TransitStopFacility.class), new Coord(35., 0.), false);
		stopDE.setLinkId(linkIdDE);
		schedule.addStopFacility(stopDE);
		TransitStopFacility stopHI = scheduleFactory
				.createTransitStopFacility(Id.create("HI", TransitStopFacility.class), new Coord(75., 0.), false);
		stopHI.setLinkId(linkIdHI);
		schedule.addStopFacility(stopHI);
		TransitStopFacility stopYZ = scheduleFactory
				.createTransitStopFacility(Id.create("YZ", TransitStopFacility.class), new Coord(20., -25.), false);
		stopYZ.setLinkId(linkIdYZ);
		schedule.addStopFacility(stopYZ);

		TransitLine line1 = scheduleFactory.createTransitLine(Id.create("ABCDEFGHI", TransitLine.class));
		NetworkRoute networkRoute1 = RouteUtils.createLinkNetworkRouteImpl(linkIdAB,
				List.of(linkIdBC, linkIdCD, linkIdDE, linkIdEF, linkIdFG, linkIdGH), linkIdHI);
		List<TransitRouteStop> route1Stops = List.of(
				scheduleFactory.createTransitRouteStop(stopAB, 0, 3),
				scheduleFactory.createTransitRouteStop(stopDE, 10, 13),
				scheduleFactory.createTransitRouteStop(stopHI, 20, 23));
		TransitRoute transitRoute1 = scheduleFactory.createTransitRoute(
				Id.create("ABCDEFGHI-route", TransitRoute.class),
				networkRoute1, route1Stops, TransportMode.pt);
		line1.addRoute(transitRoute1);
		schedule.addTransitLine(line1);

		TransitLine line2 = scheduleFactory.createTransitLine(Id.create("ABCXYZ", TransitLine.class));
		NetworkRoute networkRoute2 = RouteUtils.createLinkNetworkRouteImpl(linkIdAB,
				List.of(linkIdBC, linkIdCX, linkIdXY), linkIdYZ);
		List<TransitRouteStop> route2Stops = List.of(
				scheduleFactory.createTransitRouteStop(stopAB, 0, 3),
				scheduleFactory.createTransitRouteStop(stopYZ, 10, 13));
		TransitRoute transitRoute2 = scheduleFactory.createTransitRoute(Id.create("ABCXYZ-route", TransitRoute.class),
				networkRoute2, route2Stops, TransportMode.pt);
		line2.addRoute(transitRoute2);
		schedule.addTransitLine(line2);

		return scenario;
	}

}
