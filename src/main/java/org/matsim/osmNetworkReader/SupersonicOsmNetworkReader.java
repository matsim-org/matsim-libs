package org.matsim.osmNetworkReader;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Log
public class SupersonicOsmNetworkReader {

	public static final String HIGHWAY = "highway";
    public static final String MAXSPEED = "maxspeed";

	private static final Set<String> reverseTags = new HashSet<>(Arrays.asList("-1", "reverse"));
	private static final Set<String> oneWayTags = new HashSet<>(Arrays.asList("yes", "true", "1"));
	private static final Set<String> notOneWayTags = new HashSet<>(Arrays.asList("no", "false", "0"));

	private final Map<String, LinkProperties> linkProperties = LinkProperties.createLinkProperties();
    private final BiPredicate<Coord, Integer> linkFilter;
    private final Predicate<Long> preserveNodeWithId;
    private final Consumer<Link> afterLinkCreated;
	private final CoordinateTransformation coordinateTransformation;

	@Getter
	private final Network network;


    @lombok.Builder(builderClassName = "Builder")
    private SupersonicOsmNetworkReader(Network network, CoordinateTransformation coordinateTransformation,
                                       BiPredicate<Coord, Integer> linkFilter, Predicate<Long> preserveNodeWithId,
                                       Consumer<Link> afterLinkCreated) {
        this.linkFilter = linkFilter == null ? (coord, level) -> true : linkFilter;
        this.afterLinkCreated = afterLinkCreated == null ? link -> {
        } : afterLinkCreated;
        this.preserveNodeWithId = preserveNodeWithId == null ? id -> false : preserveNodeWithId;

        if (network == null || coordinateTransformation == null) {
            throw new IllegalArgumentException("Target network and coordinate transformation are required parameters!");
        }
		this.network = network;
		this.coordinateTransformation = coordinateTransformation;
	}

	@SuppressWarnings("WeakerAccess")
    public void read(Path inputFile) {

        var nodesAndWays = parse(inputFile);
		log.info("starting convertion \uD83D\uDE80");
        convert(nodesAndWays.ways, nodesAndWays.nodes);
    }

	private static boolean isStreetOfInterest(OsmWay way, Map<String, LinkProperties> linkProperties) {
		for (int i = 0; i < way.getNumberOfTags(); i++) {
			String tag = way.getTag(i).getKey();
			String tagvalue = way.getTag(i).getValue();
			if (tag.equals(HIGHWAY) && linkProperties.containsKey(tagvalue)) return true;
		}
		return false;
	}

    private void convert(Collection<OsmWay> ways, Map<Long, LightOsmNode> nodes) {

        log.info("linkFilter for highways create a matsim network");
        ways.parallelStream()
				.filter(way -> isStreetOfInterest(way, linkProperties))
                .flatMap(way -> this.createWaySegments(nodes, way))
                .flatMap(this::createLinks)
				.forEach(this::addLinkToNetwork);

		log.info("done");
	}

    private NodesAndWays parse(Path inputFile) {

		log.info("start reading ways");

        try {
            var waysReader = new PbfReader(inputFile.toFile(), false);
			var waysHandler = new WayHandler(linkProperties);
            waysReader.setHandler(waysHandler);
            waysReader.read();

            log.info("finished reading ways.");
            log.info("Kept " + waysHandler.getWays().size() + "/" + waysHandler.counter + "ways");
            log.info("Marked " + waysHandler.getNodes().size() + " nodes to be kept");
            log.info("starting to read nodes");

            var nodesReader = new PbfReader(inputFile.toFile(), false);
            var nodesHandler = new NodesHandler(waysHandler.getNodes());
            nodesReader.setHandler(nodesHandler);
            nodesReader.read();

            log.info("finished reading nodes");

            return new NodesAndWays(nodesHandler.nodes, waysHandler.ways);
        } catch (FileNotFoundException | OsmInputException e) {
            throw new RuntimeException(e);
        }
    }

