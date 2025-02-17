package org.matsim.contrib.osm.networkReader;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Network reader which adds bicycle specific properties to the generated matsim-network. These include:
 * - {@link TransportMode#bike} is added as allowed transport mode
 * - Surface, smoothness are stored as link attributes
 * - Cycleway and restriction attributes are set as link attributes as well
 * - sets bicycleInfrastructureSpeedFactor to 0.5 on each link which has allowed mode {@link TransportMode#bike}
 * <p>
 * Additionally the osm-highway-tags cycleway, service, track, footway, pedestrian, path, steps are parsed from the
 * original osm-network.
 */
public final class OsmBicycleReader extends SupersonicOsmNetworkReader {

	private static final double BIKE_PCU = 0.25;
	private static final Set<String> bicycleNotAllowed = new HashSet<>(Arrays.asList(OsmTags.MOTORWAY, OsmTags.MOTORWAY_LINK,
			OsmTags.TRUNK, OsmTags.TRUNK_LINK));
	private static final Set<String> onlyBicycleAllowed = new HashSet<>(Arrays.asList(OsmTags.TRACK, OsmTags.CYCLEWAY, OsmTags.SERVICE,
//			OsmTags.FOOTWAY, OsmTags.PEDESTRIAN, OsmTags.PATH, OsmTags.STEPS // Steps should only be allowed if there are ramps.
			OsmTags.FOOTWAY, OsmTags.PEDESTRIAN, OsmTags.PATH, OsmTags.STEPS_RAMP, OsmTags.STEPS_RAMP_BICYCLE
	));
	private final SupersonicOsmNetworkReader.AfterLinkCreated afterLinkCreated;

	public OsmBicycleReader(OsmNetworkParser parser,
							Predicate<Long> preserveNodeWithId,
							BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy,
							AfterLinkCreated afterLinkCreated, double freeSpeedFactor, double adjustCapacityLength, boolean storeOriginalGeometry) {
		super(parser, preserveNodeWithId, includeLinkAtCoordWithHierarchy, (link, tags, direction) -> handleLink(link, tags, direction, afterLinkCreated), freeSpeedFactor, adjustCapacityLength, storeOriginalGeometry);
		this.afterLinkCreated = afterLinkCreated;
	}

	private static void handleLink(Link link, Map<String, String> tags, SupersonicOsmNetworkReader.Direction direction, AfterLinkCreated outfacingCallback) {

		String highwayType = tags.get(OsmTags.HIGHWAY);

		setAllowedModes(link, highwayType);
		setSurface(link, tags, highwayType);
		setSmoothness(link, tags);
		setCycleWay(link, tags);
		setRestrictions(link, tags);

		outfacingCallback.accept(link, tags, direction);
	}

	private static void setAllowedModes(Link link, String highwayType) {
		HashSet<String> allowedModes = new HashSet<>(link.getAllowedModes());
		if (!bicycleNotAllowed.contains(highwayType))
			allowedModes.add(TransportMode.bike);
		if (onlyBicycleAllowed.contains(highwayType))
			allowedModes.remove(TransportMode.car);
		link.setAllowedModes(allowedModes);
	}

	private static void setSurface(Link link, Map<String, String> tags, String highwayType) {
		if (tags.containsKey(OsmTags.SURFACE)) {
			link.getAttributes().putAttribute(OsmTags.SURFACE, tags.get(OsmTags.SURFACE));
		} else if (highwayType.equals(OsmTags.PRIMARY) || highwayType.equals(OsmTags.PRIMARY_LINK)
				|| highwayType.equals(OsmTags.SECONDARY) || highwayType.equals(OsmTags.SECONDARY_LINK)) {
			link.getAttributes().putAttribute(OsmTags.SURFACE, "asphalt");
		}
	}

	private static void setSmoothness(Link link, Map<String, String> tags) {
		if (tags.containsKey(OsmTags.SMOOTHNESS)) {
			link.getAttributes().putAttribute(OsmTags.SMOOTHNESS, tags.get(OsmTags.SMOOTHNESS));
		}
	}

	private static void setCycleWay(Link link, Map<String, String> tags) {
		if (tags.containsKey(OsmTags.CYCLEWAY))
			link.getAttributes().putAttribute(OsmTags.CYCLEWAY, tags.get(OsmTags.CYCLEWAY));
	}

	private static void setRestrictions(Link link, Map<String, String> tags) {
		if (tags.containsKey(OsmTags.BICYCLE))
			link.getAttributes().putAttribute(TransportMode.bike, tags.get(OsmTags.BICYCLE));
	}

	@Override
	Collection<Link> createLinks(WaySegment segment) {

		Collection<Link> links = super.createLinks(segment);

		if (links.size() == 1 && isReverseCycleWay(segment.getTags())) {
			Link link = links.iterator().next();
			Link reverseLink = createReverseBicycleLink(link);
			links.add(reverseLink);

			// test whether the reverse link is actually reverse relative to the segment
			Direction direction = reverseLink.getFromNode().getId().equals(Id.createNodeId(segment.getFromNode().getId())) ? Direction.Forward : Direction.Reverse;
			this.afterLinkCreated.accept(link, segment.getTags(), direction);
		}
		return links;
	}

	private Link createReverseBicycleLink(Link forwardLink) {

		String linkId = forwardLink.getId().toString() + "_bike-reverse";
		Link result = getNetworkFactory().createLink(Id.createLinkId(linkId), forwardLink.getToNode(), forwardLink.getFromNode());
		result.setAllowedModes(new HashSet<>(Collections.singletonList(TransportMode.bike)));

		result.setCapacity(1500 * BIKE_PCU);
		result.setLength(forwardLink.getLength());
		result.setFreespeed(30 / 3.6);
		result.setNumberOfLanes(1);
		for (Map.Entry<String, Object> attribute : forwardLink.getAttributes().getAsMap().entrySet()) {
			result.getAttributes().putAttribute(attribute.getKey(), attribute.getValue());
		}
		return result;
	}

	private boolean isReverseCycleWay(Map<String, String> tags) {
		if (tags.containsKey(OsmTags.ONEWAYBICYCLE) && tags.get(OsmTags.ONEWAYBICYCLE).equals("no")) return true;
		if (tags.containsKey(OsmTags.CYCLEWAY)) {
			String tag = tags.get(OsmTags.CYCLEWAY);
			return (tag.equals("opposite") || tag.equals("opposite_track") || tag.equals("opposite_lane"));
		}
		return false;
	}

	public static class Builder extends AbstractBuilder<OsmBicycleReader> {

		public Builder() {
			addOverridingLinkProperties(OsmTags.TRACK, new LinkProperties(9, 1, 30 / 3.6, 1500 * BIKE_PCU, false));
			addOverridingLinkProperties(OsmTags.CYCLEWAY, new LinkProperties(9, 1, 30 / 3.6, 1500 * BIKE_PCU, false));
			addOverridingLinkProperties(OsmTags.SERVICE, new LinkProperties(9, 1, 10 / 3.6, 100 * BIKE_PCU, false));
			addOverridingLinkProperties(OsmTags.FOOTWAY, new LinkProperties(10, 1, 10 / 3.6, 600 * BIKE_PCU, false));
			addOverridingLinkProperties(OsmTags.PEDESTRIAN, new LinkProperties(10, 1, 10 / 3.6, 600 * BIKE_PCU, false));
			addOverridingLinkProperties(OsmTags.PATH, new LinkProperties(10, 1, 20 / 3.6, 600 * BIKE_PCU, false));
			addOverridingLinkProperties(OsmTags.STEPS_RAMP, new LinkProperties(11, 1, 1 / 3.6, 50 * BIKE_PCU, false));
			addOverridingLinkProperties(OsmTags.STEPS_RAMP_BICYCLE, new LinkProperties(11, 1, 1 / 3.6, 50 * BIKE_PCU, false));
		}

		@Override
		OsmBicycleReader createInstance() {
			OsmNetworkParser parser = new OsmNetworkParser(coordinateTransformation, linkProperties, includeLinkAtCoordWithHierarchy, Executors.newWorkStealingPool());
			return new OsmBicycleReader(parser, preserveNodeWithId, includeLinkAtCoordWithHierarchy, afterLinkCreated, freeSpeedFactor, adjustCapacityLength, storeOriginalGeometry);
		}
	}
}
