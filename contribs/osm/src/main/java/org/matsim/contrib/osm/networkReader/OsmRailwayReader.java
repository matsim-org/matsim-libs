package org.matsim.contrib.osm.networkReader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;

public final class OsmRailwayReader extends SupersonicOsmNetworkReader {
	
	private static final double FALLBACK_MAX_SPEED = 100.;

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
		
		setAttributes(link, tags);
		setAllowedModes(link, tags);
		setSpeed(link, tags);
		
		outfacingCallback.accept(link, tags, direction);
	}

	private static void setSpeed(Link link, Map<String, String> tags) {
		
	}

	private static void setAttributes(Link link, Map<String, String> tags) {
		
		if (tags.containsKey(OsmTags.USAGE)) {
			link.getAttributes().putAttribute(OsmTags.USAGE, tags.get(OsmTags.USAGE));
		}
		
		if (tags.containsKey(OsmTags.GAUGE)) {
			link.getAttributes().putAttribute(OsmTags.GAUGE, tags.get(OsmTags.GAUGE));
		}
		
		if (tags.containsKey(OsmTags.ELECTRIFIED)) {
			link.getAttributes().putAttribute(OsmTags.ELECTRIFIED, tags.get(OsmTags.ELECTRIFIED));
		}
		
		if (tags.containsKey(OsmTags.MAXSPEED)) {
			link.getAttributes().putAttribute(OsmTags.MAXSPEED, tags.get(OsmTags.MAXSPEED));
		} else {
			link.getAttributes().putAttribute(OsmTags.MAXSPEED, FALLBACK_MAX_SPEED);
		}
		
		if (tags.containsKey(OsmTags.ETCS)) {
			link.getAttributes().putAttribute(OsmTags.ETCS, tags.get(OsmTags.ETCS));
		}
		
		if (tags.containsKey(OsmTags.OPERATOR)) {
			link.getAttributes().putAttribute(OsmTags.OPERATOR, tags.get(OsmTags.OPERATOR));
		}
		
	}

	private static void setAllowedModes(Link link, Map<String, String> tags) {
		Set<String> allowedModes = new HashSet<>();
		allowedModes.add(tags.get(OsmTags.RAILWAY));
		link.setAllowedModes(allowedModes);
	}

	@Override
	Collection<Link> createLinks(WaySegment segment) {
		Collection<Link> links = super.createLinks(segment);
		return links;
	}

	public static class Builder extends AbstractBuilder<OsmRailwayReader> {

		public Builder() {
			ConcurrentMap<String, LinkProperties> linkProperties = new ConcurrentHashMap<>();
			linkProperties.put(OsmTags.RAIL, new LinkProperties(1, 1, 30., 1000., false));
			linkProperties.put(OsmTags.NARROW_GAUGE, new LinkProperties(1, 1, 30., 1000., false));
			linkProperties.put(OsmTags.LIGHT_RAIL, new LinkProperties(1, 1, 30., 1000., false));
			linkProperties.put(OsmTags.MONORAIL, new LinkProperties(1, 1, 30., 1000., false));
			linkProperties.put(OsmTags.FUNICULAR, new LinkProperties(1, 1, 30., 1000., false));
			linkProperties.put(OsmTags.SUBWAY, new LinkProperties(1, 1, 30., 1000., false));
			linkProperties.put(OsmTags.TRAM, new LinkProperties(1, 1, 30., 1000., false));
			
			setLinkProperties(linkProperties);
		}
		
		@Override
		OsmRailwayReader createInstance() {
			OsmNetworkParser parser = new OsmNetworkParser(coordinateTransformation, linkProperties, includeLinkAtCoordWithHierarchy, Executors.newWorkStealingPool(), OsmTags.RAILWAY);
			return new OsmRailwayReader(parser, preserveNodeWithId, includeLinkAtCoordWithHierarchy, afterLinkCreated, freeSpeedFactor, adjustCapacityLength, storeOriginalGeometry);
		}
	}
}
