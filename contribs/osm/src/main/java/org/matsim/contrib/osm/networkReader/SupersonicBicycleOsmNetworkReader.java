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
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class SupersonicBicycleOsmNetworkReader {

	public static final String SURFACE = "surface";
	public static final String SMOOTHNESS = "smoothness";
	public static final String BICYCLE_INFRASTRUCTURE_SPEED_FACTOR = "bicycleInfrastructureSpeedFactor";
	private static final double BIKE_PCU = 0.25;
	private Set<String> bicycleNotAllowed = new HashSet<>(Arrays.asList("motorway", "motorway_link", "trunk", "trunk_link"));

	private CoordinateTransformation coordinateTransformation;
	private BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy;
	private Predicate<Long> preserveNodeWithId;

	private Set<Id<Link>> linksWhichNeedBackwardBicycleDirection = ConcurrentHashMap.newKeySet();

	public SupersonicBicycleOsmNetworkReader(CoordinateTransformation coordinateTransformation, BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy, Predicate<Long> preserveNodeWithId) {
		this.coordinateTransformation = coordinateTransformation;
		this.includeLinkAtCoordWithHierarchy = includeLinkAtCoordWithHierarchy;
		this.preserveNodeWithId = preserveNodeWithId;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Network read(Path inputFile) {

		return SupersonicOsmNetworkReader.builder()
				.coordinateTransformation(this.coordinateTransformation)
				.addOverridingLinkProperties("track", new LinkProperties(9, 1, 30 / 3.6, 1500 * BIKE_PCU, false))
				.addOverridingLinkProperties("cycleway", new LinkProperties(9, 1, 30 / 3.6, 1500 * BIKE_PCU, false))
				.addOverridingLinkProperties("service", new LinkProperties(9, 1, 10 / 3.6, 100 * BIKE_PCU, false))
				.addOverridingLinkProperties("footway", new LinkProperties(10, 1, 10 / 3.6, 600 * BIKE_PCU, false))
				.addOverridingLinkProperties("pedestrian", new LinkProperties(10, 1, 10 / 3.6, 600 * BIKE_PCU, false))
				.addOverridingLinkProperties("path", new LinkProperties(10, 1, 20 / 3.6, 600 * BIKE_PCU, false))
				.addOverridingLinkProperties("steps", new LinkProperties(11, 1, 1 / 3.6, 50 * BIKE_PCU, false))
				.includeLinkAtCoordWithHierarchy(this.includeLinkAtCoordWithHierarchy)
				.preserveNodeWithId(this.preserveNodeWithId)
				.afterLinkCreated(this::handleLink)
				.build()
				.read(inputFile);
	}

	private void handleLink(Link link, Map<String, String> tags, boolean isReverse) {

		// add bike mode to most streets
		String highwayType = tags.get(OsmTags.HIGHWAY);
		if (!bicycleNotAllowed.contains(highwayType)) {
			HashSet<String> allowedModes = new HashSet<>(link.getAllowedModes());
			allowedModes.add(TransportMode.bike);
			allowedModes.add(TransportMode.ride);
			link.setAllowedModes(allowedModes);
		}

		//do surface
		if (tags.containsKey(SURFACE)) {
			link.getAttributes().putAttribute(SURFACE, tags.get(SURFACE));
		} else if (highwayType.equals("primary") || highwayType.equals("primary_link") || highwayType.equals("secondary") || highwayType.equals("secondary_link")) {
			link.getAttributes().putAttribute(SURFACE, "asphalt");
		}

		// do smoothness
		if (tags.containsKey(SMOOTHNESS)) {
			link.getAttributes().putAttribute(SMOOTHNESS, tags.get(SMOOTHNESS));
		}

		// do infrastructure factor
		link.getAttributes().putAttribute(BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 0.5);

		//TODO add reverse direction for bicylces if street is only one way. Not sure how to fit that into the model of the reader

	}

	public static class Builder {

		private CoordinateTransformation transformation;
		private BiPredicate<Coord, Integer> linkFilter;
		private Predicate<Long> preserveNodes;

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

		public SupersonicBicycleOsmNetworkReader build() {
			return new SupersonicBicycleOsmNetworkReader(transformation, linkFilter, preserveNodes);
		}
	}
}
