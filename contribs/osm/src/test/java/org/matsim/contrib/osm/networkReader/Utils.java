package org.matsim.contrib.osm.networkReader;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.pbf.seq.PbfWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Utils {

	static final CoordinateTransformation transformation = new IdentityTransformation();
	static final String MOTORWAY = "motorway";
	static final String TERTIARY = "tertiary";
	private static final Logger log = LogManager.getLogger(Utils.class);


	static void writeOsmData(OsmData data, Path file) {
		try (OutputStream outputStream = Files.newOutputStream(file)) {
			PbfWriter writer = new PbfWriter(outputStream, true);
			for (OsmNode node : data.getNodes()) {
				writer.write(node);
			}
			for (OsmWay way : data.getWays()) {
				writer.write(way);
			}
			for (OsmRelation relation : data.getRelations()) {
				writer.write(relation);
			}
			writer.complete();
		} catch (IOException e) {
			log.error("could not write osm data");
			e.printStackTrace();
		}
	}

	static void writeOsmData(Collection<OsmNode> nodes, Collection<OsmWay> ways, Path file) {

		try (OutputStream outputStream = Files.newOutputStream(file)) {
			PbfWriter writer = new PbfWriter(outputStream, true);
			for (OsmNode node : nodes) {
				writer.write(node);
			}

			for (OsmWay way : ways) {
				writer.write(way);
			}
			writer.complete();
		} catch (IOException e) {
			log.error("could not write osm data");
			e.printStackTrace();
		}
	}

	static OsmData createSingleLink() {
		return createSingleLink(Collections.singletonList(new Tag(OsmTags.HIGHWAY, Utils.MOTORWAY)));
	}

	static OsmData createSingleLink(List<OsmTag> tags) {

		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 100, 100);
		Node node3 = new Node(3, 0, 200);
		Node node4 = new Node(4, 50, 150);
		TLongArrayList nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId(), node3.getId(), node4.getId()});
		Way way = new Way(1, nodeReference, tags);

		return new OsmData(Arrays.asList(node1, node2, node3, node4), Collections.singletonList(way), Collections.emptyList());
	}

	static OsmData createTwoIntersectingLinksWithDifferentLevels() {

		// way 1 will go diagonally from bottom left to upper right
		Node node1 = new Node(1, 0, 0);
		Node node2 = new Node(2, 25, 25);
		Node node3 = new Node(3, 50, 50);
		Node node4 = new Node(4, 75, 75);
		Node node5 = new Node (5, 100, 100);
		Node node6 = new Node(6, 125, 125);
		Node node7 = new Node(7, 150, 150);
		Node node8 = new Node(8, 175, 175);
		Node node9 = new Node(9, 200, 200);

		// way 2 will go diagonally from top left to bottom right
		Node node10 = new Node(10, 0, 200);
		Node node11 = new Node(11, 25, 175);
		Node node12 = new Node(12, 50, 150);
		Node node13 = new Node(13, 75, 125);
		// 100,100 will be node 5
		Node node14 = new Node(14, 125, 75);
		Node node15 = new Node(15, 150, 50);
		Node node16 = new Node(16, 175, 25);
		Node node17 = new Node(17, 200, 0);

		List<OsmNode> nodes = new ArrayList<>();
		nodes.add(node1);
		nodes.add(node2);
		nodes.add(node3);
		nodes.add(node4);
		nodes.add(node5);
		nodes.add(node6);
		nodes.add(node7);
		nodes.add(node8);
		nodes.add(node9);
		nodes.add(node10);
		nodes.add(node11);
		nodes.add(node12);
		nodes.add(node13);
		nodes.add(node14);
		nodes.add(node15);
		nodes.add(node16);
		nodes.add(node17);

		TLongArrayList nodeReferenceForWay1 = new TLongArrayList(new long[]{node1.getId(), node2.getId(), node3.getId(),
				node4.getId(), node5.getId(), node6.getId(), node7.getId(), node8.getId(), node9.getId()});

		TLongArrayList nodeReferenceForWay2 = new TLongArrayList(new long[]{node10.getId(), node11.getId(), node12.getId(),
				node13.getId(), node5.getId(), node14.getId(), node15.getId(), node16.getId(), node17.getId()});
		Way way1 = new Way(1, nodeReferenceForWay1, Collections.singletonList(new Tag(OsmTags.HIGHWAY, Utils.MOTORWAY)));
		Way way2 = new Way(2, nodeReferenceForWay2, Collections.singletonList(new Tag(OsmTags.HIGHWAY, Utils.TERTIARY)));
		return new OsmData(nodes, Arrays.asList(way1, way2));
	}

	static OsmData createGridWithDifferentLevels() {

		List<OsmNode> nodesList = Arrays.asList(
				new Node(1, 100, 0),
				new Node(2, 200, 0),
				new Node(3, 0, 100),
				new Node(4, 100, 100),
				new Node(5, 200, 100),
				new Node(6, 300, 100),
				new Node(7, 0, 200),
				new Node(8, 100, 200),
				new Node(9, 200, 200),
				new Node(10, 300, 200),
				new Node(11, 100, 300),
				new Node(12, 200, 300)
		);

		List<OsmWay> waysList = Arrays.asList(
				new Way(1, new TLongArrayList(new long[]{3, 4, 5, 6}), Collections.singletonList(new Tag("highway", MOTORWAY))),
				new Way(2, new TLongArrayList(new long[]{7, 8, 9, 10}), Collections.singletonList(new Tag("highway", MOTORWAY))),
				new Way(3, new TLongArrayList(new long[]{1, 4, 8, 11}), Collections.singletonList(new Tag("highway", TERTIARY))),
				new Way(4, new TLongArrayList(new long[]{2, 5, 9, 12}), Collections.singletonList(new Tag("highway", TERTIARY)))
		);

		return new OsmData(nodesList, waysList);
	}

	static void assertEquals(Network expected, Network actual) {
		// check that all element from expected result are in tested network
		for (Link link : expected.getLinks().values()) {
			Link testLink = actual.getLinks().get(link.getId());
			assertNotNull(testLink);
			testLinksAreEqual(link, testLink);
		}

		for (org.matsim.api.core.v01.network.Node node : expected.getNodes().values()) {
			org.matsim.api.core.v01.network.Node testNode = actual.getNodes().get(node.getId());
			assertNotNull(testNode);
			testNodesAreEqual(node, testNode);
		}

		// also check the other way around, to make sure there are no extra elements in the network
		for (Link link : actual.getLinks().values()) {
			Link expectedLink = expected.getLinks().get(link.getId());
			assertNotNull(expectedLink);
		}

		for (org.matsim.api.core.v01.network.Node node : actual.getNodes().values()) {
			org.matsim.api.core.v01.network.Node expectedNode = expected.getNodes().get(node.getId());
			assertNotNull(expectedNode);
		}
	}

	private static void testLinksAreEqual(Link expected, Link actual) {

		expected.getAllowedModes().forEach(mode -> assertTrue(actual.getAllowedModes().contains(mode)));
		Assertions.assertEquals(expected.getCapacity(), actual.getCapacity(), 0.001);
		Assertions.assertEquals(expected.getFlowCapacityPerSec(), actual.getFlowCapacityPerSec(), 0.001);
		Assertions.assertEquals(expected.getFreespeed(), actual.getFreespeed(), 0.001);
		Assertions.assertEquals(expected.getLength(), actual.getLength(), 0.001);
		Assertions.assertEquals(expected.getNumberOfLanes(), actual.getNumberOfLanes(), 0.001);
		Assertions.assertEquals(expected.getFromNode().getId(), actual.getFromNode().getId());
		Assertions.assertEquals(expected.getToNode().getId(), actual.getToNode().getId());
	}

	private static void testNodesAreEqual(org.matsim.api.core.v01.network.Node expected, org.matsim.api.core.v01.network.Node actual) {

		// test x and y separately, so that we can have a delta.
		// In java version >=11 Assert.assertEquals(expected.getCoord(), actual.getCoord()) also works
		// keep this in as long as we use java-8
		Assertions.assertEquals(expected.getCoord().getX(), actual.getCoord().getX(), 0.00000001);
		Assertions.assertEquals(expected.getCoord().getY(), actual.getCoord().getY(), 0.00000001);
		expected.getOutLinks().forEach((id, link) -> Assertions.assertEquals(link.getId(), actual.getOutLinks().get(id).getId()));
		expected.getInLinks().forEach((id, link) -> Assertions.assertEquals(link.getId(), actual.getInLinks().get(id).getId()));
	}

	static class OsmData {

		private final List<OsmNode> nodes;
		private final List<OsmWay> ways;
		private final List<OsmRelation> relations;

		public OsmData(List<OsmNode> nodes, List<OsmWay> ways) {
			this(nodes, ways, Collections.emptyList());
		}

		public OsmData(List<OsmNode> nodes, List<OsmWay> ways, List<OsmRelation> relations) {
			this.nodes = nodes;
			this.ways = ways;
			this.relations = relations;
		}

		public List<OsmNode> getNodes() {
			return nodes;
		}

		public List<OsmWay> getWays() {
			return ways;
		}

		public List<OsmRelation> getRelations() {
			return relations;
		}
	}
}
