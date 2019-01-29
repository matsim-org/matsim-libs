package org.matsim.core.trafficmonitoring;

import org.matsim.core.router.util.LinkToLinkTravelTime;

import javax.inject.Inject;
import javax.inject.Provider;

class ObservedLinkToLinkTravelTimes implements Provider<LinkToLinkTravelTime> {

	@Inject
	TravelTimeCalculator travelTimeCalculator;

	@Override
	public LinkToLinkTravelTime get() {
		return travelTimeCalculator.getLinkToLinkTravelTimes();
	}

}
