package org.matsim.contrib.msp.traveltimematrix.prepare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.contrib.zone.skims.FreeSpeedTravelTimeMatrix;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests cutting the study area network. The two properties that matter are that the <b>link ids survive</b>
 * - they are what lets the travel times and volumes of the full run be applied to the cut network - and
 * that the <b>attributes survive</b>, because the road classes are read from them.
 */
class StudyAreaNetworkTest {

	private static final String CRS = "EPSG:25832";

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void aTurnRestrictionOfAnotherModeIsDroppedRatherThanBreakingTheMatrix() {

		// the second way to "Undefined Time", and the one that survives any amount of cleaning for car:
		// the restriction below belongs to truck, so cleaning the network for car leaves it alone - while
		// the router the matrix uses merges the restrictions of every mode. Forbidding 2-3 -> 3-4 then
		// makes n4 unreachable in the matrix's graph, although the links are plainly connected.
		Network network = TestNetworks.corridor();
		// as a real network has it: a car link that trucks may use too. A restriction is only kept if
		// every link of its sequence allows the mode, which is why the modes here are not decoration
		network.getLinks().values().forEach(link -> link.setAllowedModes(Set.of(TransportMode.car, "truck")));
		Link middle = network.getLinks().get(Id.createLinkId("2-3"));
		NetworkUtils.setDisallowedNextLinks(middle, new DisallowedNextLinks.Builder()
			.withDisallowedLinkSequence("truck", List.of(Id.createLinkId("3-4")))
			.build());

		Network cut = StudyAreaNetwork.cutCarNetwork(network, null);

		assertEquals(4, cut.getNodes().size(), "the whole corridor is kept");
		assertNull(NetworkUtils.getDisallowedNextLinks(cut.getLinks().get(Id.createLinkId("2-3"))),
			"and the restriction is gone, which costs a few seconds of detour and buys a matrix that builds");
		assertNotNull(NetworkUtils.getDisallowedNextLinks(middle),
			"the network of the run keeps it - the cut is a copy");
	}

	@Test
	void aDeadEndThatAnotherModeKeepsAliveIsRemovedAsWell() {

		// the trap a real network is full of: a one-way car stub that also allows ride. NetworkUtils
		// .cleanNetwork only takes the *mode* away from a link outside the connected cluster and keeps
		// the link whenever another mode is left on it - while the router of the matrix does not look at
		// modes at all. The cut network would then still hold a node nothing can be routed out of, and
		// the zonal matrix dies on it with "Undefined Time. Reason could be that the dvrp network is not
		// fully connected", after having computed everything else first.
		Network network = TestNetworks.corridor();
		Node n4 = network.getNodes().get(Id.createNodeId("n4"));
		Node n5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("n5"), new Coord(4000, 0));
		Link stub = NetworkUtils.createAndAddLink(network, Id.createLinkId("4-5"), n4, n5, 1000, 10, 1800, 1);
		stub.setAllowedModes(Set.of(TransportMode.car, TransportMode.ride));

		Network cut = StudyAreaNetwork.cutCarNetwork(network, null);

		assertFalse(cut.getLinks().containsKey(Id.createLinkId("4-5")),
			"the stub allows ride after cleaning, but it is not a car link any more and the router does not "
				+ "know the difference");
		assertFalse(cut.getNodes().containsKey(Id.createNodeId("n5")),
			"and the node it led to is gone with it");
		assertEquals(4, cut.getNodes().size());
		assertEquals(6, cut.getLinks().size(), "the corridor itself is untouched");

