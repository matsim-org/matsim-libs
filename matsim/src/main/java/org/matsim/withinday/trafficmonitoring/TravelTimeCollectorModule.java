package org.matsim.withinday.trafficmonitoring;

import org.matsim.core.controler.AbstractModule;

import javax.inject.Singleton;

public class TravelTimeCollectorModule extends AbstractModule {
	
	public TravelTimeCollectorModule(){}
	
	@Override
	public void install() {
		bind(TravelTimeCollector.class).in(Singleton.class);
		addEventHandlerBinding().to(TravelTimeCollector.class);
		bindNetworkTravelTime().to(TravelTimeCollector.class);
		// yyyyyy also needs to be bound as mobsim listener.  There is maybe a reason
		// why this is not added here, but could someone please explain?  thx.  kai, dec'17
		// Trying it out:
		addMobsimListenerBinding().to(TravelTimeCollector.class) ;
	}
}
