package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import org.apache.log4j.Logger;
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

	private static final Logger log = Logger.getLogger(OsmNetworkParser.class);
	private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.UK);

	private final CoordinateTransformation transformation;
	private final Map<String, LinkProperties> linkProperties;
	private final BiPredicate<Coord, Integer> linkFilter;
	final ExecutorService executor;
	Map<Long, ProcessedOsmWay> ways;
	Map<Long, ProcessedOsmNode> nodes;
	Map<Long, List<ProcessedOsmWay>> nodeReferences;

	OsmNetworkParser(CoordinateTransformation transformation, Map<String, LinkProperties> linkProperties, BiPredicate<Coord, Integer> linkFilter, ExecutorService executor) {
		this.transformation = transformation;
		this.linkProperties = linkProperties;
		this.linkFilter = linkFilter;
		this.executor = executor;
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

			if (osmNode.getId() == 51414415 || osmNode.getId() == 51414416) {
				log.info("stop it");
			}

			List<ProcessedOsmWay> waysThatReferenceNode = nodeReferences.get(osmNode.getId());
			Coord transformedCoord = transformation.transform(new Coord(osmNode.getLongitude(), osmNode.getLatitude()));

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
			LinkProperties linkProperty = linkProperties.get(tags.get(OsmTags.HIGHWAY));
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
		return tags.containsKey(OsmTags.HIGHWAY) && linkProperties.containsKey(tags.get(OsmTags.HIGHWAY));
	}

	private boolean isEndNodeOfReferencingLink(OsmNode node, ProcessedOsmWay processedOsmWay) {
		return processedOsmWay.getEndNodeId() == node.getId() || processedOsmWay.getStartNode() == node.getId();
	}

	private List<ProcessedOsmWay> testWhetherReferencingLinksAreInFilter(Coord coord, List<ProcessedOsmWay> waysThatReferenceNode) {

		return waysThatReferenceNode.stream()
				.filter(way -> linkFilter.test(coord, way.getLinkProperties().hierachyLevel))
				.collect(Collectors.toList());
	}
}
