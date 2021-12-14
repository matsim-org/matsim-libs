package org.matsim.contrib.osm.networkReader;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.SearchableNetwork;
import org.matsim.lanes.Lanes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class OsmSignalsReader extends SupersonicOsmNetworkReader {

	private final OsmSignalsParser signalsParser;

	OsmSignalsReader(OsmSignalsParser parser,
					 Predicate<Long> preserveNodeWithId,
					 BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy,
					 AfterLinkCreated afterLinkCreated, double freeSpeedFactor, double adjustCapacityLength, boolean storeOriginalGeometry) {

		super(parser,
				id -> keepCrossing(id, parser),
				includeLinkAtCoordWithHierarchy,
				(link, tags, direction) -> doAfterLinkCreated(link, tags, direction, afterLinkCreated), freeSpeedFactor, adjustCapacityLength, storeOriginalGeometry);
		signalsParser = parser;

		throw new RuntimeException("The signals reader is not yet implemented. Please don't use it yet.");
	}

	private static boolean keepCrossing(long id, OsmSignalsParser parser) {
		return parser.getSignalizedNodes().containsKey(id);
	}

	private static void doAfterLinkCreated(Link link, Map<String, String> tags, Direction direction, AfterLinkCreated outfacingCallback) {

		String turnLanes = tags.get("turn:lanes");
		// and the other tags
		//create lanes stack and safe in lanes stack field
		link.setNumberOfLanes(5000); // some reasonable number

		// it is important to call this callback, so that the outfacing api is as expected
		outfacingCallback.accept(link, tags, direction);
	}

	public Lanes getLanes() {
		throw new RuntimeException("not implemented");
	}

	public Map<Long, String> getSignalizedNodes() {
		throw new RuntimeException(" not yet implemented");
	}

	@Override
	public Network read(Path inputFile) {
		Network network = super.read(inputFile);

		SearchableNetwork searchable = (SearchableNetwork) network;

		// 1. Find complex intersections

		for (Node node : network.getNodes().values()) {

			Coord coord = node.getCoord();
			Collection<Node> out = new ArrayList<>();
			searchable.getNodeQuadTree().getRectangle(coord.getX() - 30, coord.getY() - 30, coord.getX() + 30, coord.getY() + 30, out);

			// create a new intersection node, which is going to be the single intersection
			// memorize all nodes which are part of that intersection, so they can be removed later

		}

		// 2. figure out vectors of turn restrictions with vectors

		// 3. merge intersection

		// 4. Fill toLinks of lanes
		for (Set<ProcessedRelation> value : signalsParser.getNodeRestrictions().values()) {
			for (ProcessedRelation processedRelation : value) {
				// do something relations related
				// find lanes stack in the yet to implement lanesStack property
			}
		}

		return network;
	}

	@Override
	Collection<WaySegment> createWaySegments(ProcessedOsmWay way) {


		Collection<WaySegment> segments = super.createWaySegments(way);

		for (int i = 0; i < way.getNodeIds().size(); i++) {

			long node = way.getNodeIds().get(i);

			if (signalsParser.getSignalizedNodes().containsKey(node)) {
				// wenn nicht am ende, dann evtl. verschieben
			}

		}

		return segments;
	}

	public static class Builder extends AbstractBuilder<OsmSignalsReader> {

		@Override
		OsmSignalsReader createInstance() {

			OsmSignalsParser parser = new OsmSignalsParser(coordinateTransformation,
					linkProperties, includeLinkAtCoordWithHierarchy, Executors.newWorkStealingPool());

			return new OsmSignalsReader(parser, preserveNodeWithId, includeLinkAtCoordWithHierarchy, afterLinkCreated, freeSpeedFactor, adjustCapacityLength, storeOriginalGeometry);
		}
	}
}