	private Stream<WaySegment> createWaySegments(Map<Long, LightOsmNode> nodes, OsmWay way) {

		final var tags = OsmModelUtil.getTagsAsMap(way);
		List<WaySegment> segments = new ArrayList<>();
		double segmentLength = 0;

		// set up first node for segment
		long fromNodeId = way.getNodeId(0);
		var fromNodeForSegmentWithoutTransform = nodes.get(fromNodeId);
		var fromNodeForSegment = new LightOsmNode(fromNodeForSegmentWithoutTransform.getId(), fromNodeForSegmentWithoutTransform.getNumberOfWays(),
				coordinateTransformation.transform(fromNodeForSegmentWithoutTransform.getCoord()));

		for (int i = 1, linkdIdPostfix = 1; i < way.getNumberOfNodes(); i++, linkdIdPostfix += 2) {

			// get the from and to nodes for a sub segment of the current way
			var fromOsmNode = nodes.get(fromNodeId);
			var toOsmNode = nodes.get(way.getNodeId(i));

			// add the distance between those nodes to the overal length of segment
			Coord fromCoord = coordinateTransformation.transform(fromOsmNode.getCoord());
			Coord toCoord = coordinateTransformation.transform(toOsmNode.getCoord());
			segmentLength += CoordUtils.calcEuclideanDistance(fromCoord, toCoord);

			if (fromNodeForSegment.getId() == toOsmNode.getId()) {
				log.info("Detected loop. Keeping all the nodes which are part of the way");
				var loopSegments = handleLoop(nodes, fromNodeForSegment, toOsmNode, way, i, linkdIdPostfix);
				segments.addAll(loopSegments);

				// set up next iteration
				linkdIdPostfix += loopSegments.size() * 2;
				segmentLength = 0;
				fromNodeForSegment = toOsmNode;

			} else if (toOsmNode.isUsedByMoreThanOneWay() || i == way.getNumberOfNodes() - 1 || this.preserveNodeWithId.test(toOsmNode.getId())) {
				// create way segments between intersections or between the first and last node of a way or between nodes, which are upposed to be preserved

				segments.add(new WaySegment(
						new LightOsmNode(fromNodeForSegment.getId(), fromNodeForSegment.getNumberOfWays(), fromNodeForSegment.getCoord()),
						new LightOsmNode(toOsmNode.getId(), toOsmNode.getNumberOfWays(), toCoord),
						segmentLength, tags, way.getId(),
						//if the way id is 1234 we will get a link id like 12340001, this is necessary because we need to generate unique
						// ids. The osm wiki says ways have no more than 2000 nodes which means that i will never be greater 1999.
						// we have to increase the appendix by two for each segment, to leave room for backwards links
						way.getId() * 10000 + linkdIdPostfix));

				//prepare for next segment
				segmentLength = 0;
				fromNodeForSegment = toOsmNode;
			}

			//prepare for next iteration
			fromNodeId = toOsmNode.getId();
		}
		return segments.stream();
	}

    private Collection<WaySegment> handleLoop(Map<Long, LightOsmNode> nodes, LightOsmNode fromNode, LightOsmNode toNode, OsmWay way, int toNodeIndex, int idPostfix) {

        List<WaySegment> result = new ArrayList<>();
        var toSegmentNode = toNode;
        // iterate backwards and keep all elements of the loop. Don't do thinning since this is an edge case
        for (int i = toNodeIndex - 1; i > 0; i--) {

            var fromId = way.getNodeId(i);


            var fromSegmentNode = nodes.get(fromId);
            Coord fromCoord = coordinateTransformation.transform(fromSegmentNode.getCoord());
            Coord toCoord = coordinateTransformation.transform(toSegmentNode.getCoord());

            result.add(new WaySegment(
                    new LightOsmNode(fromSegmentNode.getId(), fromSegmentNode.getNumberOfWays(), fromCoord),
                    new LightOsmNode(toSegmentNode.getId(), toSegmentNode.getNumberOfWays(), toCoord),
                    CoordUtils.calcEuclideanDistance(fromCoord, toCoord),
                    OsmModelUtil.getTagsAsMap(way),
                    way.getId(), way.getId() * 10000 + idPostfix)
            );

            if (fromId == fromNode.getId()) {
                // finish creating segments after creating the last one
                break;
            }
            toSegmentNode = fromSegmentNode;
            idPostfix += 2;
        }
        return result;
    }

