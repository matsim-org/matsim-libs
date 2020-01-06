package org.matsim.contrib.osm.networkReader;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class SupersonicBicycleOsmNetworkReader {

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

		Network network = SupersonicOsmNetworkReader.builder()
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

		for (Id<Link> linkId : linksWhichNeedBackwardBicycleDirection.values()) {

			Link forwardLink = network.getLinks().get(linkId);
			Link reverseLink = createReverseBicycleLink(forwardLink, network.getFactory());
			network.addLink(reverseLink);
		}
		return network;
	}

	private void handleLink(Link link, Map<String, String> tags, SupersonicOsmNetworkReader.Direction direction) {

		String highwayType = tags.get(OsmTags.HIGHWAY);

		setAllowedModes(link, highwayType);
		setSurface(link, tags, highwayType);
		setSmoothness(link, tags);
		setCycleWay(link, tags);
		setRestrictions(link, tags);

		// do infrastructure factor
		link.getAttributes().putAttribute(BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 0.5);

		if (link.getAllowedModes().contains(TransportMode.bike))
			collectReverseDirectionForBicycle(link, tags, direction);

		afterLinkCreated.accept(link, tags, direction);
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

	private Link createReverseBicycleLink(Link forwardLink, NetworkFactory factory) {

		String linkId = forwardLink.getId().toString() + "_bike-reverse";
		Link result = factory.createLink(Id.createLinkId(linkId), forwardLink.getToNode(), forwardLink.getFromNode());
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

	private String createLinkKey(Link link, SupersonicOsmNetworkReader.Direction direction) {
		// this relies on internals of the network reader :-(
		if (direction == SupersonicOsmNetworkReader.Direction.Reverse)
			return link.getId().toString().replace("r", "") + link.getToNode().getId() + link.getFromNode().getId();
		return link.getId().toString().replace("f", "") + link.getFromNode().getId() + link.getToNode().getId();
	}

	private void collectReverseDirectionForBicycle(Link link, Map<String, String> tags, SupersonicOsmNetworkReader.Direction direction) {

		String linkKey = createLinkKey(link, direction);
		if (linksWhichNeedBackwardBicycleDirection.containsKey(linkKey)) {
			linksWhichNeedBackwardBicycleDirection.remove(linkKey);
		} else if (isReverseCycleWay(tags)) {
			// in case we have a forward link and the bicycle tags indicate a reverse direction we memorize
			// this link, so that we can add a reverse link for bicycles later.
			linksWhichNeedBackwardBicycleDirection.put(linkKey, link.getId());
		}
	}

	private boolean isReverseCycleWay(Map<String, String> tags) {
		if (tags.containsKey(OsmTags.ONEWAYBICYCLE) && tags.get(OsmTags.ONEWAYBICYCLE).equals("no")) return true;
		if (tags.containsKey(OsmTags.CYCLEWAY)) {
			String tag = tags.get(OsmTags.CYCLEWAY);
			return (tag.equals("opposite") || tag.equals("opposite_track") || tag.equals("opposite_lane"));
		}
		return false;
	}

	public static class Builder {

		private CoordinateTransformation transformation;
		private BiPredicate<Coord, Integer> linkFilter = (coord, integer) -> true;
		private Predicate<Long> preserveNodes = id -> false;
		private SupersonicOsmNetworkReader.AfterLinkCreated afterLinkCreated = (a, b, c) -> {
		};

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
