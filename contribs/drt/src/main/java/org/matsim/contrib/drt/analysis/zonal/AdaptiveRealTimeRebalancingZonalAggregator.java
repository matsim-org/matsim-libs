package org.matsim.contrib.drt.analysis.zonal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.dvrp.fleet.Fleet;

/**
 * This class distribute the available (idling) vehicles evenly accross the
 * network. The algorithm is based on the Adaptive Real-Time Rebalancing Policy
 * in AMoDeus
 *
 * TODO: fix the binding problem
 * "Explicit bindings are required and org.matsim.contrib.dvrp.fleet.Fleet 
 * annotated with @org.matsim.contrib.dvrp.run.DvrpMode(value=drt) is not explicitly bound."
 *
 * @author Chengqi Lu
 */
public class AdaptiveRealTimeRebalancingZonalAggregator implements ZonalDemandAggregator {

	private final Map<String, MutableInt> vehiclesPerZone = new HashMap<>();
	private static final MutableInt ZERO = new MutableInt(0);

	public AdaptiveRealTimeRebalancingZonalAggregator(@NotNull DrtZonalSystem zonalSystem, @NotNull Fleet fleet,
			@NotNull DrtScheduleInquiry scheduleInquiry) {
		long numIdlingVehicles = fleet.getVehicles().values().stream().filter(scheduleInquiry::isIdle).count();
		compute(zonalSystem, (int) numIdlingVehicles);
	}

	private void compute(@NotNull DrtZonalSystem zonalSystem, int numIdlingVehicles) {
		vehiclesPerZone.clear();

		for (String zone : zonalSystem.getZones().keySet()) {
			vehiclesPerZone.put(zone, new MutableInt(Math.floor(numIdlingVehicles / zonalSystem.getZones().size())));
		}
	}

	@Override
	public ToIntFunction<String> getExpectedDemandForTimeBin(double time) {
		return zoneId -> vehiclesPerZone.getOrDefault(zoneId, ZERO).intValue();
	}

}
