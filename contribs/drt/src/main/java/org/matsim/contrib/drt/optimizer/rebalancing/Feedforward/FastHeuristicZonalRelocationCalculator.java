package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.ZonalRelocationCalculator;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.FastHeuristicLinkBasedRelocationCalculator;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.LinkBasedRelocationCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FastHeuristicZonalRelocationCalculator implements ZonalRelocationCalculator {

	private final DrtZoneTargetLinkSelector drtZoneTargetLinkSelector;
	private final LinkBasedRelocationCalculator linkBasedRelocationCalculator = new FastHeuristicLinkBasedRelocationCalculator();

	public FastHeuristicZonalRelocationCalculator(DrtZoneTargetLinkSelector drtZoneTargetLinkSelector) {
		this.drtZoneTargetLinkSelector = drtZoneTargetLinkSelector;
	}

	@Override
	public List<Relocation> calcRelocations(List<DrtZoneVehicleSurplus> vehicleSurplus,
			Map<Zone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		List<Link> targetLinks = new ArrayList<>();
		List<DvrpVehicle> selectedRebalancableVehicles = new ArrayList<>();

		for (DrtZoneVehicleSurplus drtZoneVehicleSurplus : vehicleSurplus) {
			int surplus = drtZoneVehicleSurplus.surplus;
			Zone zone = drtZoneVehicleSurplus.zone;

			if (surplus > 0) {
				List<DvrpVehicle> rebalancableVehiclesInZone = rebalancableVehiclesPerZone.get(zone);
				selectedRebalancableVehicles.addAll(rebalancableVehiclesInZone.subList(0, surplus));
			} else if (surplus < 0) {
				for (int i = 0; i > surplus; i--) {
					targetLinks.add(drtZoneTargetLinkSelector.selectTargetLink(zone));
				}
			}
		}

		return linkBasedRelocationCalculator.calcRelocations(targetLinks, selectedRebalancableVehicles);
	}
}
