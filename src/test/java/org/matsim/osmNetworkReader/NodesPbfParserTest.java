package org.matsim.osmNetworkReader;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.AtlantisToWGS84;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class NodesPbfParserTest {

	private static ExecutorService executor = Executors.newSingleThreadExecutor();

	@Test
	public void parse_singleLink() throws IOException {

		var singleLink = Utils.createSingleLink();

		Path file = Paths.get("parallel-nodes-parser-single-link.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		var waysParser = new WaysPbfParser(executor, LinkProperties.createLinkProperties());

		try (var fileInputStream = new FileInputStream(file.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
			waysParser.parse(input);
		}

		var nodesParser = new NodesPbfParser(executor,
				(coord, level) -> true,
				waysParser.getNodes(),
				Utils.transformation
		);

		try (var fileInputStream = new FileInputStream(file.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
			nodesParser.parse(input);
		}

		var nodes = nodesParser.getNodes();

		// we want all three nodes of the way
		assertEquals(3, nodes.size());

		// all nodes should have a reference count of one
		assertEquals(1, nodes.get(singleLink.getNodes().get(0).getId()).getFilteredReferencedWays().size());
		assertEquals(1, nodes.get(singleLink.getNodes().get(1).getId()).getFilteredReferencedWays().size());
		assertEquals(1, nodes.get(singleLink.getNodes().get(2).getId()).getFilteredReferencedWays().size());
	}

	@Test
	public void parse_singleLink_withTransformation() throws IOException {

		final var transformation = new AtlantisToWGS84();

		var singleLink = Utils.createSingleLink();

		Path file = Paths.get("parallel-nodes-parser-single-link-with-transformation.pbf");
		Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

		var waysParser = new WaysPbfParser(executor, LinkProperties.createLinkProperties());

		try (var fileInputStream = new FileInputStream(file.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
			waysParser.parse(input);
		}

		var nodesParser = new NodesPbfParser(executor,
				(coord, level) -> true,
				waysParser.getNodes(),
				transformation
		);

		try (var fileInputStream = new FileInputStream(file.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
			nodesParser.parse(input);
		}

		var nodes = nodesParser.getNodes();

		// we want all three nodes of the way
		assertEquals(3, nodes.size());

		var node1 = singleLink.getNodes().get(0);
		var transformedNode1 = transformation.transform(new Coord(node1.getLatitude(), node1.getLongitude()));
		assertEquals(nodes.get(node1.getId()).getCoord(), transformedNode1);
	}

	@Test
	public void test_twoIntersectingWays() throws IOException {

		var twoIntersectingLinks = Utils.createTwoIntersectingLinksWithDifferentLevels();

		Path file = Paths.get("parallel-nodes-parser-intersecting-links.pbf");
		Utils.writeOsmData(twoIntersectingLinks.getNodes(), twoIntersectingLinks.getWays(), file);

		var waysParser = new WaysPbfParser(executor, LinkProperties.createLinkProperties());

		try (var fileInputStream = new FileInputStream(file.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
			waysParser.parse(input);
		}

		var nodesParser = new NodesPbfParser(executor,
				(coord, level) -> true,
				waysParser.getNodes(),
				Utils.transformation
		);

		try (var fileInputStream = new FileInputStream(file.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
			nodesParser.parse(input);
		}

		var nodes = nodesParser.getNodes();

		// we should have five nodes
		assertEquals(5, nodes.size());

		// all nodes should have a reference count of 1 exept node with id 2 is referenced by both links
		for (ProcessedOsmNode node : nodes.values()) {
			if (node.getId() == 2) assertEquals(2, node.getFilteredReferencedWays().size());
			else assertEquals(1, node.getFilteredReferencedWays().size());
		}
	}

	@Test
	public void parse_intersectingLinksOneDoesNotMatchFilter() throws IOException {

		var twoIntersectingLinks = Utils.createTwoIntersectingLinksWithDifferentLevels();

		Path file = Paths.get("parallel-nodes-parser-intersecting-links.pbf");
		Utils.writeOsmData(twoIntersectingLinks.getNodes(), twoIntersectingLinks.getWays(), file);

		var waysParser = new WaysPbfParser(executor, LinkProperties.createLinkProperties());

		try (var fileInputStream = new FileInputStream(file.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
			waysParser.parse(input);
		}

		var nodesParser = new NodesPbfParser(executor,
				(coord, level) -> level == LinkProperties.LEVEL_MOTORWAY, // just take the motorway link
				waysParser.getNodes(),
				Utils.transformation
		);

		try (var fileInputStream = new FileInputStream(file.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
			nodesParser.parse(input);
		}

		var nodes = nodesParser.getNodes();

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
}