package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.RelocationCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.util.distance.DistanceUtils;

public class FastHeuristicRelocationCalculator implements RelocationCalculator {

	private final DrtZoneTargetLinkSelector drtZoneTargetLinkSelector;
	private final List<DvrpVehicle> truelyRebalancableVehicles = new ArrayList<>();

	public FastHeuristicRelocationCalculator(DrtZoneTargetLinkSelector drtZoneTargetLinkSelector) {
		this.drtZoneTargetLinkSelector = drtZoneTargetLinkSelector;
	}

	@Override
	public List<Relocation> calcRelocations(List<DrtZoneVehicleSurplus> vehicleSurplus,
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		List<Relocation> relocations = new ArrayList<>();
		List<Link> destinationLinks = new ArrayList<>();
		truelyRebalancableVehicles.clear();
		
		for (DrtZoneVehicleSurplus drtZoneVehicleSurplus : vehicleSurplus) {
			int surplus = drtZoneVehicleSurplus.surplus;
			DrtZone zone = drtZoneVehicleSurplus.zone;

			if (surplus > 0) {
				int numVehiclesToAdd = Math.min(surplus,
						rebalancableVehiclesPerZone.getOrDefault(zone, new ArrayList<>()).size());
				for (int i = 0; i < numVehiclesToAdd; i++) {
					truelyRebalancableVehicles
							.add(rebalancableVehiclesPerZone.getOrDefault(zone, new ArrayList<>()).get(i));
				}
			} else if (surplus < 0) {
				int deficit = -1 * surplus;
				for (int i = 0; i < deficit; i++) {
					Link destinationLink = drtZoneTargetLinkSelector.selectTargetLink(zone);
					destinationLinks.add(destinationLink);
				}
			}
		}
		
		if (!truelyRebalancableVehicles.isEmpty()) {
			for (Link link : destinationLinks) {
				DvrpVehicle nearestVehicle = truelyRebalancableVehicles.stream().min(Comparator.comparing(
						v -> DistanceUtils.calculateSquaredDistance(Schedules.getLastLinkInSchedule(v).getCoord(),
								link.getCoord())))
						.get();
				relocations.add(new Relocation(nearestVehicle, link));
				truelyRebalancableVehicles.remove(nearestVehicle);
				if (truelyRebalancableVehicles.isEmpty()) {
					break;
				}
			}
		}
		return relocations;
	}
	
	public List<DvrpVehicle> getTruelyRebalancableVehicles() {
		return truelyRebalancableVehicles;
	}
}
