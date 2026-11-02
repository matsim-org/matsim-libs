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
import org.matsim.core.network.NetworkUtils;
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
