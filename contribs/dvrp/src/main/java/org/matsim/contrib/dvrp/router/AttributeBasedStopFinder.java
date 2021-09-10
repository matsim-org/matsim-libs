package org.matsim.contrib.dvrp.router;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.google.common.base.Verify;

/**
 * A stop finder which works like ClosestAccessEgressFacilityFinder in that it
 * searches for the closes DRT stops given the start and end location of the
 * ride. However, the set of stops is filtered by stop categories. Each stop
 * facility has a "stopNetworks" attribute containing a comma-separated list of
 * stop network identifiers. Each trip in the population can then define which
 * stop network to use by defining the "stopNetwork" attribute for the trip (in
 * the preceding activity).
 *
 */
public class AttributeBasedStopFinder implements AccessEgressFacilityFinder {
	private final static Logger logger = Logger.getLogger(AttributeBasedStopFinder.class);

	public final static String FACILITY_STOP_NETWORKS_ATTRIBUTE = "stopNetworks";
	public final static String TRIP_STOP_NETWORK_ATTRIBUTE = "stopNetwork";
	public final static String DEFAULT_STOP_NETWORK = "DEFAULT";

	private final Network network;
	private final Map<String, QuadTree<? extends Facility>> quadtrees;
	private final double maxDistance;

	private AttributeBasedStopFinder(double maxDistance, Network network,
			Map<String, QuadTree<? extends Facility>> quadtrees) {
		this.network = network;
		this.quadtrees = quadtrees;
		this.maxDistance = maxDistance;
	}

	@Override
	public Optional<Pair<Facility, Facility>> findFacilities(Facility fromFacility, Facility toFacility,
			Attributes attributes) {
		String stopNetwork = Optional.ofNullable((String) attributes.getAttribute(TRIP_STOP_NETWORK_ATTRIBUTE))
				.orElse(DEFAULT_STOP_NETWORK);

		Facility accessFacility = findClosestStop(fromFacility, stopNetwork);

		if (accessFacility == null) {
			return Optional.empty();
		}

		Facility egressFacility = findClosestStop(toFacility, stopNetwork);

		return egressFacility == null ? Optional.empty()
				: Optional.of(new ImmutablePair<>(accessFacility, egressFacility));
	}

	private Facility findClosestStop(Facility facility, String stopNetwork) {
		QuadTree<? extends Facility> selectedQuadtree = quadtrees.get(stopNetwork);
		Verify.verify(selectedQuadtree != null, "Stop network does not exist: " + stopNetwork);

		Coord coord = getFacilityCoord(facility, network);
		Facility closestStop = selectedQuadtree.getClosest(coord.getX(), coord.getY());

		double closestStopDistance = CoordUtils.calcEuclideanDistance(coord, closestStop.getCoord());
		return closestStopDistance > maxDistance ? null : closestStop;
	}

	static Coord getFacilityCoord(Facility facility, Network network) {
		Coord coord = facility.getCoord();
		if (coord == null) {
			coord = network.getLinks().get(facility.getLinkId()).getCoord();
			Verify.verify(coord != null, "From facility has neither coordinates nor link Id. Should not happen.");
		}
		return coord;
	}

	static public <T extends Facility & Attributable> AttributeBasedStopFinder create(double maxDistance,
			Network network, Collection<T> facilities) {
		Map<Facility, Set<String>> facilityMap = new HashMap<>();
		facilities.forEach(f -> facilityMap.put(f, parseStopNetworks(f)));
		return create(maxDistance, network, facilityMap);
	}

	static public AttributeBasedStopFinder create(double maxDistance, Network network, Collection<Facility> facilities,
			Function<Facility, Set<String>> mapper) {
		Map<Facility, Set<String>> facilityMap = new HashMap<>();
		facilities.forEach(f -> facilityMap.put(f, mapper.apply(f)));
		return create(maxDistance, network, facilityMap);
	}

	static public AttributeBasedStopFinder create(double maxDistance, Network network,
			Map<Facility, Set<String>> facilities) {
		Set<String> availableNames = new HashSet<>();
		facilities.values().forEach(availableNames::addAll);

		Map<String, QuadTree<? extends Facility>> quadtrees = new HashMap<>();

		for (String networkName : availableNames) {
			List<? extends Facility> networkFacilities = facilities.entrySet().stream() //
					.filter(e -> e.getValue().contains(networkName)) //
					.map(e -> e.getKey()) //
					.collect(Collectors.toList());

			quadtrees.put(networkName, QuadTrees.createQuadTree(networkFacilities));

			logger.info(
					String.format("Found %d facilities for stop network %s", networkFacilities.size(), networkName));
		}

		List<? extends Facility> defaultFacilities = facilities.entrySet().stream() //
				.filter(e -> e.getValue().isEmpty()) //
				.map(e -> e.getKey()) //
				.collect(Collectors.toList());

		if (defaultFacilities.size() > 0) {
			quadtrees.put(DEFAULT_STOP_NETWORK, QuadTrees.createQuadTree(defaultFacilities));
		}

		logger.info(String.format("Found %d facilities for %s stop network", defaultFacilities.size(),
				DEFAULT_STOP_NETWORK));

		return new AttributeBasedStopFinder(maxDistance, network, quadtrees);
	}

	private static Set<String> parseStopNetworks(Attributable facility) {
		String attributeValue = (String) facility.getAttributes().getAttribute(FACILITY_STOP_NETWORKS_ATTRIBUTE);

		if (attributeValue != null) {
			return Arrays.asList(attributeValue.split(",")).stream().map(String::trim).collect(Collectors.toSet());
		}

		return Collections.emptySet();
	}
}