    private Stream<Link> createLinks(WaySegment segment) {

        var highwayType = segment.getTags().get(HIGHWAY);
        var properties = linkProperties.get(highwayType);
        List<Link> result = new ArrayList<>();

        if (linkFilter.test(segment.getFromNode().getCoord(), properties.level) || linkFilter.test(segment.getToNode().getCoord(), properties.level)) {
            var fromNode = createNode(segment.getFromNode().getCoord(), segment.getFromNode().getId());
            var toNode = createNode(segment.getToNode().getCoord(), segment.getToNode().getId());

            if (!isOnewayReverse(segment.tags)) {
                Link forwardLink = createLink(fromNode, toNode, segment, false);
                result.add(forwardLink);
            }

            if (!isOneway(segment.getTags(), properties)) {
                Link reverseLink = createLink(toNode, fromNode, segment, true);
                result.add(reverseLink);
			}
		}
        return result.stream();
	}

	private Node createNode(Coord coord, long nodeId) {
		Id<Node> id = Id.createNodeId(nodeId);
		return network.getFactory().createNode(id, coord);
	}

    private Link createLink(Node fromNode, Node toNode, WaySegment segment, boolean isReverse) {

        String highwayType = segment.getTags().get(HIGHWAY);
		LinkProperties properties = linkProperties.get(highwayType);

        long linkId = isReverse ? segment.getSegmentId() + 1 : segment.getSegmentId();
		Link link = network.getFactory().createLink(Id.createLinkId(linkId), fromNode, toNode);
        link.setLength(segment.getLength());
        link.setFreespeed(getFreespeed(segment.getTags(), link.getLength(), properties));
        link.setNumberOfLanes(getNumberOfLanes(segment.getTags(), isReverse, properties));
        link.setCapacity(getLaneCapacity(link.getLength(), properties) * link.getNumberOfLanes());
        link.getAttributes().putAttribute(NetworkUtils.ORIGID, segment.getOriginalWayId());
		link.getAttributes().putAttribute(NetworkUtils.TYPE, highwayType);
        afterLinkCreated.accept(link);
		return link;
	}

	private boolean isOneway(Map<String, String> tags, LinkProperties properties) {

		if (tags.containsKey("oneway")) {
			String tag = tags.get("oneway");
			if (oneWayTags.contains(tag)) return true;
			if (reverseTags.contains(tag) || notOneWayTags.contains(tag)) return false;
		}

		// no oneway tag
		if ("roundabout".equals(tags.get("junction"))) return true;

		// just return the default for this type of link
		return properties.oneway;
	}

	private boolean isOnewayReverse(Map<String, String> tags) {

		if (tags.containsKey("oneway")) {
			String tag = tags.get("oneway");
			if (oneWayTags.contains(tag) || notOneWayTags.contains(tag)) return false;
			return reverseTags.contains(tag);
		}
		return false;
	}

    private double getFreespeed(Map<String, String> tags, double linkLength, LinkProperties properties) {
        if (tags.containsKey(MAXSPEED)) {
            double speed = parseSpeedTag(tags.get(MAXSPEED), properties);
			double urbanSpeedFactor = speed <= 51 / 3.6 ? 0.5 : 1.0; // assume for links with max speed lower than 51km/h to be in urban areas. Reduce speed to reflect traffic lights and suc
			return speed * urbanSpeedFactor;
		} else {
            return calculateSpeedIfNoSpeedTag(properties, linkLength);
		}
	}

	private double parseSpeedTag(String tag, LinkProperties properties) {

		try {
			if (tag.endsWith("mph"))
				return Double.parseDouble(tag.replace("mph", "").trim()) * 1.609344 / 3.6;
			else
				return Double.parseDouble(tag) / 3.6;
		} catch (NumberFormatException e) {
			//System.out.println("Could not parse maxspeed tag: " + tag + " ignoring it");
		}
		return properties.freespeed;
	}

