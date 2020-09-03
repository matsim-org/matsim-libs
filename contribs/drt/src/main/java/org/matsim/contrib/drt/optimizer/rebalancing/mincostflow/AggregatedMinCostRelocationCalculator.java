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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.util.distance.DistanceUtils;

/**
 * @author michalm
 */
public class AggregatedMinCostRelocationCalculator implements MinCostRelocationCalculator {
	private final DrtZonalSystem zonalSystem;
	private final Network network;
	private final DrtZoneTargetLinkSelector targetLinkSelector;

	public AggregatedMinCostRelocationCalculator(DrtZonalSystem zonalSystem, Network network, DrtZoneTargetLinkSelector targetLinkSelector) {
		this.zonalSystem = zonalSystem;
		this.network = network;
		this.targetLinkSelector = targetLinkSelector;
	}

	@Override
	public List<Relocation> calcRelocations(List<Pair<DrtZone, Integer>> supply, List<Pair<DrtZone, Integer>> demand,
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		List<Triple<DrtZone, DrtZone, Integer>> interZonalRelocations = new TransportProblem<>(
				this::calcStraightLineDistance).solve(supply, demand);
		return calcRelocations(rebalancableVehiclesPerZone, interZonalRelocations);
	}

	private int calcStraightLineDistance(DrtZone zone1, DrtZone zone2) {
		return (int)DistanceUtils.calculateDistance(zone1.getCentroid(), zone2.getCentroid());
	}

	private List<Relocation> calcRelocations(Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone,
			List<Triple<DrtZone, DrtZone, Integer>> interZonalRelocations) {
		List<Relocation> relocations = new ArrayList<>();
		for (Triple<DrtZone, DrtZone, Integer> r : interZonalRelocations) {
			List<DvrpVehicle> rebalancableVehicles = rebalancableVehiclesPerZone.get(r.getLeft());

			DrtZone toZone = r.getMiddle();
			Link targetLink = targetLinkSelector.selectTargetLink(toZone);

			int flow = r.getRight();
			for (int f = 0; f < flow; f++) {
				// TODO use BestDispatchFinder (needs to be moved from taxi to dvrp) instead
				DvrpVehicle nearestVehicle = findNearestVehicle(rebalancableVehicles, targetLink);
				relocations.add(new Relocation(nearestVehicle, targetLink));
				rebalancableVehicles.remove(nearestVehicle);// TODO use map to have O(1) removal
			}
		}
		return relocations;
	}

	private DvrpVehicle findNearestVehicle(List<DvrpVehicle> rebalancableVehicles, Link destinationLink) {
		Coord toCoord = destinationLink.getFromNode().getCoord();
		return rebalancableVehicles.stream()
				.min(Comparator.comparing(v -> DistanceUtils.calculateSquaredDistance(
						Schedules.getLastLinkInSchedule(v).getToNode().getCoord(), toCoord)))
				.get();
	}
}
