package org.matsim.contrib.drt.optimizer.rebalancing.demandestimator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Triple;
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

public class NetDepartureReplenishDemandEstimator implements PassengerRequestScheduledEventHandler,
		DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler {

	private final DrtZonalSystem zonalSystem;
	private final String mode;
	private final int timeBinSize;
	private final Map<Double, Map<DrtZone, MutableInt>> currentZoneNetDepartureMap = new HashMap<>();
	private final Map<Double, Map<DrtZone, MutableInt>> previousZoneNetDepartureMap = new HashMap<>();
	private final Map<Id<Request>, Triple<Double, DrtZone, DrtZone>> potentialDrtTripsMap = new HashMap<>();
	private static final MutableInt ZERO = new MutableInt(0);

	public NetDepartureReplenishDemandEstimator(DrtZonalSystem zonalSystem, DrtConfigGroup drtCfg,
			FeedforwardRebalancingStrategyParams strategySpecificParams) {
		this.zonalSystem = zonalSystem;
		mode = drtCfg.getMode();
		timeBinSize = strategySpecificParams.getTimeBinSize();

	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (event.getMode().equals(mode)) {
			// If a request is rejected, remove the request info from the temporary storage place
			potentialDrtTripsMap.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		// Here, we get a potential DRT trip. We will first note it down in the
		// temporary data base (Potential DRT Trips Map)
		if (event.getMode().equals(mode)) {
			double timeBin = Math.floor(event.getTime() / timeBinSize);
			DrtZone departureZoneId = zonalSystem.getZoneForLinkId(event.getFromLinkId());
			DrtZone arrivalZoneId = zonalSystem.getZoneForLinkId(event.getToLinkId());
			potentialDrtTripsMap.put(event.getRequestId(), Triple.of(timeBin, departureZoneId, arrivalZoneId));
		}
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		// When the request is scheduled (i.e. accepted), add this travel information to
		// the database;
		// Then remove the travel information from the potential trips Map
		if (event.getMode().equals(mode)) {
			Triple<Double, DrtZone, DrtZone> triple = potentialDrtTripsMap.remove(event.getRequestId());
			double timeBin = triple.getLeft();
			DrtZone departureZone = triple.getMiddle();
			DrtZone arrivalZone = triple.getRight();

			var zoneNetDepartureMapSlice = currentZoneNetDepartureMap.computeIfAbsent(timeBin, t -> new HashMap<>());
			zoneNetDepartureMapSlice.computeIfAbsent(departureZone, z -> new MutableInt()).increment();
			zoneNetDepartureMapSlice.computeIfAbsent(arrivalZone, z -> new MutableInt()).decrement();
		}
	}

	public void update(int iteration) {
		previousZoneNetDepartureMap.clear();
		previousZoneNetDepartureMap.putAll(currentZoneNetDepartureMap);
		currentZoneNetDepartureMap.clear();
	}

	public ToDoubleFunction<DrtZone> getExpectedDemandForTimeBin(double timeBin) {
		Map<DrtZone, MutableInt> expectedDemandForTimeBin = previousZoneNetDepartureMap.getOrDefault(timeBin,
				Collections.emptyMap());
		return zone -> expectedDemandForTimeBin.getOrDefault(zone, ZERO).intValue();
	}
}
