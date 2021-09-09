/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.router;

import com.google.common.base.Verify;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.*;
import java.util.stream.Collectors;

public class StopNetworkFacilityFinder implements AccessEgressFacilityFinder {
	private final static Logger logger = Logger.getLogger(StopNetworkFacilityFinder.class);

	public final static String FACILITY_STOP_NETWORKS_ATTRIBUTE = "stopNetworks";
	public final static String TRIP_STOP_NETWORK_ATTRIBUTE = "stopNetwork";
	public final static String DEFAULT_STOP_NETWORK = "DEFAULT";

	private final Network network;
	private final Map<String, QuadTree<? extends DrtStopFacility>> quadtrees;
	private final double maxDistance;

	private StopNetworkFacilityFinder(double maxDistance, Network network,
                                      Map<String, QuadTree<? extends DrtStopFacility>> quadtrees) {
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

	static public StopNetworkFacilityFinder create(double maxDistance, Network network, DrtStopNetwork stopNetwork) {
		Set<String> availableNames = new HashSet<>();
		stopNetwork.getDrtStops().values().forEach(f -> availableNames.addAll(parseStopNetworks(f)));

		Map<String, QuadTree<? extends DrtStopFacility>> quadtrees = new HashMap<>();

		for (String networkName : availableNames) {
			List<? extends DrtStopFacility> networkFacilities = stopNetwork.getDrtStops().values().stream()
					.filter(f -> parseStopNetworks(f).contains(networkName)).collect(Collectors.toList());

			quadtrees.put(networkName, QuadTrees.createQuadTree(networkFacilities));

			logger.info(
					String.format("Found %d facilities for stop network %s", networkFacilities.size(), networkName));
		}

		List<? extends DrtStopFacility> defaultFacilities = stopNetwork.getDrtStops().values().stream()
				.filter(f -> parseStopNetworks(f).isEmpty()).collect(Collectors.toList());

		if (defaultFacilities.size() > 0) {
			quadtrees.put(DEFAULT_STOP_NETWORK, QuadTrees.createQuadTree(defaultFacilities));
		}

		logger.info(String.format("Found %d facilities for %s stop network", defaultFacilities.size(),
				DEFAULT_STOP_NETWORK));

		return new StopNetworkFacilityFinder(maxDistance, network, quadtrees);
	}

	private static Set<String> parseStopNetworks(DrtStopFacility facility) {
		String attributeValue = (String) facility.getAttributes().getAttribute(FACILITY_STOP_NETWORKS_ATTRIBUTE);

		if (attributeValue != null) {
			return Arrays.asList(attributeValue.split(",")).stream().map(String::trim).collect(Collectors.toSet());
		}

		return Collections.emptySet();
	}

	@Override
	public Optional<Pair<Facility, Facility>> findFacilities(Facility fromFacility, Facility toFacility) {
		throw new IllegalStateException("This finder should always be called with service information.");
	}
}
