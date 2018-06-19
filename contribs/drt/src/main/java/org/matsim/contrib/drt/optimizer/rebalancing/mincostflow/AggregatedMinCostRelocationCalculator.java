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

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.google.inject.name.Named;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author michalm
 */
public class AggregatedMinCostRelocationCalculator implements MinCostRelocationCalculator {
	private final DrtZonalSystem zonalSystem;
	private final Network network;

	@Inject
	public AggregatedMinCostRelocationCalculator(DrtZonalSystem zonalSystem,
			@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network) {
		this.zonalSystem = zonalSystem;
		this.network = network;
	}

	@Override
	public List<Relocation> calcRelocations(List<Pair<String, Integer>> supply, List<Pair<String, Integer>> demand,
			Map<String, List<Vehicle>> rebalancableVehiclesPerZone) {
		List<Triple<String, String, Integer>> interZonalRelocations = new TransportProblem<String, String>(
				this::calcStraightLineDistance).solve(supply, demand);
		return calcRelocations(rebalancableVehiclesPerZone, interZonalRelocations);
	}

	private int calcStraightLineDistance(String zone1, String zone2) {
		return (int)DistanceUtils.calculateDistance(zonalSystem.getZoneCentroid(zone1),
				zonalSystem.getZoneCentroid(zone2));
	}

	private List<Relocation> calcRelocations(Map<String, List<Vehicle>> rebalancableVehiclesPerZone,
			List<Triple<String, String, Integer>> interZonalRelocations) {
		List<Relocation> relocations = new ArrayList<>();
		for (Triple<String, String, Integer> r : interZonalRelocations) {
			List<Vehicle> rebalancableVehicles = rebalancableVehiclesPerZone.get(r.getLeft());

			String toZone = r.getMiddle();
			Geometry z = zonalSystem.getZone(toZone);
			Coord zoneCentroid = MGC.point2Coord(z.getCentroid());
			Link destinationLink = NetworkUtils.getNearestLink(network, zoneCentroid);

			int flow = r.getRight();
			for (int f = 0; f < flow; f++) {
				// TODO use BestDispatchFinder (needs to be moved from taxi to dvrp) instead
				Vehicle nearestVehicle = findNearestVehicle(rebalancableVehicles, destinationLink);
				relocations.add(new Relocation(nearestVehicle, destinationLink));
				rebalancableVehicles.remove(nearestVehicle);// TODO use map to have O(1) removal
			}
		}
		return relocations;
	}

	private Vehicle findNearestVehicle(List<Vehicle> rebalancableVehicles, Link destinationLink) {
		Coord toCoord = destinationLink.getFromNode().getCoord();
		return rebalancableVehicles.stream().min(Comparator.comparing(v -> DistanceUtils.calculateSquaredDistance(//
				Schedules.getLastLinkInSchedule(v).getToNode().getCoord(), toCoord))).get();
	}
}
