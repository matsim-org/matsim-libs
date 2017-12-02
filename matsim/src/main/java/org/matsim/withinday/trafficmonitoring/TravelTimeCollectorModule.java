package org.matsim.withinday.trafficmonitoring;

import org.matsim.core.controler.AbstractModule;

public class TravelTimeCollectorModule extends AbstractModule {
	@Override
	public void install() {
		bind(TravelTimeCollector.class);
		addEventHandlerBinding().to(TravelTimeCollector.class);
		bindNetworkTravelTime().to(TravelTimeCollector.class);
		// yyyyyy also needs to be bound as mobsim listener.  There is probably a reason
		// why this is not added here, but could someone please explain?  thx.  kai, dec'17
	}
}
