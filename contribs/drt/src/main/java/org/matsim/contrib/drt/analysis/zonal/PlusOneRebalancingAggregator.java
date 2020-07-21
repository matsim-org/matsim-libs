package org.matsim.contrib.drt.analysis.zonal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;

/**
 * This aggregator use real time demand data to create the rebalance target.
 * Whenever there is a demand in a zone, the aggregator will create one
 * additional rebalance target in that zone. The algorithm is based on the Plus
 * one Rebalancing Policy in AMoDeus.
 * 
 * Important: to make the algorithm work properly, the rebalance interval should
 * be very small (e.g. 1 minute)
 *
 * TODO: Test
 *
 * @author Chengqi Lu
 */
public class PlusOneRebalancingAggregator implements ZonalDemandAggregator, ActivityStartEventHandler {
	private final Map<String, MutableInt> vehiclesPerZone = new HashMap<>();
	private static final MutableInt ZERO = new MutableInt(0);

	public PlusOneRebalancingAggregator() {

	}

	@Override
	public ToIntFunction<String> getExpectedDemandForTimeBin(double time) {
		return zoneId -> vehiclesPerZone.getOrDefault(zoneId, ZERO).intValue();
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		event.getActType();

	}

}
