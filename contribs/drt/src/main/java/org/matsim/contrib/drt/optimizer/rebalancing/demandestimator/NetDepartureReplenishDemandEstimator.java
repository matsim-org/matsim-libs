package org.matsim.contrib.drt.optimizer.rebalancing.demandestimator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;

public class NetDepartureReplenishDemandEstimator
		implements PassengerRequestScheduledEventHandler, DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler {

	private record Trip(int timeBin, DrtZone fromZone, DrtZone toZone) {
	}

	private final DrtZonalSystem zonalSystem;
	private final String mode;
	private final int timeBinSize;
	private final Map<Integer, Map<DrtZone, Integer>> currentZoneNetDepartureMap = new HashMap<>();
	private final Map<Integer, Map<DrtZone, Integer>> previousZoneNetDepartureMap = new HashMap<>();
	private final Map<Id<Request>, Trip> potentialDrtTripsMap = new HashMap<>();

	public NetDepartureReplenishDemandEstimator(DrtZonalSystem zonalSystem, DrtConfigGroup drtCfg,
			FeedforwardRebalancingStrategyParams strategySpecificParams) {
		this.zonalSystem = zonalSystem;
		mode = drtCfg.getMode();
		timeBinSize = strategySpecificParams.timeBinSize;

	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (event.getMode().equals(mode)) {
			// Ignore rejected request.
			potentialDrtTripsMap.remove(event.getRequestId());
		}
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		if (event.getMode().equals(mode)) {
			// At the submission time, this is only a potential trip.
			int timeBin = (int)Math.floor(event.getTime() / timeBinSize);
			DrtZone departureZoneId = zonalSystem.getZoneForLinkId(event.getFromLinkId());
			DrtZone arrivalZoneId = zonalSystem.getZoneForLinkId(event.getToLinkId());
			potentialDrtTripsMap.put(event.getRequestId(), new Trip(timeBin, departureZoneId, arrivalZoneId));
		}
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		if (event.getMode().equals(mode)) {
			// A potential trip has been scheduled and needs to be considered in the departure maps.
			var trip = potentialDrtTripsMap.remove(event.getRequestId());

			var zoneNetDepartureMapSlice = currentZoneNetDepartureMap.computeIfAbsent(trip.timeBin, t -> new HashMap<>());
			zoneNetDepartureMapSlice.merge(trip.fromZone, 1, Integer::sum);
			zoneNetDepartureMapSlice.merge(trip.toZone, -1, Integer::sum);
		}
	}

	public void updateForNextIteration() {
		previousZoneNetDepartureMap.clear();
		previousZoneNetDepartureMap.putAll(currentZoneNetDepartureMap);
		currentZoneNetDepartureMap.clear();

		potentialDrtTripsMap.clear();
	}

	public ToDoubleFunction<DrtZone> getExpectedDemandForTimeBin(int timeBin) {
		var expectedDemandForTimeBin = previousZoneNetDepartureMap.getOrDefault(timeBin, Collections.emptyMap());
		return zone -> expectedDemandForTimeBin.getOrDefault(zone, 0);
	}
}
