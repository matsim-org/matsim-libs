package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmRelationMember;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BiPredicate;

class OsmSignalsParser extends OsmNetworkParser {

	private Map<Long, Set<ProcessedRelation>> nodeRestrictions = new ConcurrentHashMap<>();
	private Map<Long, String> signalizedNodes = new ConcurrentHashMap<>();

	public Map<Long, Set<ProcessedRelation>> getNodeRestrictions() {
		return nodeRestrictions;
	}

	public Map<Long, String> getSignalizedNodes() {
		return signalizedNodes;
	}

	OsmSignalsParser(CoordinateTransformation transformation, Map<String, LinkProperties> linkProperties, BiPredicate<Coord, Integer> linkFilter, ExecutorService executor) {
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
				.setWaysHandler(this::handleWay)
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

		// we only consider restrictions if they have from, to and via members set
		if (osmRelation.getNumberOfMembers() == 3) {

			Map<String, String> tags = OsmModelUtil.getTagsAsMap(osmRelation);

			// if the relation is of type restriction, we consider doing something with it.
			// also test, whether the restriction type is set, which has restriction as key.
			if (tags.containsKey(OsmTags.TYPE) && tags.get(OsmTags.TYPE).equals(OsmTags.RESTRICTION) && tags.containsKey(OsmTags.RESTRICTION)) {

				String restrictionTag = tags.get(OsmTags.RESTRICTION);
				ProcessedRelation.Type type = restrictionTag.startsWith("only") ? ProcessedRelation.Type.IMPERATIVE : ProcessedRelation.Type.PROHIBITIVE;

				OsmRelationMember via = null;
				OsmRelationMember from = null;
				OsmRelationMember to = null;

				for (int i = 0; i < osmRelation.getNumberOfMembers(); i++) {
					OsmRelationMember member = osmRelation.getMember(i);
					if (member.getRole().equals("via") && member.getType().equals(EntityType.Node)) {
						via = member;
					} else if (member.getRole().equals("from") && member.getType().equals(EntityType.Way)) {
						from = member;
					} else if (member.getRole().equals("to") && member.getType().equals(EntityType.Way)) {
						to = member;
					}
				}
				if (via != null && from != null && to != null) {
					nodeRestrictions.computeIfAbsent(via.getId(), id -> Collections.synchronizedSet(new HashSet<>()))
							.add(new ProcessedRelation(via.getId(), from.getId(), to.getId(), type));
				}
			}
		}
	}

	@Override
	void handleNode(OsmNode osmNode) {
		super.handleNode(osmNode);

		// if the node was added by parent parser, check whether we should do anything signals related
		if (super.nodes.containsKey(osmNode.getId())) {
			Map<String, String> tags = OsmModelUtil.getTagsAsMap(osmNode);

			if (tags.containsKey(OsmTags.HIGHWAY)) {
				String highwayTag = tags.get(OsmTags.HIGHWAY);

				if (highwayTag.equals(OsmTags.TRAFFIC_SINGALS) || highwayTag.equals(OsmTags.CROSSING)) {
					signalizedNodes.put(osmNode.getId(), highwayTag);
				}
			}
		}
	}
}
