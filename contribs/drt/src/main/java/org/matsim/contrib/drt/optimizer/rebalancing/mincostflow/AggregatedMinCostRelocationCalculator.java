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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private static final Logger log = LogManager.getLogger(AggregatedMinCostRelocationCalculator.class);

	public static class DrtZoneVehicleSurplus {
		public final Zone zone;
		public final int surplus;

		public DrtZoneVehicleSurplus(Zone zone, int surplus) {
			this.zone = zone;
			this.surplus = surplus;
		}
	}

	private final DrtZoneTargetLinkSelector targetLinkSelector;
	private final int maxRelocationDistance;

	public AggregatedMinCostRelocationCalculator(DrtZoneTargetLinkSelector targetLinkSelector) {
		this(targetLinkSelector, Double.POSITIVE_INFINITY);
	}

	/**
	 * @param maxRelocationDistance maximum straight-line distance [m] between the origin and the destination zone of a
	 *                              relocation, or {@code Double.POSITIVE_INFINITY} for no limit
	 */
	public AggregatedMinCostRelocationCalculator(DrtZoneTargetLinkSelector targetLinkSelector,
			double maxRelocationDistance) {
		this.targetLinkSelector = targetLinkSelector;
		this.maxRelocationDistance = maxRelocationDistance < TransportProblem.NO_MAX_COST ?
				(int)maxRelocationDistance :
				TransportProblem.NO_MAX_COST;
	}

	@Override
	public List<Relocation> calcRelocations(List<DrtZoneVehicleSurplus> vehicleSurplus,
			Map<Zone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		List<TransportProblem.Flow<Zone, Zone>> flows = TransportProblem.solveForVehicleSurplus(vehicleSurplus,
				maxRelocationDistance);
		logUnservedDeficit(vehicleSurplus, flows);
		return calcRelocations(rebalancableVehiclesPerZone, flows);
	}

	/**
	 * Without a distance limit the transport problem always saturates the scarce side, so there is nothing to report.
	 * With a limit, a zone can end up with no surplus zone within reach, in which case its deficit stays unserved. That
	 * is the price of the limit and it has to be visible in the log, otherwise the vehicles simply never show up.
	 */
	private void logUnservedDeficit(List<DrtZoneVehicleSurplus> vehicleSurplus,
			List<TransportProblem.Flow<Zone, Zone>> flows) {
		if (maxRelocationDistance == TransportProblem.NO_MAX_COST) {
			return;
		}

		int totalSupply = 0;
		int totalDemand = 0;
		int demandZones = 0;
		for (DrtZoneVehicleSurplus s : vehicleSurplus) {
			if (s.surplus > 0) {
				totalSupply += s.surplus;
			} else if (s.surplus < 0) {
				totalDemand -= s.surplus;
				demandZones++;
			}
		}

		int served = 0;
		Set<Zone> servedZones = new HashSet<>();
		for (TransportProblem.Flow<Zone, Zone> flow : flows) {
			served += flow.amount();
			servedZones.add(flow.destination());
		}

		int feasibleWithoutLimit = Math.min(totalSupply, totalDemand);
		int unserved = feasibleWithoutLimit - served;
		if (unserved > 0) {
			log.info("maxRelocationDistance={} m drops {} of {} possible relocations;"
							+ " {} of {} zones with a deficit receive no vehicle at all", maxRelocationDistance, unserved,
					feasibleWithoutLimit, demandZones - servedZones.size(), demandZones);
		}
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
