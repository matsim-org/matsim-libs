package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

class OsmNetworkParser {

	private static final Logger log = LogManager.getLogger(OsmNetworkParser.class);
	private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.UK);

	private final CoordinateTransformation transformation;
	private final Map<String, LinkProperties> linkProperties;
	private final BiPredicate<Coord, Integer> linkFilter;
	final ExecutorService executor;
	Map<Long, ProcessedOsmWay> ways;
	Map<Long, ProcessedOsmNode> nodes;
	Map<Long, List<ProcessedOsmWay>> nodeReferences;
	private final String wayType;

	/**
	 * The default constructor for roads (OSM highway tag)
	 * 
	 * @param transformation
	 * @param linkProperties
	 * @param linkFilter
	 * @param executor
	 */
	OsmNetworkParser(CoordinateTransformation transformation, Map<String, LinkProperties> linkProperties, BiPredicate<Coord, Integer> linkFilter, ExecutorService executor) {
		this.transformation = transformation;
		this.linkProperties = linkProperties;
		this.linkFilter = linkFilter;
		this.executor = executor;
		this.wayType = OsmTags.HIGHWAY;
	}
	
	/**
	 * A more flexible constructor which allows to pass a different way type, e.g. railway
	 * 
	 * @param transformation
	 * @param linkProperties
	 * @param linkFilter
	 * @param executor
	 * @param wayType
	 */
	OsmNetworkParser(CoordinateTransformation transformation, Map<String, LinkProperties> linkProperties, BiPredicate<Coord, Integer> linkFilter, ExecutorService executor, String wayType) {
		this.transformation = transformation;
		this.linkProperties = linkProperties;
		this.linkFilter = linkFilter;
		this.executor = executor;
		this.wayType = wayType;
	}

	public Map<Long, ProcessedOsmWay> getWays() {
		return ways;
	}

	public Map<Long, ProcessedOsmNode> getNodes() {
		return nodes;
	}

	void parse(Path inputFile) {

		// make sure we have empty collections
		ways = new ConcurrentHashMap<>();
		nodes = new ConcurrentHashMap<>();
		nodeReferences = new ConcurrentHashMap<>();

		new PbfParser.Builder()
				.setWaysHandler(this::handleWay)
				.setExecutor(executor)
				.build()
				.parse(inputFile);

		log.info("Finished reading ways");
		log.info("Starting to read nodes");

		new PbfParser.Builder()
				.setNodeHandler(this::handleNode)
				.setExecutor(executor)
				.build()
				.parse(inputFile);

		log.info("finished reading nodes");
	}

	void handleNode(OsmNode osmNode) {

		if (nodeReferences.containsKey(osmNode.getId())) {

			List<ProcessedOsmWay> waysThatReferenceNode = nodeReferences.get(osmNode.getId());
			Coord transformedCoord = transformation.transform(new Coord(osmNode.getLongitude(), osmNode.getLatitude()));

			// 'testWhetherReferencingLinksAreInFilter' may be expensive because it might include a test whether the
			// supplied coordinate is within a shape. Therefore we want to test as few cases as possible. Two cases are tested anyway:
			//   1. if more than one way references this node, this node is an intersection and a possible end node for a
			//      matsim link.
			//   2. if this node is either the start- or end node of a referencing way which makes it a candidate for a node
			//      in a matsim link as well
			//
			// if a way has both ends outside the filter and no intersections within the filter it will not be included
			// in the final network. I think this is unlikely in real world scenarios, so I think we can live with this
			// to achieve faster execution
			List<ProcessedOsmWay> filteredReferencingLinks;
			if (waysThatReferenceNode.size() > 1 || isEndNodeOfReferencingLink(osmNode, waysThatReferenceNode.get(0)))
				filteredReferencingLinks = testWhetherReferencingLinksAreInFilter(transformedCoord, waysThatReferenceNode);
			else
				filteredReferencingLinks = Collections.emptyList();

			ProcessedOsmNode result = new ProcessedOsmNode(osmNode.getId(), filteredReferencingLinks, transformedCoord);
			this.nodes.put(result.getId(), result);

			if (nodes.size() % 100000 == 0) {
				log.info("Added " + numberFormat.format(nodes.size()) + " nodes");
			}
		}
	}

	void handleWay(OsmWay osmWay) {

		Map<String, String> tags = OsmModelUtil.getTagsAsMap(osmWay);

		if (isStreetOfInterest(tags)) {
			LinkProperties linkProperty = linkProperties.get(tags.get(wayType));
			ProcessedOsmWay processedWay = ProcessedOsmWay.create(osmWay, tags, linkProperty);
			ways.put(osmWay.getId(), processedWay);

			// keep track of which node is referenced by which way
			for (int i = 0; i < osmWay.getNumberOfNodes(); i++) {

				long nodeId = osmWay.getNodeId(i);
				nodeReferences.computeIfAbsent(nodeId, id -> Collections.synchronizedList(new ArrayList<>()))
						.add(processedWay);
			}

			if (ways.size() % 10000 == 0) {
				log.info("Added " + numberFormat.format(ways.size()) + " ways");
			}
		}
	}

	private boolean isStreetOfInterest(Map<String, String> tags) {
		return tags.containsKey(wayType) && linkProperties.containsKey(tags.get(wayType));
	}

	private boolean isEndNodeOfReferencingLink(OsmNode node, ProcessedOsmWay processedOsmWay) {
		return processedOsmWay.getEndNodeId() == node.getId() || processedOsmWay.getStartNode() == node.getId();
	}

	private List<ProcessedOsmWay> testWhetherReferencingLinksAreInFilter(Coord coord, List<ProcessedOsmWay> waysThatReferenceNode) {

		return waysThatReferenceNode.stream()
				.filter(way -> linkFilter.test(coord, way.getLinkProperties().hierarchyLevel))
				.collect(Collectors.toList());
	}
}
