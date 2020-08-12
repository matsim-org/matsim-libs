package org.matsim.contrib.drt.optimizer.rebalancing.adaptiveRealTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
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
	private static final Logger log = Logger.getLogger(AdaptiveRealTimeRebalncingStrategy.class);
	
	private final DrtZonalSystem zonalSystem;
	private final Fleet fleet;
	private final MinCostRelocationCalculator minCostRelocationCalculator;
	private final AdaptiveRealTimeRebalancingParams params;

	private final List<Pair<String, Integer>> supply = new ArrayList<>();
	private final List<Pair<String, Integer>> demand = new ArrayList<>();
	private final Map<String, Integer> targetMap = new HashMap<>();
	private final Set<String> activeZones = new HashSet<>();

	public AdaptiveRealTimeRebalncingStrategy(DrtZonalSystem zonalSystem, Fleet fleet,
			MinCostRelocationCalculator minCostRelocationCalculator, AdaptiveRealTimeRebalancingParams params,
			InactiveZoneIdentifier inactiveZoneIdentifier) {
		this.zonalSystem = zonalSystem;
		this.fleet = fleet;
		this.minCostRelocationCalculator = minCostRelocationCalculator;
		this.params = params;

		activeZones.addAll(zonalSystem.getZones().keySet());
		activeZones.removeAll(inactiveZoneIdentifier.getInactiveZone());
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		log.info("Rebalance fleet now: Adaptive Real Time Rebalancing Strategy is used");

		// initialization each time this function is called
		VehicleInfoCollector vehicleInfoCollector = new VehicleInfoCollector(fleet, zonalSystem);
		supply.clear();
		demand.clear();
		targetMap.clear();
		List<? extends DvrpVehicle> rebalancableVehiclesList = rebalancableVehicles.collect(Collectors.toList());
		int numAvailableVehicles = rebalancableVehiclesList.size();

		// Get idling vehicles in each zone
		Map<String, List<DvrpVehicle>> rebalancableVehiclesPerZone = vehicleInfoCollector
				.groupRebalancableVehicles(rebalancableVehiclesList.stream(), time, params.getMinServiceTime());
		if (rebalancableVehiclesPerZone.isEmpty()) {
			log.info("There is no rebalancable Vehicle at this moment!");
			return Collections.emptyList();
		}

		// Get soon idle vehicle for each zone
		Map<String, List<DvrpVehicle>> soonIdleVehiclesPerZone = vehicleInfoCollector.groupSoonIdleVehicles(time,
				params.getMaxTimeBeforeIdle(), params.getMinServiceTime());

		// calculate real time target of each zone
		calculateRealTimeRebalanceTarget(targetMap, fleet, activeZones, numAvailableVehicles);

		// calculate supply and demand for each zone
		for (String z : zonalSystem.getZones().keySet()) {
			int rebalancable = rebalancableVehiclesPerZone.getOrDefault(z, Collections.emptyList()).size();
			int soonIdle = soonIdleVehiclesPerZone.getOrDefault(z, Collections.emptyList()).size();
			int target = targetMap.getOrDefault(z, 0);
			int delta = Math.min(rebalancable + soonIdle - target, rebalancable);
			if (delta < 0) {
				demand.add(Pair.of(z, -delta));
			} else if (delta > 0) {
				supply.add(Pair.of(z, delta));
			}
		}

		// calculate using min cost flow method
		return minCostRelocationCalculator.calcRelocations(supply, demand, rebalancableVehiclesPerZone);
	}

	private void calculateRealTimeRebalanceTarget(Map<String, Integer> targetMap, Fleet fleet,
			Set<String> activeZones, int numAvailableVehicles) {
		// TODO enable different methods for real time target generation by adding
		// switch and corresponding parameter entry in the parameter file

		// First implementation: Simply evenly distribute the rebalancable (i.e. idling
		// and have enough service time) accross the network
		int targetValue = (int) Math.floor(numAvailableVehicles / activeZones.size());
		if (targetValue < 1)
			log.warn("There is too few idling vehicles to perform rebalance! No vehicles will be assigned to rebalance task at this period");
		for (String z : activeZones) {
			targetMap.put(z, targetValue);
		}
	}

}
