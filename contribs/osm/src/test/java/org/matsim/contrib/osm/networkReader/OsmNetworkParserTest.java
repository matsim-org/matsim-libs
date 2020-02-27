package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.AtlantisToWGS84;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OsmNetworkParserTest {

	private static ExecutorService executor = Executors.newSingleThreadExecutor();
	private static final CoordinateTransformation transformation = new IdentityTransformation();

	@Rule
	public MatsimTestUtils matsimUtils = new MatsimTestUtils();

	@Test
	public void parse_singleLink() {

		Utils.OsmData singleLink = Utils.createSingleLink();
		Path file = Paths.get(matsimUtils.getOutputDirectory(), "parser_single-link.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		OsmNetworkParser parser = new OsmNetworkParser(transformation, LinkProperties.createLinkProperties(), (coord, id) -> true, executor);
		parser.parse(file);

		Map<Long, ProcessedOsmWay> ways = parser.getWays();
		assertEquals(singleLink.getWays().size(), ways.size());

		// get the first way
		OsmWay osmWay = singleLink.getWays().get(0);
		assertTrue(ways.containsKey(osmWay.getId()));
		ProcessedOsmWay processedOsmWay = ways.get(osmWay.getId());

		assertEquals(osmWay.getId(), processedOsmWay.getId());
		assertEquals(1, processedOsmWay.getStartNode());
		assertEquals(3, processedOsmWay.getEndNodeId());
		assertTrue(processedOsmWay.getTags().containsKey(OsmTags.HIGHWAY));
		assertEquals(OsmTags.MOTORWAY, processedOsmWay.getTags().get(OsmTags.HIGHWAY));

		// get the nodes
		assertEquals(singleLink.getNodes().size(), parser.getNodes().size());
		assertEquals(1, parser.getNodes().get(1L).getFilteredReferencedWays().size());
		assertEquals(0, parser.getNodes().get(2L).getFilteredReferencedWays().size());
		assertEquals(1, parser.getNodes().get(3L).getFilteredReferencedWays().size());
	}

	@Test
	public void parse_twoIntersectingWays() {

		Utils.OsmData twoIntersectingLinks = Utils.createTwoIntersectingLinksWithDifferentLevels();
		Path file = Paths.get(matsimUtils.getOutputDirectory(), "parser_two-intersecting-ways.pbf");
		Utils.writeOsmData(twoIntersectingLinks.getNodes(), twoIntersectingLinks.getWays(), file);

		OsmNetworkParser parser = new OsmNetworkParser(transformation, LinkProperties.createLinkProperties(), (coord, id) -> true, executor);
		parser.parse(file);

		Map<Long, ProcessedOsmWay> ways = parser.getWays();
		assertEquals(2, ways.size());

		Map<Long, ProcessedOsmNode> nodes = parser.getNodes();
		assertEquals(5, nodes.size());

		assertEquals(2, nodes.get(2L).getFilteredReferencedWays().size());
		assertEquals(1, nodes.get(1L).getFilteredReferencedWays().size());
		assertEquals(1, nodes.get(1L).getFilteredReferencedWays().size());
		assertEquals(1, nodes.get(1L).getFilteredReferencedWays().size());
		assertEquals(1, nodes.get(1L).getFilteredReferencedWays().size());
	}

	@Test
	public void parse_intersectingLinksOneDoesNotMatchFilter() {

		Utils.OsmData twoIntersectingLinks = Utils.createTwoIntersectingLinksWithDifferentLevels();

		Path file = Paths.get(matsimUtils.getOutputDirectory(), "parser_intersecting-links-with-filtering.pbf");
		Utils.writeOsmData(twoIntersectingLinks.getNodes(), twoIntersectingLinks.getWays(), file);

		OsmNetworkParser parser = new OsmNetworkParser(transformation, LinkProperties.createLinkProperties(),
				(coord, level) -> level == LinkProperties.LEVEL_MOTORWAY, // just take the motorway link,
				executor);
		parser.parse(file);

		Map<Long, ProcessedOsmNode> nodes = parser.getNodes();

		// we expect all nodes to be stored
		assertEquals(5, nodes.size());

		// all nodes should be referenced only by the motorway
		for (ProcessedOsmNode node : nodes.values()) {
			if (node.getId() == 4 || node.getId() == 5) {
				assertEquals(0, node.getFilteredReferencedWays().size());
			} else {
				assertEquals(1, node.getFilteredReferencedWays().size());
				assertEquals(twoIntersectingLinks.getWays().get(0).getId(), node.getFilteredReferencedWays().get(0).getId());
			}
		}
	}

	@Test
	public void parse_singleLink_withTransformation() {

		final CoordinateTransformation atlantis = new AtlantisToWGS84();

		Utils.OsmData singleLink = Utils.createSingleLink();

		Path file = Paths.get(matsimUtils.getOutputDirectory(), "parser_single-link-with-transformation.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		OsmNetworkParser parser = new OsmNetworkParser(atlantis, LinkProperties.createLinkProperties(), (coord, id) -> true, executor);
		parser.parse(file);

		Map<Long, ProcessedOsmNode> nodes = parser.getNodes();

		// we want all three nodes of the way
		assertEquals(3, nodes.size());

		OsmNode node1 = singleLink.getNodes().get(0);
		Coord transformedNode1 = atlantis.transform(new Coord(node1.getLatitude(), node1.getLongitude()));
		assertEquals(nodes.get(node1.getId()).getCoord(), transformedNode1);
	}
}
