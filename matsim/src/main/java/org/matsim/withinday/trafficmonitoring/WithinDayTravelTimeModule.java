package org.matsim.withinday.trafficmonitoring;

import org.matsim.core.controler.AbstractModule;

import javax.inject.Singleton;

public class WithinDayTravelTimeModule extends AbstractModule {
	
	public WithinDayTravelTimeModule(){}
	
	@Override
	public void install() {
		bind(WithinDayTravelTime.class).in(Singleton.class);
		addEventHandlerBinding().to(WithinDayTravelTime.class);
		bindNetworkTravelTime().to(WithinDayTravelTime.class);
		// yyyyyy also needs to be bound as mobsim listener.  There is maybe a reason
		// why this is not added here, but could someone please explain?  thx.  kai, dec'17
		// Trying it out:
		addMobsimListenerBinding().to(WithinDayTravelTime.class) ;
	}
}
