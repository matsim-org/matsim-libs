package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmRelationMember;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BiPredicate;

class OsmSignalsParser extends OsmNetworkParser {

	private Logger log = Logger.getLogger(OsmSignalsParser.class);

	OsmSignalsParser(CoordinateTransformation transformation, ConcurrentMap<String, LinkProperties> linkProperties, BiPredicate<Coord, Integer> linkFilter, ExecutorService executor) {
		super(transformation, linkProperties, linkFilter, executor);
	}

	@Override
	void parse(Path inputFile) {

		// make sure we have empty collections
		ways = new ConcurrentHashMap<>();
		nodes = new ConcurrentHashMap<>();
		nodeReferences = new ConcurrentHashMap<>();

		// parse ways and relations first
		new PbfParser.Builder()
				.setWaysHandler(super::handleWay)
				.setRelationHandler(this::handleRelation)
				.setExecutor(executor)
				.build()
				.parse(inputFile);

		// then parse necessary nodes
		new PbfParser.Builder()
				.setNodeHandler(this::handleNode)
				.setExecutor(executor)
				.build()
				.parse(inputFile);

	}

	private void handleRelation(OsmRelation osmRelation) {

		Map<String, String> tags = OsmModelUtil.getTagsAsMap(osmRelation);
		if (tags.containsKey("type") && tags.get("type").equals("restriction")) {
			if (tags.containsKey("restriction") && tags.get("restriction").startsWith("no")) {
				log.info(tags.get("restriction") + " is a no restriction");
			} else if (tags.containsKey("restriction") && tags.get("restriction").startsWith("only")) {
				log.info(tags.get("restriction") + " should be an only restriction");
			}

			List<OsmRelationMember> members = OsmModelUtil.membersAsList(osmRelation);
		}


	}

	@Override
	void handleNode(OsmNode osmNode) {
		super.handleNode(osmNode);

		Map<String, String> tags = OsmModelUtil.getTagsAsMap(osmNode);

		if (tags.containsKey("highway") && tags.get("highway").equals("traffic_signals")) {
			log.info("a traffic signals node");
		}
		if (tags.containsKey("highway") && tags.get("highway").equals("crossing")) {
			log.info("crossing");
		}

	}
}
