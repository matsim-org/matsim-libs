package org.matsim.core.mobsim.qsim.changeeventsengine;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class NetworkChangeEventsModule extends AbstractQSimModule {
	public final static String NETWORK_CHANGE_EVENTS_ENGINE_NAME = "NetworkChangeEventsEngine";
	
	@Override
	protected void configureQSim() {
		bind(NewNetworkChangeEventsEngine.class).asEagerSingleton();
		bindMobsimEngine(NETWORK_CHANGE_EVENTS_ENGINE_NAME).to(NewNetworkChangeEventsEngine.class);
	}
}
