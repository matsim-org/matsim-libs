package org.matsim.contrib.osm.networkReader;

import com.slimjars.dist.gnu.trove.list.TLongList;
import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Class for converting osm-networks into matsim-networks. This class uses the binary osm.pbf format as an input. Suitable
 * input files can be found at https://download.geofabrik.de
 * <p>
 * Examples on how to use the reader can be found in {@link org.matsim.contrib.osm.examples}
 * <p>
 * For the most common highway tags the {@link LinkProperties} class contains default properties for the
 * corresponding links in the matsim-network (e.g. speed, number of lanes). Those default properties may be overridden
 * with custom link properties using the {@link SupersonicOsmNetworkReader.Builder#addOverridingLinkProperties(String, LinkProperties)}
 * method of the Builder.
 */
public class SupersonicOsmNetworkReader {

    private static final Logger log = Logger.getLogger(SupersonicOsmNetworkReader.class);

    private static final Set<String> reverseTags = new HashSet<>(Arrays.asList("-1", "reverse"));
    private static final Set<String> oneWayTags = new HashSet<>(Arrays.asList("yes", "true", "1"));
    private static final Set<String> notOneWayTags = new HashSet<>(Arrays.asList("no", "false", "0"));

    private final Predicate<Long> preserveNodeWithId;
    private final AfterLinkCreated afterLinkCreated;
    private final double freeSpeedFactor;
    private final double adjustCapacityLength;
    private final boolean storeOriginalGeometry;
    private final BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy;
    final OsmNetworkParser parser;

    private Network network;
    private Map<Long, ProcessedOsmNode> nodes;
    private Map<Long, ProcessedOsmWay> ways;

    /**
     * Provide this getter for inheriting classes
     */
    NetworkFactory getNetworkFactory() {
        return network.getFactory();
    }

    SupersonicOsmNetworkReader(OsmNetworkParser parser,
                               Predicate<Long> preserveNodeWithId,
                               BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy,
                               AfterLinkCreated afterLinkCreated,
                               double freeSpeedFactor, double adjustCapacityLength, boolean storeOriginalGeometry) {
        this.parser = parser;
        this.preserveNodeWithId = preserveNodeWithId;
        this.includeLinkAtCoordWithHierarchy = includeLinkAtCoordWithHierarchy;
        this.afterLinkCreated = afterLinkCreated;
        this.freeSpeedFactor = freeSpeedFactor;
        this.adjustCapacityLength = adjustCapacityLength;
        this.storeOriginalGeometry = storeOriginalGeometry;
    }

    /**
     * Creates a function to adjust the freespeed for urban links.
     * @see LinkProperties#DEFAULT_FREESPEED_FACTOR
     *
     * @apiNote Can be used as example, but no public access currently
     */
    static AfterLinkCreated adjustFreespeed(final double factor) {
        return (link, osmTags, direction) -> {
            if (osmTags.containsKey(OsmTags.MAXSPEED)) {
                if (link.getFreespeed() < 51 / 3.6)
                    link.setFreespeed(link.getFreespeed() * factor);
            }
        };
    }

    public Network read(String inputFile) {
        return read(Paths.get(inputFile));
    }

    public Network read(Path inputFile) {

        parser.parse(inputFile);

        // set up state for convertion of parsed osm data
        this.network = NetworkUtils.createNetwork();
        this.ways = parser.getWays();
        this.nodes = parser.getNodes();

        log.info("starting conversion \uD83D\uDE80");
        convert();

        log.info("finished conversion");
        return network;
    }

    private void convert() {

        ways.values().parallelStream()
                .flatMap(way -> this.createWaySegments(way).stream())
                .flatMap(segment -> this.createLinks(segment).stream())
                .forEach(this::addLinkToNetwork);
    }

    Collection<WaySegment> createWaySegments(ProcessedOsmWay way) {

        List<WaySegment> segments = new ArrayList<>();
        double segmentLength = 0;
        int fromNodeForSegmentIndex = 0;

        for (int i = 1; i < way.getNodeIds().size(); i++) {

            // get the first node of the segment
            ProcessedOsmNode fromNodeForSegment = nodes.get(way.getNodeIds().get(fromNodeForSegmentIndex));
            // get the from and to nodes for a sub segment of the current way
            ProcessedOsmNode fromOsmNode = nodes.get(way.getNodeIds().get(i - 1));
            ProcessedOsmNode toOsmNode = nodes.get(way.getNodeIds().get(i));

            // add the distance between those nodes to the overall length of segment
            segmentLength += CoordUtils.calcEuclideanDistance(fromOsmNode.getCoord(), toOsmNode.getCoord());

            if (isLoop(fromNodeForSegment, toOsmNode)) {
                Collection<WaySegment> loopSegments = handleLoop(nodes, fromNodeForSegment, way, i);
                segments.addAll(loopSegments);

                segmentLength = 0;
                fromNodeForSegmentIndex = i;
            }
            // if we have an intersection or the end of the way
            else if (isCreateSegment(fromNodeForSegment, toOsmNode, way)) {

                // get all the node ids for the way segment. TLongList.subList(fromIndex, toIndex) has toIndex exclusive, therefore i + 1
                var nodesForSegment = way.getNodeIds().subList(fromNodeForSegmentIndex, i + 1);
                var waySegment = new WaySegment(
                        fromNodeForSegment, toOsmNode, nodesForSegment,
                        segmentLength,
                        way.getLinkProperties(), way.getTags(), way.getId(),
                        //if the way id is 1234 we will get a link id like 12340001, this is necessary because we need to generate unique
                        // ids. The osm wiki says ways have no more than 2000 nodes which means that i will never be greater 1999.
                        way.getId() * 10000 + i - 1
                );
                segments.add(waySegment);

                segmentLength = 0;
                fromNodeForSegmentIndex = i;
            }
        }
        return segments;
    }

    private boolean isCreateSegment(ProcessedOsmNode from, ProcessedOsmNode to, ProcessedOsmWay way) {

        // if the user wants to have this node, then he should get it no matter what the other conditions are.
        if (preserveNodeWithId.test(to.getId())) return true;

        return (to.isIntersection() || to.getId() == way.getEndNodeId())
                && (to.isWayReferenced(way.getId()) || from.isWayReferenced(way.getId()));
    }

    private boolean isLoop(ProcessedOsmNode fromNode, ProcessedOsmNode toNode) {
        return fromNode.getId() == toNode.getId();
    }

    private Collection<WaySegment> handleLoop(Map<Long, ProcessedOsmNode> nodes, ProcessedOsmNode node, ProcessedOsmWay way, int toNodeIndex) {

        // we need an extra test whether the loop is within the link filter
        if (!includeLinkAtCoordWithHierarchy.test(node.getCoord(), way.getLinkProperties().hierarchyLevel))
            return Collections.emptyList();

        List<WaySegment> result = new ArrayList<>();
        ProcessedOsmNode toSegmentNode = node;

        // iterate backwards and keep all elements of the loop. Don't do thinning since this is an edge case
        for (int i = toNodeIndex - 1; i > 0; i--) {

            long fromId = way.getNodeIds().get(i);
            ProcessedOsmNode fromSegmentNode = nodes.get(fromId);

            result.add(new WaySegment(
                    fromSegmentNode, toSegmentNode, new TLongArrayList(new long[] { fromSegmentNode.getId(), toSegmentNode.getId()}),
                    CoordUtils.calcEuclideanDistance(fromSegmentNode.getCoord(), toSegmentNode.getCoord()),
                    way.getLinkProperties(),
                    way.getTags(),
                    way.getId(), way.getId() * 10000 + i));

            if (fromId == node.getId()) {
                // finish creating segments after creating the last one
                break;
            }
            toSegmentNode = fromSegmentNode;
        }
        return result;
    }

    Collection<Link> createLinks(WaySegment segment) {

        LinkProperties properties = segment.getLinkProperties();
        List<Link> result = new ArrayList<>();

        Node fromNode = createNode(segment.getFromNode().getCoord(), segment.getFromNode().getId());
        Node toNode = createNode(segment.getToNode().getCoord(), segment.getToNode().getId());

        if (!isOnewayReverse(segment.tags)) {
            Link forwardLink = createLink(fromNode, toNode, segment, Direction.Forward);
            result.add(forwardLink);
        }

        if (!isOneway(segment.getTags(), properties)) {
            Link reverseLink = createLink(toNode, fromNode, segment, Direction.Reverse);
            result.add(reverseLink);
        }

        return result;
    }

    private Node createNode(Coord coord, long nodeId) {
        Id<Node> id = Id.createNodeId(nodeId);
        return network.getFactory().createNode(id, coord);
    }

    Link createLink(Node fromNode, Node toNode, WaySegment segment, Direction direction) {

        String highwayType = segment.getTags().get(OsmTags.HIGHWAY);
        LinkProperties properties = segment.getLinkProperties();

        String linkId = direction == Direction.Forward ? segment.getSegmentId() + "f" : segment.getSegmentId() + "r";
        Link link = getNetworkFactory().createLink(Id.createLinkId(linkId), fromNode, toNode);
        link.setLength(segment.getLength());
        link.setFreespeed(getFreespeed(segment.getTags(), link.getLength(), properties));
        link.setNumberOfLanes(getNumberOfLanes(segment.getTags(), direction, properties));
        link.setCapacity(LinkProperties.getLaneCapacity(link.getLength(), properties, adjustCapacityLength) * link.getNumberOfLanes());
        link.getAttributes().putAttribute(NetworkUtils.ORIGID, segment.getOriginalWayId());
        link.getAttributes().putAttribute(NetworkUtils.TYPE, highwayType);
        if (storeOriginalGeometry)
            link.getAttributes().putAttribute(NetworkUtils.ORIG_GEOM, getOriginalGeometry(segment.getNodesForSegment(), direction));
        afterLinkCreated.accept(link, segment.getTags(), direction);
        return link;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isOneway(Map<String, String> tags, LinkProperties properties) {

        if (tags.containsKey(OsmTags.ONEWAY)) {
            String tag = tags.get(OsmTags.ONEWAY);
            if (oneWayTags.contains(tag)) return true;
            if (reverseTags.contains(tag) || notOneWayTags.contains(tag)) return false;
        }

        // no oneway tag
        if (OsmTags.ROUNDABOUT.equals(tags.get(OsmTags.JUNCTION))) return true;

        // just return the default for this type of link
        return properties.oneway;
    }

    boolean isOnewayReverse(Map<String, String> tags) {

        if (tags.containsKey(OsmTags.ONEWAY)) {
            String tag = tags.get(OsmTags.ONEWAY);
            if (oneWayTags.contains(tag) || notOneWayTags.contains(tag)) return false;
            return reverseTags.contains(tag);
        }
        return false;
    }

    private double getFreespeed(Map<String, String> tags, double linkLength, LinkProperties properties) {
        if (tags.containsKey(OsmTags.MAXSPEED)) {
            double maxSpeed = parseSpeedTag(tags.get(OsmTags.MAXSPEED), properties);
            return LinkProperties.calculateSpeedIfSpeedTag(maxSpeed, freeSpeedFactor);
        } else {
            return LinkProperties.calculateSpeedIfNoSpeedTag(linkLength, properties);
        }
    }

    private double parseSpeedTag(String tag, LinkProperties properties) {

        try {
            if (tag.endsWith(OsmTags.MPH))
                return Double.parseDouble(tag.replace(OsmTags.MPH, "").trim()) * 1.609344 / 3.6;
            else
                return Double.parseDouble(tag) / 3.6;
        } catch (NumberFormatException e) {
            //System.out.println("Could not parse maxspeed tag: " + tag + " ignoring it");
        }
        return properties.freespeed;
    }

    private double getNumberOfLanes(Map<String, String> tags, Direction direction, LinkProperties properties) {

        try {
            if (tags.containsKey(OsmTags.LANES)) {
                String directionKey = direction == Direction.Reverse ? OsmTags.LANES_BACKWARD : OsmTags.LANES_FORWARD;
                if (tags.containsKey(directionKey)) {
                    double directionLanes = Double.parseDouble(tags.get(directionKey));
                    if (directionLanes > 0) return directionLanes;
                }
                // no forward lane tag, so use the regular lanes tag
                double lanes = Double.parseDouble(tags.get(OsmTags.LANES));

                // lanes tag specifies lanes into both directions of a way, so cut it in half if it is not a oneway street
                if (!isOneway(tags, properties)) return lanes / 2;
                return lanes;
            }
        } catch (NumberFormatException e) {
            return properties.lanesPerDirection;
        }
        return properties.lanesPerDirection;
    }

    private String getOriginalGeometry(TLongList nodeIds, Direction direction) {

        var stringBuilder = new StringBuilder();

        // the first and last nodes are the from- and to node of a link. We don't have to include them here
        // if forward link, put nodes in the same order as original osm way, otherwise the other way around
        var startIndex = direction == Direction.Forward ? 1 : nodeIds.size() - 2;
        var endIndex = direction == Direction.Forward ? nodeIds.size() - 1 : 0;
        var incrementBy = direction == Direction.Forward ? 1 : -1;

        for (var i = startIndex; i != endIndex; i += incrementBy) {
            var id = nodeIds.get(i);
            var node = nodes.get(id);
            var coord = node.getCoord();
            stringBuilder.append(node.getId());
            stringBuilder.append(",");
            stringBuilder.append(coord.getX());
            stringBuilder.append(',');
            stringBuilder.append(coord.getY());
            stringBuilder.append(' ');
        }

        return stringBuilder.toString();
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
            log.error("Link id: " + link.getId() + " was already present. This should not happen");
            log.error("The link associated with this id: " + link);
            throw new RuntimeException("Link id: " + link.getId() + " was already present!");
        }
    }

    public enum Direction {Forward, Reverse}

    @FunctionalInterface
    public interface AfterLinkCreated {

        void accept(Link link, Map<String, String> osmTags, Direction direction);

        /**
         * Executes function {@code after}, after this function has been executed.
         * @see java.util.function.Consumer#andThen(Consumer)
         */
        default AfterLinkCreated andThen(AfterLinkCreated after) {
            Objects.requireNonNull(after);
            return (link, osmTags, direction) -> {
                accept(link, osmTags, direction);
                after.accept(link, osmTags, direction);
            };
        }

    }

    public static abstract class AbstractBuilder<T> {

        Map<String, LinkProperties> linkProperties = LinkProperties.createLinkProperties();
        BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy = (coord, level) -> true;
        Predicate<Long> preserveNodeWithId = id -> false;
        AfterLinkCreated afterLinkCreated = (link, tags, isReverse) -> { };
        CoordinateTransformation coordinateTransformation;
        double freeSpeedFactor = LinkProperties.DEFAULT_FREESPEED_FACTOR;
        double adjustCapacityLength = LinkProperties.DEFAULT_ADJUST_CAPACITY_LENGTH;
        boolean storeOriginalGeometry = false;

        /**
         * Replace all Link-Properties at once. Link properties describe how an osm-highway-tag is translated into a
         * matsim-link. E.g. which freespeed is set on the link, whether it is one-way, etc.
         * <p>
         * The reader only reads osm-ways which have a corresponding entry in the linkProperties map.
         *
         * @param linkProperties The link properties to be included in the matsim-network.
         */
        public AbstractBuilder<T> setLinkProperties(ConcurrentMap<String, LinkProperties> linkProperties) {
            this.linkProperties = linkProperties;
            return this;
        }

        /**
         * The reader has a default set of osm-highway-tags which are parsed. The following are included by default:
         * motorway, motorway_link, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link,
         * unclassified, residential, living_street
         * <p>
         * By invoking this method one can override the assigned {@link LinkProperties} by supplying the desired highway
         * tag with a new {@link LinkProperties} object.
         *
         * @param highwayType the highway type which receives new properties
         * @param properties  the properties the corresponding matsim-links will receive
         */
        public AbstractBuilder<T> addOverridingLinkProperties(String highwayType, LinkProperties properties) {
            linkProperties.put(highwayType, properties);
            return this;
        }

        /**
         * This sets a filter to in- or exclude links depending on its hierarchy level and location. The filter is invoked
         * for each to and from node of a link.
         * <p>
         * The hierarchy levels reach from {@link LinkProperties#LEVEL_MOTORWAY} = 1 to {@link LinkProperties#LEVEL_LIVING_STREET} = 8
         * <p>
         * Note: The supplied function is invoked concurrently
         *
         * @param includeLinkAtCoordWithHierarchy Bi-Predicate which takes a node's coordinate and its hierarchy level
         */
        public AbstractBuilder<T> setIncludeLinkAtCoordWithHierarchy(BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy) {
            this.includeLinkAtCoordWithHierarchy = includeLinkAtCoordWithHierarchy;
            return this;
        }

        /**
         * This sets a filter to prevent certain nodes from being removed.
         * <p>
         * The reader tries to simplify the network as much as possible. If it is essential to keep certain nodes of the
         * original osm-network within the output matsim-network for e.g. counts the filter can prevent the removal of
         * the node with the supplied id
         * <p>
         * Setting the filter to '() -> true' will omit network simplification entirely and is equivalent to the 'keepPath'
         * option of the previous OsmNetworkReader
         * <p>
         * Note: The supplied function is invoked concurrently
         *
         * @param preserveNodeWithId Predicate which returns true if the node corresponding to the supplied id must not
         *                           be removed.
         */
        public AbstractBuilder<T> setPreserveNodeWithId(Predicate<Long> preserveNodeWithId) {
            this.preserveNodeWithId = preserveNodeWithId;
            return this;
        }

        /**
         * This sets a hook to alter a link right before it is inserted into the result-network.
         * <p>
         * Note: The supplied function is invoked concurrently
         *
         * @param afterLinkCreated Basically a tri-consumer which accepts the created link, the original osm-tags
         *                         as a map and the direction enum which describes whether it is the forward or backward
         *                         link for an osm-way
         */
        public AbstractBuilder<T> setAfterLinkCreated(AfterLinkCreated afterLinkCreated) {
            this.afterLinkCreated = afterLinkCreated;
            return this;
        }

        /**
         * This sets whether the factor, with which speed will be adjusted for urban links.
         *
         * @see LinkProperties#DEFAULT_FREESPEED_FACTOR
         */
        public AbstractBuilder<T> setFreeSpeedFactor(double freeSpeedFactor) {
            this.freeSpeedFactor = freeSpeedFactor;
            return this;
        }

        /**
         * Sets the threshold in meter under which the lane capacity will be adjusted.
         * @see LinkProperties#DEFAULT_ADJUST_CAPACITY_LENGTH
         */
        public AbstractBuilder<T> setAdjustCapacityLength(double adjustCapacityLength) {
            this.adjustCapacityLength = adjustCapacityLength;
            return this;
        }

        /**
         * Coordinate transformation to transform spherical-osm-coordinates into euclidean-coordinates suited for matsim
         * simulations
         *
         * @param coordinateTransformation The supplied transformation should have {@link org.matsim.core.utils.geometry.transformations.TransformationFactory#WGS84} as
         *                                 input coordinate-system. And something like "EPSG:25832" as output sytem
         */
        public AbstractBuilder<T> setCoordinateTransformation(CoordinateTransformation coordinateTransformation) {
            this.coordinateTransformation = coordinateTransformation;
            return this;
        }

        /**
         * Sets wheter original node-ids and coordinates are stored as link attribute. Attribute key is
         * {@link org.matsim.core.network.NetworkUtils#ORIG_GEOM}
         * A collection of nodes can be fetched with {@link org.matsim.core.network.NetworkUtils#getOriginalGeometry(Link)}
         * @param value flag if set to true, the geometry will be stored. default is false
         */
        public AbstractBuilder<T> setStoreOriginalGeometry(boolean value) {
            this.storeOriginalGeometry = value;
            return this;
        }

        /**
         * Builds a reader
         */
        public T build() {
            return createInstance();
        }

        abstract T createInstance();
    }

    public static class Builder extends AbstractBuilder<SupersonicOsmNetworkReader> {

        SupersonicOsmNetworkReader createInstance() {

            if (coordinateTransformation == null) {
                throw new IllegalArgumentException("Target coordinate transformation is required parameter!");
            }

            OsmNetworkParser parser = new OsmNetworkParser(
                    coordinateTransformation, linkProperties, includeLinkAtCoordWithHierarchy,
                    Executors.newWorkStealingPool());

            return new SupersonicOsmNetworkReader(
                    parser, preserveNodeWithId,
                    includeLinkAtCoordWithHierarchy,
                    afterLinkCreated, freeSpeedFactor, adjustCapacityLength, storeOriginalGeometry
            );
        }
    }

    static class WaySegment {
        private final ProcessedOsmNode fromNode;
        private final ProcessedOsmNode toNode;
        private final double length;
        private final LinkProperties linkProperties;
        private final Map<String, String> tags;
        private final long originalWayId;
        private final long segmentId;
        private final TLongList nodesForSegment;

        public WaySegment(ProcessedOsmNode fromNode, ProcessedOsmNode toNode, TLongList nodesForSegment, double length, LinkProperties linkProperties, Map<String, String> tags, long originalWayId, long segmentId) {
            this.fromNode = fromNode;
            this.toNode = toNode;
            this.length = length;
            this.linkProperties = linkProperties;
            this.tags = tags;
            this.originalWayId = originalWayId;
            this.segmentId = segmentId;
            this.nodesForSegment = nodesForSegment;
        }

        public ProcessedOsmNode getFromNode() {
            return fromNode;
        }

        public ProcessedOsmNode getToNode() {
            return toNode;
        }

        public double getLength() {
            return length;
        }

        public LinkProperties getLinkProperties() {
            return linkProperties;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        public long getOriginalWayId() {
            return originalWayId;
        }

        public long getSegmentId() {
            return segmentId;
        }

        public TLongList getNodesForSegment() {
            return nodesForSegment;
        }
    }
}
