package org.matsim.contrib.osm.networkReader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;

public final class OsmRailwayReader extends SupersonicOsmNetworkReader {

	public OsmRailwayReader(OsmNetworkParser parser,
							Predicate<Long> preserveNodeWithId,
							BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy,
							AfterLinkCreated afterLinkCreated, double freeSpeedFactor, double adjustCapacityLength, boolean storeOriginalGeometry) {
		
		super(parser, preserveNodeWithId, includeLinkAtCoordWithHierarchy, (link, tags, direction) -> handleLink(link, tags, direction, afterLinkCreated), freeSpeedFactor, adjustCapacityLength, storeOriginalGeometry);
	}

	private static void handleLink(Link link, Map<String, String> tags, SupersonicOsmNetworkReader.Direction direction, AfterLinkCreated outfacingCallback) {
		
		String railwayType = tags.get(OsmTags.RAILWAY);
		link.getAttributes().putAttribute("osm_way_type", "railway");
		link.getAttributes().putAttribute(NetworkUtils.TYPE, railwayType);
		
		for (String tag : tags.keySet()) {
			link.getAttributes().putAttribute(tag, tags.get(tag));
		}
		
		setAllowedModes(link, tags);
		
		outfacingCallback.accept(link, tags, direction);
	}

	private static void setAllowedModes(Link link, Map<String, String> tags) {
		if (tags.containsKey(OsmTags.SERVICE) && tags.get(OsmTags.SERVICE).equals("yard")) {
			Set<String> allowedModes = new HashSet<>();
			link.setAllowedModes(allowedModes);
		} else {
			// everything else should be allowed for rail
			Set<String> allowedModes = new HashSet<>();
			allowedModes.add("rail");
			link.setAllowedModes(allowedModes);
		}
	}

	@Override
	Collection<Link> createLinks(WaySegment segment) {
		Collection<Link> links = super.createLinks(segment);
		return links;
	}

	public static class Builder extends AbstractBuilder<OsmRailwayReader> {

		@Override
		OsmRailwayReader createInstance() {
			OsmRailwayParser parser = new OsmRailwayParser(coordinateTransformation, linkProperties, includeLinkAtCoordWithHierarchy, Executors.newWorkStealingPool());
			return new OsmRailwayReader(parser, preserveNodeWithId, includeLinkAtCoordWithHierarchy, afterLinkCreated, freeSpeedFactor, adjustCapacityLength, storeOriginalGeometry);
		}
	}
}
