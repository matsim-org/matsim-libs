package org.matsim.core.utils.io;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfReader;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class OsmNetworkReader {

	private final String HIGHWAY = "highway";
	private final Map<String, DefaultLinkProperties> linkProperties = DefaultLinkProperties.createLinkProperties();
	private final BiPredicate<Coord, Integer> filter;
	private final Network network;
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);

	OsmNetworkReader(Network network, BiPredicate<Coord, Integer> filter) {
		this.filter = filter;
		this.network = network;
	}

	@SuppressWarnings("WeakerAccess")
	public void read(Path inputFile) throws FileNotFoundException, OsmInputException {

		var handler = parse(inputFile);
		convert(handler.ways, handler.nodes);

	}

	private void convert(Collection<OsmWay> ways, Map<Long, OsmNode> nodes) {

		// This introduces a side effect, since the mark nodes method will mutate the cache's state. But I don't know
		// how to properly do this
		NodesCache cache = new NodesCache(nodes.keySet());

		var filteredWays = ways.parallelStream()
				.filter(way -> {

					var map = OsmModelUtil.getTagsAsMap(way);
					return map.containsKey(HIGHWAY) && linkProperties.containsKey(map.get(HIGHWAY));
				})
				.peek(way -> this.markNodes(cache, way))
				.collect(Collectors.toList());
		// we need to collect here to finish the counting of visited nodes before actually creating matsim links

		// convert ways to links
		filteredWays.parallelStream()
				.flatMap(way -> this.createNecessaryLinks(cache, nodes, way).stream())
				.forEach(this::addLinkToNetwork);
	}

	private ParsingHandler parse(Path inputFile) throws FileNotFoundException, OsmInputException {
		var reader = new PbfReader(inputFile.toFile(), true);
		var handler = new ParsingHandler();
		reader.setHandler(handler);
		reader.read();
		return handler;
	}

	private void markNodes(NodesCache cache, OsmWay way) {
		for (int i = 0; i < way.getNumberOfNodes(); i++) {
			long nodeId = way.getNodeId(i);
			cache.markNode(nodeId);
		}
	}

	private Collection<Link> createNecessaryLinks(NodesCache cache, Map<Long, OsmNode> nodes, OsmWay way) {
		long fromNodeId = way.getNodeId(0);

		List<Link> result = new ArrayList<>();
		for (int i = 1; i < way.getNumberOfNodes(); i++) {
			// take all nodes which are used by more than one way e.g. intersections. Definitely take the last node
			//TODO there is no loop detection yet
			if (cache.isNodeUsedByMoreThanOneWay(way.getNodeId(i)) || i == way.getNumberOfNodes() - 1) {
				// make a link from to this node

				OsmNode fromOsmNode = nodes.get(fromNodeId);
				OsmNode toOsmNode = nodes.get(way.getNodeId(i));
				Coord fromCoord = ct.transform(new Coord(fromOsmNode.getLongitude(), fromOsmNode.getLatitude()));
				Coord toCoord = ct.transform(new Coord(toOsmNode.getLongitude(), toOsmNode.getLatitude()));

				// we should have filtered for these before, so no testing whether they are present
				String highwayType = OsmModelUtil.getTagsAsMap(way).get(HIGHWAY);
				int level = linkProperties.get(highwayType).level;

				if (filter.test(fromCoord, level) || filter.test(toCoord, level)) {
					Node fromNode = createNode(fromCoord, fromOsmNode.getId());
					Node toNode = createNode(toCoord, toOsmNode.getId());
					//if the way id is 1234 we will get a link id like 12340001, this is necessary because we need to generate unique
					// ids. The osm wiki says ways have no more than 2000 nodes which means that i will never be creater 1999.
					Id<Link> id = Id.createLinkId(way.getId() * 10000 + i);
					Link link = network.getFactory().createLink(id, fromNode, toNode);
					link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
					result.add(link);
				}
				fromNodeId = toOsmNode.getId();
			}
		}
		return result;
	}

	private Node createNode(Coord coord, long nodeId) {
		Id<Node> id = Id.createNodeId(nodeId);
		return network.getFactory().createNode(id, coord);
	}

	private synchronized void addLinkToNetwork(Link link) {

		//we have to test for presence
		if (!network.getNodes().containsKey(link.getFromNode().getId())) {
			network.addNode(link.getFromNode());
		}
		if (!network.getNodes().containsKey(link.getToNode().getId())) {
			network.addNode(link.getToNode());
		}
		if (!network.getLinks().containsKey(link.getId())) {
			network.addLink(link);
		} else {
			throw new RuntimeException("ups");
		}
	}

	@FunctionalInterface
	public interface OsmFilter {

		boolean filter(Coord coord, int linkLevel);
	}

	private static class NodesCache {

		private ConcurrentMap<Long, Integer> nodes;

		NodesCache(Collection<Long> nodes) {
			this.nodes = nodes.parallelStream().collect(Collectors.toConcurrentMap(node -> node, node -> 0));
		}

		void markNode(long nodeId) {
			nodes.merge(nodeId, 1, Integer::sum);
		}

		int getCount(long nodeId) {
			return nodes.get(nodeId);
		}

		boolean isNodeUsedByMoreThanOneWay(long nodeId) {
			return nodes.get(nodeId) > 1;
		}
	}

	public static class DefaultLinkProperties {

		final double lanesPerDirection;
		final double freespeed;
		final double freespeedFactor;
		final double laneCapacity;
		final boolean oneway;
		final int level;

		DefaultLinkProperties(final int level, final double lanesPerDirection, final double freespeed, final double freespeedFactor, final double laneCapacity, final boolean oneway) {
			this.level = level;
			this.lanesPerDirection = lanesPerDirection;
			this.freespeed = freespeed;
			this.freespeedFactor = freespeedFactor;
			this.laneCapacity = laneCapacity;
			this.oneway = oneway;
		}

		static DefaultLinkProperties createMotorway() {
			return new DefaultLinkProperties(1, 2, 120 / 3.6, 1.0, 2000, true);
		}

		static DefaultLinkProperties createMotorwayLink() {
			return new DefaultLinkProperties(1, 1, 80 / 3.6, 1, 1500, true);
		}

		static DefaultLinkProperties createTrunk() {
			return new DefaultLinkProperties(2, 1, 80 / 3.6, 1, 2000, false);
		}

		static DefaultLinkProperties createTrunkLink() {
			return new DefaultLinkProperties(2, 1, 50 / 3.6, 1, 1500, false);
		}

		static DefaultLinkProperties createPrimary() {
			return new DefaultLinkProperties(3, 1, 80 / 3.6, 1, 1500, false);
		}

		static DefaultLinkProperties createPrimaryLink() {
			return new DefaultLinkProperties(3, 1, 60 / 3.6, 1, 1500, false);
		}

		static DefaultLinkProperties createSecondary() {
			return new DefaultLinkProperties(4, 1, 30 / 3.6, 1, 800, false);
		}

		static DefaultLinkProperties createSecondaryLink() {
			return new DefaultLinkProperties(4, 1, 30 / 3.6, 1, 800, false);
		}

		static DefaultLinkProperties createTertiary() {
			return new DefaultLinkProperties(5, 1, 25 / 3.6, 1, 600, false);
		}

		static DefaultLinkProperties createTertiaryLink() {
			return new DefaultLinkProperties(5, 1, 25 / 3.6, 1, 600, false);
		}

		static DefaultLinkProperties createUnclassified() {
			return new DefaultLinkProperties(6, 1, 15 / 3.6, 1, 600, false);
		}

		static DefaultLinkProperties createResidential() {
			return new DefaultLinkProperties(6, 1, 15 / 3.6, 1, 600, false);
		}

		static DefaultLinkProperties createLivingStreet() {
			return new DefaultLinkProperties(6, 1, 10 / 3.6, 1, 300, false);
		}

		static Map<String, DefaultLinkProperties> createLinkProperties() {
			Map<String, DefaultLinkProperties> result = new HashMap<>();
			result.put("motorway", createMotorway());
			result.put("motorway_link", createMotorwayLink());
			result.put("trunk", createTrunk());
			result.put("trunk_link", createTrunkLink());
			result.put("primary", createPrimary());
			result.put("primary_link", createPrimaryLink());
			result.put("secondary", createSecondary());
			result.put("secondary_link", createSecondaryLink());
			result.put("tertiary", createTertiary());
			result.put("tertiary_link", createTertiaryLink());
			result.put("unclassified", createUnclassified());
			result.put("residential", createResidential());
			result.put("living_street", createLivingStreet());
			return result;
		}
	}

	private static class ParsingHandler implements OsmHandler {

		private Map<Long, OsmNode> nodes = new HashMap<>();
		private Set<OsmWay> ways = new HashSet<>();

		Map<Long, OsmNode> getNodes() {
			return nodes;
		}

		Set<OsmWay> getWays() {
			return ways;
		}

		@Override
		public void handle(OsmBounds osmBounds) {
			//ignore
		}

		@Override
		public void handle(OsmNode osmNode) {
			nodes.put(osmNode.getId(), osmNode);
		}

		@Override
		public void handle(OsmWay osmWay) {
			ways.add(osmWay);
			if (osmWay.getNumberOfNodes() < 2) {
				System.out.println("less than 2");
			}
		}

		@Override
		public void handle(OsmRelation osmRelation) {
			//ignore
		}

		@Override
		public void complete() {
			//ignore
		}
	}
}
