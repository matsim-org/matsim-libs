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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
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
public class DefaultAccessEgressStopFinder implements AccessEgressStopFinder {

	private final Network network;
	private final Map<Id<TransitStopFacility>, TransitStopFacility> stops;
	private final double maxWalkDistance;
	private final double walkBeelineFactor;

	@Inject
	public DefaultAccessEgressStopFinder(@Named(DrtConfigGroup.DRT_MODE) TransitSchedule transitSchedule,
			DrtConfigGroup drtconfig, PlansCalcRouteConfigGroup planscCalcRouteCfg, Network network) {
		this.network = network;
		this.stops = transitSchedule.getFacilities();
		this.maxWalkDistance = drtconfig.getMaxWalkDistance();
		this.walkBeelineFactor = planscCalcRouteCfg.getModeRoutingParams().get(TransportMode.walk)
				.getBeelineDistanceFactor();;
	}

	@Override
	public Pair<TransitStopFacility, TransitStopFacility> findStops(Facility<?> fromFacility, Facility<?> toFacility) {
		TransitStopFacility accessFacility = findAccessFacility(fromFacility, toFacility);
		if (accessFacility == null) {
			return new ImmutablePair<>(null, null);
		}

		TransitStopFacility egressFacility = findEgressFacility(accessFacility, toFacility);
		return new ImmutablePair<TransitStopFacility, TransitStopFacility>(accessFacility, egressFacility);
	}

	/**
	 * @param fromFacility
	 * @param toFacility
	 * @return
	 */
	private TransitStopFacility findAccessFacility(Facility<?> fromFacility, Facility<?> toFacility) {
		Coord fromCoord = StopBasedDrtRoutingModule.getFacilityCoord(fromFacility, network);
		Coord toCoord = StopBasedDrtRoutingModule.getFacilityCoord(toFacility, network);
		Set<TransitStopFacility> stopCandidates = findStopCandidates(fromCoord);

		TransitStopFacility accessFacility = null;
		double bestHeading = Double.MAX_VALUE;
		for (TransitStopFacility stop : stopCandidates) {
			Link stopLink = network.getLinks().get(stop.getLinkId());
			if (stopLink == null) {
				throw new RuntimeException(
						"Stop " + stop.getId() + " on link id " + stop.getLinkId() + " is not part of the network.");
			}

			double[] stopLinkVector = getVector(stopLink.getFromNode().getCoord(), stopLink.getToNode().getCoord());
			double[] destinationVector = getVector(stopLink.getFromNode().getCoord(), toCoord);
			double heading = calcHeading(stopLinkVector, destinationVector);
			if (heading < bestHeading) {
				accessFacility = stop;
				bestHeading = heading;
			}
		}
		return accessFacility;
	}

	private TransitStopFacility findEgressFacility(TransitStopFacility fromStopFacility, Facility<?> toFacility) {
		Coord fromCoord = fromStopFacility.getCoord();
		Coord toCoord = StopBasedDrtRoutingModule.getFacilityCoord(toFacility, network);
		Set<TransitStopFacility> stopCandidates = findStopCandidates(toCoord);

		TransitStopFacility egressFacility = null;
		double bestHeading = Double.MAX_VALUE;
		for (TransitStopFacility stop : stopCandidates) {
			Link stopLink = network.getLinks().get(stop.getLinkId());

			double[] stopLinkVector = getVector(stopLink.getFromNode().getCoord(), stopLink.getToNode().getCoord());
			double[] originVector = getVector(fromCoord, stopLink.getToNode().getCoord());
			double heading = calcHeading(stopLinkVector, originVector);
			if (heading < bestHeading) {
				egressFacility = stop;
				bestHeading = heading;
			}
		}
		return egressFacility;
	}

	/**
	 * @param stopLinkVector
	 * @param destinationVector
	 * @return
	 */
	private double calcHeading(double[] stopLinkVector, double[] destinationVector) {
		return Math.acos((stopLinkVector[0] * destinationVector[0] + stopLinkVector[1] * destinationVector[1]) //
				/ (Math.sqrt(stopLinkVector[0] * stopLinkVector[0] + stopLinkVector[1] * stopLinkVector[1])//
						* Math.sqrt(destinationVector[0] * destinationVector[0]
								+ destinationVector[1] * destinationVector[1])));
	}

	private Set<TransitStopFacility> findStopCandidates(Coord coord) {
		double maxBeelineDistance = (maxWalkDistance / walkBeelineFactor);
		double maxSquaredBeelineDistance = maxBeelineDistance * maxBeelineDistance;
		return stops.values().stream()//
				.filter(s -> DistanceUtils.calculateSquaredDistance(coord, s.getCoord()) < maxSquaredBeelineDistance)//
				.collect(Collectors.toSet());
	}

	private double[] getVector(Coord from, Coord to) {
		double[] vector = new double[2];
		vector[0] = to.getX() - from.getX();
		vector[1] = to.getY() - from.getY();
		return vector;
	}
}
