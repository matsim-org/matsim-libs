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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.common.util.DistanceUtils;

/**
 * Computes inter-zonal flows at the zonal (aggregated) level (i.e. without looking into individual vehicles)
 *
 * @author michalm
 */
public class AggregatedMinCostRelocationCalculator implements ZonalRelocationCalculator {
	public static class DrtZoneVehicleSurplus {
		public final Zone zone;
		public final int surplus;

		public DrtZoneVehicleSurplus(Zone zone, int surplus) {
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
			Map<Zone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		return calcRelocations(rebalancableVehiclesPerZone, TransportProblem.solveForVehicleSurplus(vehicleSurplus));
	}

	private List<Relocation> calcRelocations(Map<Zone, List<DvrpVehicle>> rebalancableVehiclesPerZone,
			List<TransportProblem.Flow<Zone, Zone>> flows) {
		List<Relocation> relocations = new ArrayList<>();
		for (TransportProblem.Flow<Zone, Zone> flow : flows) {
			List<DvrpVehicle> rebalancableVehicles = rebalancableVehiclesPerZone.get(flow.origin());

			Link targetLink = targetLinkSelector.selectTargetLink(flow.destination());

			for (int f = 0; f < flow.amount(); f++) {
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
