package org.matsim.contrib.osm.networkReader;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class SupersonicBicycleOsmNetworkReader {


	public static final String BICYCLE_INFRASTRUCTURE_SPEED_FACTOR = "bicycleInfrastructureSpeedFactor";
	private static final double BIKE_PCU = 0.25;
	private Set<String> bicycleNotAllowed = new HashSet<>(Arrays.asList(OsmTags.MOTORWAY, OsmTags.MOTORWAY_LINK,
			OsmTags.TRUNK, OsmTags.TRUNK_LINK));
	private Set<String> onlyBicycleAllowed = new HashSet<>(Arrays.asList(OsmTags.TRACK, OsmTags.CYCLEWAY, OsmTags.SERVICE,
			OsmTags.FOOTWAY, OsmTags.PEDESTRIAN, OsmTags.PATH, OsmTags.STEPS));

	private CoordinateTransformation coordinateTransformation;
	private BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy;
	private Predicate<Long> preserveNodeWithId;
	private SupersonicOsmNetworkReader.AfterLinkCreated afterLinkCreated;

	private ConcurrentMap<String, Id<Link>> linksWhichNeedBackwardBicycleDirection = new ConcurrentHashMap<>();

	public SupersonicBicycleOsmNetworkReader(CoordinateTransformation coordinateTransformation, BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy, Predicate<Long> preserveNodeWithId, SupersonicOsmNetworkReader.AfterLinkCreated afterLinkCreated) {
		this.coordinateTransformation = coordinateTransformation;
		this.includeLinkAtCoordWithHierarchy = includeLinkAtCoordWithHierarchy;
		this.preserveNodeWithId = preserveNodeWithId;
		this.afterLinkCreated = afterLinkCreated;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Network read(Path inputFile) {

		return SupersonicOsmNetworkReader.builder()
				.coordinateTransformation(this.coordinateTransformation)
				.addOverridingLinkProperties(OsmTags.TRACK, new LinkProperties(9, 1, 30 / 3.6, 1500 * BIKE_PCU, false))
				.addOverridingLinkProperties(OsmTags.CYCLEWAY, new LinkProperties(9, 1, 30 / 3.6, 1500 * BIKE_PCU, false))
				.addOverridingLinkProperties(OsmTags.SERVICE, new LinkProperties(9, 1, 10 / 3.6, 100 * BIKE_PCU, false))
				.addOverridingLinkProperties(OsmTags.FOOTWAY, new LinkProperties(10, 1, 10 / 3.6, 600 * BIKE_PCU, false))
				.addOverridingLinkProperties(OsmTags.PEDESTRIAN, new LinkProperties(10, 1, 10 / 3.6, 600 * BIKE_PCU, false))
				.addOverridingLinkProperties(OsmTags.PATH, new LinkProperties(10, 1, 20 / 3.6, 600 * BIKE_PCU, false))
				.addOverridingLinkProperties(OsmTags.STEPS, new LinkProperties(11, 1, 1 / 3.6, 50 * BIKE_PCU, false))
				.includeLinkAtCoordWithHierarchy(this.includeLinkAtCoordWithHierarchy)
				.preserveNodeWithId(this.preserveNodeWithId)
				.afterLinkCreated(this::handleLink)
				.build()
				.read(inputFile);
	}

	private void handleLink(Link link, Map<String, String> tags, boolean isReverse) {

		String highwayType = tags.get(OsmTags.HIGHWAY);

		setAllowedModes(link, highwayType);
		setSurface(link, tags, highwayType);
		setSmoothness(link, tags);
		setCycleWay(link, tags);
		setRestrictions(link, tags);

		// do infrastructure factor
		link.getAttributes().putAttribute(BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 0.5);

		//TODO add reverse direction for bicylces if street is only one way. Not sure how to fit that into the model of the reader
		collectReverseDirectionForBicycle(link, tags, isReverse);

		afterLinkCreated.accept(link, tags, isReverse);
	}

	private void setAllowedModes(Link link, String highwayType) {
		HashSet<String> allowedModes = new HashSet<>(link.getAllowedModes());
		if (!bicycleNotAllowed.contains(highwayType))
			allowedModes.add(TransportMode.bike);
		if (onlyBicycleAllowed.contains(highwayType))
			allowedModes.remove(TransportMode.car);
		link.setAllowedModes(allowedModes);
	}

	private void setSurface(Link link, Map<String, String> tags, String highwayType) {
		if (tags.containsKey(OsmTags.SURFACE)) {
			link.getAttributes().putAttribute(OsmTags.SURFACE, tags.get(OsmTags.SURFACE));
		} else if (highwayType.equals(OsmTags.PRIMARY) || highwayType.equals(OsmTags.PRIMARY_LINK)
				|| highwayType.equals(OsmTags.SECONDARY) || highwayType.equals(OsmTags.SECONDARY_LINK)) {
			link.getAttributes().putAttribute(OsmTags.SURFACE, "asphalt");
		}
	}

	private void setSmoothness(Link link, Map<String, String> tags) {
		if (tags.containsKey(OsmTags.SMOOTHNESS)) {
			link.getAttributes().putAttribute(OsmTags.SMOOTHNESS, tags.get(OsmTags.SMOOTHNESS));
		}
	}

	private void setCycleWay(Link link, Map<String, String> tags) {
		if (tags.containsKey(OsmTags.CYCLEWAY))
			link.getAttributes().putAttribute(OsmTags.CYCLEWAY, tags.get(OsmTags.CYCLEWAY));
	}

	private void setRestrictions(Link link, Map<String, String> tags) {
		if (tags.containsKey(OsmTags.BICYCLE))
			link.getAttributes().putAttribute(TransportMode.bike, tags.get(OsmTags.BICYCLE));
	}

	private void collectReverseDirectionForBicycle(Link link, Map<String, String> tags, boolean isReverse) {

		if (isReverse) {
			String linkKey = createLinkKey(link, isReverse);
			linksWhichNeedBackwardBicycleDirection.remove(linkKey);
		} else {
			if (tags.containsKey(OsmTags.ONEWAYBICYCLE) && tags.get(OsmTags.ONEWAYBICYCLE).equals("no")) {
				linksWhichNeedBackwardBicycleDirection.put(createLinkKey(link, isReverse), link.getId());
			} else if (tags.containsKey(OsmTags.CYCLEWAY)) {
				String tag = tags.get(OsmTags.CYCLEWAY);
				if (tag.equals("opposite") || tag.equals("opposite_track") || tag.equals("opposite_lane")) {
					linksWhichNeedBackwardBicycleDirection.put(createLinkKey(link, isReverse), link.getId());
				}
			}
		}
	}

	private String createLinkKey(Link link, boolean isReverse) {
		if (isReverse)
			return link.getId().toString() + link.getToNode().getId() + link.getFromNode().getId();
		return link.getId().toString() + link.getFromNode().getId() + link.getToNode().getId();
	}

	public static class Builder {

		private CoordinateTransformation transformation;
		private BiPredicate<Coord, Integer> linkFilter;
		private Predicate<Long> preserveNodes;
		private SupersonicOsmNetworkReader.AfterLinkCreated afterLinkCreated;

		private Builder() {
		}

		public Builder coordinateTransformation(CoordinateTransformation transformation) {
			this.transformation = transformation;
			return this;
		}

		public Builder includeLinkAtCoordWithHierarchy(BiPredicate<Coord, Integer> biPredicate) {
			this.linkFilter = biPredicate;
			return this;
		}

		public Builder preserveNodeWithId(Predicate<Long> predicate) {
			this.preserveNodes = predicate;
			return this;
		}

		public Builder afterLinkCreated(SupersonicOsmNetworkReader.AfterLinkCreated consumer) {
			this.afterLinkCreated = consumer;
			return this;
		}

		public SupersonicBicycleOsmNetworkReader build() {
			return new SupersonicBicycleOsmNetworkReader(transformation, linkFilter, preserveNodes, afterLinkCreated);
		}
	}
}
