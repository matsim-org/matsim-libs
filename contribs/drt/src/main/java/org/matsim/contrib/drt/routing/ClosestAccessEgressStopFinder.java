/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.routing;

import java.util.Comparator;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule.AccessEgressStopFinder;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class ClosestAccessEgressStopFinder implements AccessEgressStopFinder {

	private final Network network;
	private final Map<Id<TransitStopFacility>, TransitStopFacility> stops;
	private final double maxWalkDistance;
	private final double walkBeelineFactor;

	@Inject
	public ClosestAccessEgressStopFinder(@Named(TransportMode.drt) TransitSchedule transitSchedule,
			DrtConfigGroup drtconfig, PlansCalcRouteConfigGroup planscCalcRouteCfg, Network network) {
		this.network = network;
		this.stops = transitSchedule.getFacilities();
		this.maxWalkDistance = drtconfig.getMaxWalkDistance();
		this.walkBeelineFactor = planscCalcRouteCfg.getModeRoutingParams().get(TransportMode.walk)
				.getBeelineDistanceFactor();;
	}

	@Override
	public Pair<TransitStopFacility, TransitStopFacility> findStops(Facility<?> fromFacility, Facility<?> toFacility) {
		TransitStopFacility accessFacility = findClosestStop(fromFacility);
		if (accessFacility == null) {
			return new ImmutablePair<>(null, null);
		}

		TransitStopFacility egressFacility = findClosestStop(toFacility);
		return new ImmutablePair<TransitStopFacility, TransitStopFacility>(accessFacility, egressFacility);
	}

	private TransitStopFacility findClosestStop(Facility<?> facility) {
		Coord coord = StopBasedDrtRoutingModule.getFacilityCoord(facility, network);

		TransitStopFacility closest = stops.values().stream()//
				.max(Comparator.comparing(s -> DistanceUtils.calculateSquaredDistance(coord, s.getCoord())))//
				.orElse(null);

		if (closest != null
				&& walkBeelineFactor * DistanceUtils.calculateDistance(coord, closest.getCoord()) <= maxWalkDistance) {
			return closest;
		} else {
			return null;
		}
	}
}