    /*
     * For links with unknown max speed we assume that links with a length of less than 300m are urban links. For urban
     * links with a length of 0m the speed is 10km/h. For links with a length of 300m the speed is the default freespeed
     * property for that highway type. For links with a length between 0 and 300m the speed is interpolated linearly.
     *
     * All links longer than 300m the default freesped property is assumed
     */
    private double calculateSpeedIfNoSpeedTag(LinkProperties properties, double linkLength) {
        if (properties.level > LinkProperties.LEVEL_MOTORWAY && properties.level < LinkProperties.LEVEL_SMALLER_THAN_TERTIARY
                && linkLength < 300) {
            return ((10 + (properties.freespeed - 10) / 300 * linkLength) / 3.6);
        }
        return properties.freespeed;
    }

	private double getLaneCapacity(double linkLength, LinkProperties properties) {
		double capacityFactor = linkLength < 100 ? 2 : 1;
		return properties.laneCapacity * capacityFactor;
	}

    private double getNumberOfLanes(Map<String, String> tags, boolean isReverse, LinkProperties properties) {

        try {
            if (tags.containsKey("lanes")) {
                String directionKey = isReverse ? "lanes:backward" : "lanes:forward";
                if (tags.containsKey(directionKey)) {
                    double directionLanes = Double.parseDouble(tags.get(directionKey));
                    if (directionLanes > 0) return directionLanes;
                }
                // no forward lane tag, so use the regular lanes tag
                double lanes = Double.parseDouble(tags.get("lanes"));

                // lanes tag specifies lanes into both directions of a way, so cut it in half if it is not a oneway street
                if (!isOneway(tags, properties)) return lanes / 2;
                return lanes;
            }
        } catch (NumberFormatException e) {
            return properties.lanesPerDirection;
		}
		return properties.lanesPerDirection;
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

	@RequiredArgsConstructor
	public static class LinkProperties {

		public static final int LEVEL_MOTORWAY = 1;
		public static final int LEVEL_TRUNK = 2;
		public static final int LEVEL_PRIMARY = 3;
		public static final int LEVEL_SECONDARY = 4;
		public static final int LEVEL_TERTIARY = 5;
		public static final int LEVEL_SMALLER_THAN_TERTIARY = 6; // choose a better name

		final int level;
		final double lanesPerDirection;
		final double freespeed;
		final double freespeedFactor;
		final double laneCapacity;
		final boolean oneway;

		static LinkProperties createMotorway() {
			return new LinkProperties(LEVEL_MOTORWAY, 2, 120 / 3.6, 1.0, 2000, true);
		}

		static LinkProperties createMotorwayLink() {
			return new LinkProperties(LEVEL_MOTORWAY, 1, 80 / 3.6, 1, 1500, true);
		}

		static LinkProperties createTrunk() {
			return new LinkProperties(LEVEL_TRUNK, 1, 80 / 3.6, 1, 2000, false);
		}

		static LinkProperties createTrunkLink() {
			return new LinkProperties(LEVEL_TRUNK, 1, 50 / 3.6, 1, 1500, false);
		}

		static LinkProperties createPrimary() {
			return new LinkProperties(LEVEL_PRIMARY, 1, 80 / 3.6, 1, 1500, false);
		}

		static LinkProperties createPrimaryLink() {
			return new LinkProperties(LEVEL_PRIMARY, 1, 60 / 3.6, 1, 1500, false);
		}

		static LinkProperties createSecondary() {
			return new LinkProperties(LEVEL_SECONDARY, 1, 30 / 3.6, 1, 800, false);
		}

		static LinkProperties createSecondaryLink() {
			return new LinkProperties(LEVEL_SECONDARY, 1, 30 / 3.6, 1, 800, false);
		}

		static LinkProperties createTertiary() {
			return new LinkProperties(LEVEL_TERTIARY, 1, 25 / 3.6, 1, 600, false);
		}

		static LinkProperties createTertiaryLink() {
			return new LinkProperties(LEVEL_TERTIARY, 1, 25 / 3.6, 1, 600, false);
		}

		static LinkProperties createUnclassified() {
			return new LinkProperties(LEVEL_SMALLER_THAN_TERTIARY, 1, 15 / 3.6, 1, 600, false);
		}

		static LinkProperties createResidential() {
			return new LinkProperties(LEVEL_SMALLER_THAN_TERTIARY, 1, 15 / 3.6, 1, 600, false);
		}

		static LinkProperties createLivingStreet() {
			return new LinkProperties(LEVEL_SMALLER_THAN_TERTIARY, 1, 10 / 3.6, 1, 300, false);
		}

		static Map<String, LinkProperties> createLinkProperties() {
			Map<String, LinkProperties> result = new HashMap<>();
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

	@RequiredArgsConstructor
	@Getter
	private static class WayHandler implements OsmHandler {

		private final Set<OsmWay> ways = new HashSet<>();
		private final Map<Long, Integer> nodes = new HashMap<>();
		private final Map<String, LinkProperties> linkProperties;

		private int counter = 0;

		private static boolean isStreet(OsmWay way) {
			for (int i = 0; i < way.getNumberOfTags(); i++) {
				String tag = way.getTag(i).getKey();
				if (tag.equals(HIGHWAY)) return true;
			}
			return false;
		}

		@Override
		public void handle(OsmWay osmWay) {
			counter++;
			if (isStreetOfInterest(osmWay, linkProperties)) {
				ways.add(osmWay);
				for (int i = 0; i < osmWay.getNumberOfNodes(); i++) {
					nodes.merge(osmWay.getNodeId(i), 1, Integer::sum);
				}
			}
			if (counter % 100000 == 0) {
				log.info("Read: " + counter + " ways");
			}
		}

		@Override
		public void handle(OsmBounds osmBounds) {
		}

		@Override
		public void handle(OsmNode osmNode) {
		}

		@Override
		public void handle(OsmRelation osmRelation) {
		}

		@Override
		public void complete() {
		}
	}

    @RequiredArgsConstructor
    @Getter
    private static class LightOsmNode {

        private final long id;
        private final int numberOfWays;
        private final Coord coord;

        boolean isUsedByMoreThanOneWay() {
            return numberOfWays > 1;
        }
    }

	@RequiredArgsConstructor
	private static class NodesHandler implements OsmHandler {

        private final Map<Long, Integer> nodeIdsOfInterest;
        @Getter
        private final Map<Long, LightOsmNode> nodes = new HashMap<>();
        @Getter
        private int counter = 0;

		@Override
        public void handle(OsmNode osmNode) {

            counter++;

            if (nodeIdsOfInterest.containsKey(osmNode.getId())) {
                Coord coord = new Coord(osmNode.getLongitude(), osmNode.getLatitude());
                int numberOfWays = nodeIdsOfInterest.get(osmNode.getId());
                nodes.put(osmNode.getId(), new LightOsmNode(osmNode.getId(), numberOfWays, coord));
			}
            if (counter % 100000 == 0) {
                log.info("Read: " + counter + " nodes");
            }
		}

        @Override
        public void handle(OsmBounds osmBounds) {
        }

		@Override
        public void handle(OsmWay osmWay) {
        }

		@Override
        public void handle(OsmRelation osmRelation) {
        }

		@Override
        public void complete() {
        }
	}

	@RequiredArgsConstructor
	@Getter
	private static class NodesAndWays {
        private final Map<Long, LightOsmNode> nodes;
		private final Set<OsmWay> ways;
	}

    @RequiredArgsConstructor
    @Getter
    private static class WaySegment {
        private final LightOsmNode fromNode;
        private final LightOsmNode toNode;
        private final double length;
        private final Map<String, String> tags;
        private final long originalWayId;
        private final long segmentId;
    }
}
