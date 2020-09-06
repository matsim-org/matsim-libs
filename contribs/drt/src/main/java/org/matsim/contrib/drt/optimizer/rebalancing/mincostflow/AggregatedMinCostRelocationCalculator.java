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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.util.distance.DistanceUtils;

/**
 * Computes inter-zonal flows at the zonal (aggregated) level (i.e. without looking into individual vehicles)
 *
 * @author michalm
 */
public class AggregatedMinCostRelocationCalculator implements RelocationCalculator {
	public static class DrtZoneVehicleSurplus {
		public final DrtZone zone;
		public final int surplus;

		public DrtZoneVehicleSurplus(DrtZone zone, int surplus) {
			this.zone = zone;
			this.surplus = surplus;
		}
	}

	private final DrtZoneTargetLinkSelector targetLinkSelector;

	public AggregatedMinCostRelocationCalculator(DrtZoneTargetLinkSelector targetLinkSelector) {
		this.targetLinkSelector = targetLinkSelector;
	}

	@Override
	public List<Relocation> calcRelocations(List<DrtZoneVehicleSurplus> vehicleSurplus,
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		List<Pair<DrtZone, Integer>> supply = new ArrayList<>();
		List<Pair<DrtZone, Integer>> demand = new ArrayList<>();
		for (DrtZoneVehicleSurplus s : vehicleSurplus) {
			if (s.surplus > 0) {
				supply.add(Pair.of(s.zone, s.surplus));
			} else if (s.surplus < 0) {
				demand.add(Pair.of(s.zone, -s.surplus));
			}
		}

		return calcRelocations(rebalancableVehiclesPerZone,
				new TransportProblem<>(this::calcStraightLineDistance).solve(supply, demand));
	}

	private int calcStraightLineDistance(DrtZone zone1, DrtZone zone2) {
		return (int)DistanceUtils.calculateDistance(zone1.getCentroid(), zone2.getCentroid());
	}

	private List<Relocation> calcRelocations(Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone,
			List<TransportProblem.Flow<DrtZone, DrtZone>> flows) {
		List<Relocation> relocations = new ArrayList<>();
		for (TransportProblem.Flow<DrtZone, DrtZone> flow : flows) {
			List<DvrpVehicle> rebalancableVehicles = rebalancableVehiclesPerZone.get(flow.origin);

			Link targetLink = targetLinkSelector.selectTargetLink(flow.destination);

			for (int f = 0; f < flow.amount; f++) {
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
