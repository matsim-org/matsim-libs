package org.matsim.core.mobsim.qsim.changeeventsengine;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

import static org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsModule.NETWORK_CHANGE_EVENTS_ENGINE_NAME;

public class NetworkChangeEventsQSimComponentsConfigurator implements QSimComponentsConfigurator {
	final private Config config;

	public NetworkChangeEventsQSimComponentsConfigurator(Config config) {
		this.config = config;
	}

	@Override
	public void configure(QSimComponents components) {
		if (config.network().isTimeVariantNetwork()) {
			components.activeMobsimEngines.add(NETWORK_CHANGE_EVENTS_ENGINE_NAME);
		}
	}
}
