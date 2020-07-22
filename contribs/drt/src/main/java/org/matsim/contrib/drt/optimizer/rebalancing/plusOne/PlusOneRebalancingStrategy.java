package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostRelocationCalculator;
import org.matsim.contrib.drt.optimizer.rebalancing.toolbox.VehicleInfoCollector;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;

public class PlusOneRebalancingStrategy implements RebalancingStrategy, PersonDepartureEventHandler {
	private final DrtZonalSystem zonalSystem;
	private final Fleet fleet;
	private final MinCostRelocationCalculator minCostRelocationCalculator;
	private final PlusOneRebalancingParams params;

	private final List<Pair<String, Integer>> supply = new ArrayList<>();
	private final List<Pair<String, Integer>> demand = new ArrayList<>();
	private final Map<String, Integer> targetMap = new HashMap<>();

	private final int baseValue = 1; // The base value in the target calculation for each region //TODO put this in
										// the parameter file

	public PlusOneRebalancingStrategy(DrtZonalSystem zonalSystem, Fleet fleet,
			MinCostRelocationCalculator minCostRelocationCalculator, PlusOneRebalancingParams params) {
		this.zonalSystem = zonalSystem;
		this.fleet = fleet;
		this.minCostRelocationCalculator = minCostRelocationCalculator;
		this.params = params;
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		// Initialization
		VehicleInfoCollector vehicleInfoCollector = new VehicleInfoCollector(fleet, zonalSystem);
		demand.clear();
		supply.clear();

		// Get idling vehicles in each zone
		Map<String, List<DvrpVehicle>> rebalancableVehiclesPerZone = vehicleInfoCollector
				.groupRebalancableVehicles(rebalancableVehicles, time, params.getMinServiceTime());
		if (rebalancableVehiclesPerZone.isEmpty()) {
			return Collections.emptyList();
		}

		// Get soon idle vehicle for each zone
		Map<String, List<DvrpVehicle>> soonIdleVehiclesPerZone = vehicleInfoCollector.groupSoonIdleVehicles(time,
				params.getMaxTimeBeforeIdle(), params.getMinServiceTime());

		// Calculate demand and supply for each zone
		for (String z : zonalSystem.getZones().keySet()) {
			int rebalancable = rebalancableVehiclesPerZone.getOrDefault(z, Collections.emptyList()).size();
			int soonIdle = soonIdleVehiclesPerZone.getOrDefault(z, Collections.emptyList()).size();
			int target = targetMap.get(z);
			int delta = Math.min(rebalancable + soonIdle - target, rebalancable);
			if (delta < 0) {
				demand.add(Pair.of(z, -delta));
			} else if (delta > 0) {
				supply.add(Pair.of(z, delta));
			}
		}

		// clear the target map for next rebalancing cycle
		for (String z : zonalSystem.getZones().keySet()) {
			targetMap.put(z, baseValue);
		}
		// calculate using min cost flow method
		return minCostRelocationCalculator.calcRelocations(supply, demand, rebalancableVehiclesPerZone);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals("drt")) { // TODO get the string from drt config file
			String zoneId = zonalSystem.getZoneForLinkId(event.getLinkId());
			int newValue = targetMap.get(zoneId) + 1;
			targetMap.put(zoneId, newValue);
		}
	}

}
