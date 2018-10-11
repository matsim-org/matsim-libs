package org.matsim.core.mobsim.qsim.changeeventsengine;

import static org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsModule.NETWORK_CHANGE_EVENTS_ENGINE_NAME;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

public class NetworkChangeEventsQSimComponentsConfigurator implements QSimComponentsConfigurator {
	final private Config config;

	public NetworkChangeEventsQSimComponentsConfigurator(Config config) {
		this.config = config;
	}

	@Override
	public void configure(QSimComponents components) {
		if (config.network().isTimeVariantNetwork()) {
			components.addNamedComponent(NETWORK_CHANGE_EVENTS_ENGINE_NAME);
		}
	}
}
