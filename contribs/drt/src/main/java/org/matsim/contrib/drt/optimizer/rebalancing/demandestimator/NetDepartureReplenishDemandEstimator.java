package org.matsim.contrib.drt.optimizer.rebalancing.demandestimator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;

public class NetDepartureReplenishDemandEstimator implements ZonalDemandEstimator,
		PassengerRequestScheduledEventHandler, DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler {

	private final DrtZonalSystem zonalSystem;
	private final String mode;
	private final int timeBinSize;
	private final Map<Double, Map<DrtZone, MutableInt>> zoneNetDepartureMap = new HashMap<>();
	private final Map<Id<Person>, Triple<Double, DrtZone, DrtZone>> potentialDRTTripsMap = new HashMap<>();
	
	public NetDepartureReplenishDemandEstimator(DrtZonalSystem zonalSystem, DrtConfigGroup drtCfg,
			FeedforwardRebalancingStrategyParams strategySpecificParams) {
		this.zonalSystem = zonalSystem;
		mode = drtCfg.getMode();
		timeBinSize = strategySpecificParams.getTimeBinSize();

	}

	@Override
	public ToIntFunction<DrtZone> getExpectedDemandForTimeBin(double time) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		// TODO Auto-generated method stub

	}

}
