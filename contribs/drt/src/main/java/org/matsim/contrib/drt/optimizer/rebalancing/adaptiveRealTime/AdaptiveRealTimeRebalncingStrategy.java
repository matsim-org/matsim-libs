package org.matsim.contrib.drt.optimizer.rebalancing.adaptiveRealTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostRelocationCalculator;
import org.matsim.contrib.drt.optimizer.rebalancing.toolbox.VehicleInfoCollector;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;

/**
 * 
 * @author Chengqi Lu This strategy is based on the Adaptive Real Time
 *         Rebalancing Algorithm in the AMoDeus. At each rebalancing period, the
 *         algorithm will distribute all the idling vehicles across the all the
 *         zones based on the demand-supply information in each zone.
 */
public class AdaptiveRealTimeRebalncingStrategy implements RebalancingStrategy {
	private final DrtZonalSystem zonalSystem;
	private final Fleet fleet;
	private final MinCostRelocationCalculator minCostRelocationCalculator;
	private final AdaptiveRealTimeRebalancingParams params;

	private final List<Pair<String, Integer>> supply = new ArrayList<>();
	private final List<Pair<String, Integer>> demand = new ArrayList<>();
	private final Map<String, Integer> targetMap = new HashMap<>();

	public AdaptiveRealTimeRebalncingStrategy(DrtZonalSystem zonalSystem, Fleet fleet,
			MinCostRelocationCalculator minCostRelocationCalculator, AdaptiveRealTimeRebalancingParams params) {
		this.zonalSystem = zonalSystem;
		this.fleet = fleet;
		this.minCostRelocationCalculator = minCostRelocationCalculator;
		this.params = params;
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		// initialization each time this function is called
		VehicleInfoCollector vehicleInfoCollector = new VehicleInfoCollector(fleet, zonalSystem);
		supply.clear();
		demand.clear();
		targetMap.clear();
		// Get idling vehicles in each zone
		Map<String, List<DvrpVehicle>> rebalancableVehiclesPerZone = vehicleInfoCollector
				.groupRebalancableVehicles(rebalancableVehicles, time, params.getMinServiceTime());
		if (rebalancableVehiclesPerZone.isEmpty()) {
			return Collections.emptyList();
		}

		// Get soon idle vehicle for each zone
		Map<String, List<DvrpVehicle>> soonIdleVehiclesPerZone = vehicleInfoCollector.groupSoonIdleVehicles(time,
				params.getMaxTimeBeforeIdle(), params.getMinServiceTime());

		// calculate real time target of each zone
		calculateRealTimeRebalanceTarget(targetMap, fleet, zonalSystem, rebalancableVehicles);

		// calculate supply and demand for each zone
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

		System.err.println("we are here!!!"); // TODO delete this line after running properly
		
		// calculate using min cost flow method
		return minCostRelocationCalculator.calcRelocations(supply, demand, rebalancableVehiclesPerZone);
	}

	private void calculateRealTimeRebalanceTarget(Map<String, Integer> targetMap, Fleet fleet,
			DrtZonalSystem zonalSystem, Stream<? extends DvrpVehicle> rebalancableVehicles) {
		// TODO enable different methods for real time target generation by adding
		// switch and corresponding parameter entry in the parameter file

		// First implementation: Simply evenly distribute the rebalancable (i.e. idling
		// and have enough service time) accross the network
		int numAvailableVehicles = (int) rebalancableVehicles.count();
		int targetValue = (int) Math.floor(numAvailableVehicles / zonalSystem.getZones().keySet().size());
		for (String z : zonalSystem.getZones().keySet()) {
			targetMap.put(z, targetValue);
		}
	}

}
