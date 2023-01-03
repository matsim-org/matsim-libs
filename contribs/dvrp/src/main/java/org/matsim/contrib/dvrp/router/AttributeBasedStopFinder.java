package org.matsim.contrib.dvrp.router;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * A stop finder which works like ClosestAccessEgressFacilityFinder in that it
 * searches for the closes DRT stops given the start and end location of the
 * ride. However, the set of stops is filtered by stop categories. Each stop
 * facility has a "stopNetworks" attribute containing a comma-separated list of
 * stop network identifiers. Each trip in the population can then define which
 * stop network to use by defining the "stopNetwork" attribute for the trip (in
 * the preceding activity).
 */
public class AttributeBasedStopFinder implements AccessEgressFacilityFinder {
	private final static Logger logger = LogManager.getLogger(AttributeBasedStopFinder.class);

	public final static String FACILITY_STOP_NETWORKS_ATTRIBUTE = "stopNetworks";
	public final static String TRIP_STOP_NETWORK_ATTRIBUTE = "stopNetwork";
	public final static String DEFAULT_STOP_NETWORK = "DEFAULT";

	private final Map<String, AccessEgressFacilityFinder> facilityFinders;

	private AttributeBasedStopFinder(Map<String, AccessEgressFacilityFinder> facilityFinders) {
		this.facilityFinders = facilityFinders;
	}

	@Override
	public Optional<Pair<Facility, Facility>> findFacilities(Facility fromFacility, Facility toFacility,
			Attributes attributes) {
		String stopNetwork = Optional.ofNullable((String)attributes.getAttribute(TRIP_STOP_NETWORK_ATTRIBUTE))
				.orElse(DEFAULT_STOP_NETWORK);
		var facilityFinder = Objects.requireNonNull(facilityFinders.get(stopNetwork),
				() -> "Stop network does not exist: " + stopNetwork);
		return facilityFinder.findFacilities(fromFacility, toFacility, attributes);
	}

	static public <T extends Facility & Attributable> AttributeBasedStopFinder create(double maxDistance,
			Network network, Collection<T> facilities) {
		return create(maxDistance, network, facilities, AttributeBasedStopFinder::parseStopNetworks);
	}

	static public <T extends Facility> AttributeBasedStopFinder create(double maxDistance, Network network,
			Collection<T> facilities, Function<T, Set<String>> mapper) {
		return create(maxDistance, network, facilities.stream().collect(Collectors.toMap(f -> f, mapper)));
	}

	static public AttributeBasedStopFinder create(double maxDistance, Network network,
			Map<Facility, Set<String>> facilities) {
		Set<String> availableNames = new HashSet<>();
		facilities.values().forEach(availableNames::addAll);

		Map<String, AccessEgressFacilityFinder> facilityFinders = new HashMap<>();

		for (String networkName : availableNames) {
			List<? extends Facility> networkFacilities = facilities.entrySet().stream() //
					.filter(e -> e.getValue().contains(networkName)) //
					.map(e -> e.getKey()) //
					.collect(Collectors.toList());

			facilityFinders.put(networkName, new ClosestAccessEgressFacilityFinder(maxDistance, network,
					QuadTrees.createQuadTree(networkFacilities)));

			logger.info(
					String.format("Found %d facilities for stop network %s", networkFacilities.size(), networkName));
		}

		List<? extends Facility> defaultFacilities = facilities.entrySet().stream() //
				.filter(e -> e.getValue().isEmpty()) //
				.map(e -> e.getKey()) //
				.collect(Collectors.toList());

		if (defaultFacilities.size() > 0) {
			facilityFinders.put(DEFAULT_STOP_NETWORK, new ClosestAccessEgressFacilityFinder(maxDistance, network,
					QuadTrees.createQuadTree(defaultFacilities)));
		}

		logger.info(String.format("Found %d facilities for %s stop network", defaultFacilities.size(),
				DEFAULT_STOP_NETWORK));

		return new AttributeBasedStopFinder(facilityFinders);
	}

	private static Set<String> parseStopNetworks(Attributable facility) {
		String attributeValue = (String)facility.getAttributes().getAttribute(FACILITY_STOP_NETWORKS_ATTRIBUTE);

		if (attributeValue != null) {
			return Arrays.stream(attributeValue.split(",")).map(String::trim).collect(Collectors.toSet());
		}

		return Collections.emptySet();
	}
}
