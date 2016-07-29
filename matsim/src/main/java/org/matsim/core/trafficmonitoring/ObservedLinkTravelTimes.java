package org.matsim.core.trafficmonitoring;

import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import javax.inject.Provider;

class ObservedLinkTravelTimes implements Provider<TravelTime> {

	@Inject
	TravelTimeCalculator travelTimeCalculator;

	@Override
	public TravelTime get() {
		return travelTimeCalculator.getLinkTravelTimes();
	}

}
