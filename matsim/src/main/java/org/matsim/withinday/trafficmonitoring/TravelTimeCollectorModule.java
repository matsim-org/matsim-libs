package org.matsim.withinday.trafficmonitoring;

import org.matsim.core.controler.AbstractModule;

public class TravelTimeCollectorModule extends AbstractModule {
	@Override
	public void install() {
		bind(TravelTimeCollector.class);
		addMobsimListenerBinding().to(TravelTimeCollector.class);
		addEventHandlerBinding().to(TravelTimeCollector.class);
		bindNetworkTravelTime().to(TravelTimeCollector.class);
	}
}