		// and the point of all that: the matrix can be built. Without the step above this throws
		// "Undefined Time. Reason could be that the dvrp network is not fully connected", because n5 is
		// the central node of its zone and nothing leads out of it.
		ZoneSystem zoneSystem = TestNetworks.oneZonePerNode(cut);
		DvrpTravelTimeMatrixParams params = new DvrpTravelTimeMatrixParams();
		params.addParameterSet(new SquareGridZoneSystemParams());
		params.setMaxNeighborDistance(0);
		assertNotNull(FreeSpeedTravelTimeMatrix.createFreeSpeedMatrix(cut, zoneSystem, params, 1, 1));
	}

	@Test
	void aNetworkThatCannotBeRoutedIsRefusedWithTheNodesThatCauseIt() {

		// one-way out of n4 and no way back, on a link that allows nothing but car: the cleaner removes
		// it, so this is really a test of the check itself - it has to speak up rather than let the
		// matrix fail later
		Network network = TestNetworks.corridor();
		Node n1 = network.getNodes().get(Id.createNodeId("n1"));
		Node n4 = network.getNodes().get(Id.createNodeId("n4"));

		Network disconnected = NetworkUtils.createNetwork();
		NetworkUtils.createAndAddNode(disconnected, n1.getId(), n1.getCoord());
		NetworkUtils.createAndAddNode(disconnected, n4.getId(), n4.getCoord());
		Link oneWay = NetworkUtils.createAndAddLink(disconnected, Id.createLinkId("1-4"),
			disconnected.getNodes().get(n1.getId()), disconnected.getNodes().get(n4.getId()), 3000, 10, 1800, 1);
		oneWay.setAllowedModes(Set.of(TransportMode.car));

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> StudyAreaNetwork.verifyRoutable(disconnected));
		assertTrue(thrown.getMessage().contains("n1") || thrown.getMessage().contains("n4"),
			"the message names a node that cannot be reached: " + thrown.getMessage());
	}

	@Test
	void theLinkIdsAndAttributesSurviveTheCut() {

		Network network = cityWithASuburb();
		Network cut = StudyAreaNetwork.cutCarNetwork(network, null);

		Link inside = cut.getLinks().get(Id.createLinkId("city-1-2"));
		assertNotNull(inside, "a link of the area keeps its id, which is what makes the cut useful at all");
		assertEquals("primary", NetworkUtils.getHighwayType(inside),
			"the road class is read from an attribute, so the attributes have to survive as well");
	}

	@Test
	void onlyLinksOfTheModeAreKept() {

		Network network = cityWithASuburb();
		Network cut = StudyAreaNetwork.cutCarNetwork(network, null);

		assertFalse(cut.getLinks().containsKey(Id.createLinkId("cycleway-1-2")),
			"a link car may not use is not part of a car matrix");
		assertTrue(cut.getLinks().containsKey(Id.createLinkId("city-1-2")));
	}

	@Test
	void onlyLinksInsideTheAreaAreKept() {

		Network network = cityWithASuburb();
		Network cut = StudyAreaNetwork.cutCarNetwork(network, studyArea());

		assertTrue(cut.getLinks().containsKey(Id.createLinkId("city-1-2")));
		assertFalse(cut.getLinks().containsKey(Id.createLinkId("suburb-3-4")),
			"a link outside the study area is not part of the matrix");
		assertFalse(cut.getLinks().containsKey(Id.createLinkId("city-2-3")),
			"a link leading out of the area has one node outside and is dropped with it");
	}

	@Test
	void theCutNetworkIsConnected() {

		Network network = cityWithASuburb();
		Network cut = StudyAreaNetwork.cutCarNetwork(network, studyArea());

		// a zonal matrix refuses a pair it finds no path for, so a dangling link is worse than a missing
		// one: every node left has to be reachable from every other
		for (Node node : cut.getNodes().values()) {
			assertFalse(node.getInLinks().isEmpty(), node.getId() + " has no incoming link");
			assertFalse(node.getOutLinks().isEmpty(), node.getId() + " has no outgoing link");
		}
	}

	@Test
	void theNetworkOfTheRunIsNotModified() {

		Network network = cityWithASuburb();
		int linksBefore = network.getLinks().size();

		StudyAreaNetwork.cutCarNetwork(network, studyArea());

		assertEquals(linksBefore, network.getLinks().size(), "the cut is a copy, so the run's network stays whole");
	}

	@Test
	void anEmptyCutIsRefusedWithAHintAboutTheProjection() {

		Network network = cityWithASuburb();

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> StudyAreaNetwork.cut(network, null, Set.of(TransportMode.ship)));
		assertTrue(e.getMessage().contains("coordinate reference system"), e.getMessage());
	}

	/**
	 * A two-link city inside the study area, a two-link suburb outside it, one car link joining the two,
	 * and a cycleway inside that car may not use.
	 * <pre>
	 *   c1 == c2 -- s3 == s4        study area: x &lt; 1000
	 * </pre>
	 */
	private static Network cityWithASuburb() {

		Network network = NetworkUtils.createNetwork();
		Node c1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("c1"), new Coord(100, 100));
		Node c2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("c2"), new Coord(500, 100));
		Node s3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("s3"), new Coord(5000, 100));
		Node s4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("s4"), new Coord(5400, 100));

		car(network, "city-1-2", c1, c2, "primary");
		car(network, "city-2-1", c2, c1, "primary");
		car(network, "city-2-3", c2, s3, "motorway");
		car(network, "city-3-2", s3, c2, "motorway");
		car(network, "suburb-3-4", s3, s4, "residential");
		car(network, "suburb-4-3", s4, s3, "residential");

		Link cycleway = NetworkUtils.createAndAddLink(network, Id.createLinkId("cycleway-1-2"), c1, c2, 400, 5, 300, 1);
		cycleway.setAllowedModes(Set.of(TransportMode.bike));
		NetworkUtils.setType(cycleway, "cycleway");

		return network;
	}

	private static void car(Network network, String id, Node from, Node to, String highwayType) {
		Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId(id), from, to, 400, 14, 1800, 1);
		link.setAllowedModes(Set.of(TransportMode.car));
		NetworkUtils.setType(link, highwayType);
	}

	/**
	 * The study area: a square from (0,0) to (1000,1000), so the city is inside and the suburb is not.
	 */
	private ShpOptions.Index studyArea() {

		Path file = Path.of(utils.getOutputDirectory(), "study-area.shp");

		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder()
			.setName("study-area")
			.setCrs(MGC.getCRS(CRS))
			.addAttribute("name", String.class)
			.create();

		GeoFileWriter.writeGeometries(List.of(factory.createPolygon(new Coordinate[]{
			new Coordinate(0, 0), new Coordinate(1000, 0), new Coordinate(1000, 1000), new Coordinate(0, 1000)
		}, Map.of("name", "study-area"), "study-area")), file.toString());

		return new ShpOptions(file, CRS, null).createIndex(ShpOptions.SAME_CRS, "name");
	}
}
