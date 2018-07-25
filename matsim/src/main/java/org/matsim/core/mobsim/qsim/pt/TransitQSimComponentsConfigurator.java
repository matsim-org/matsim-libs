package org.matsim.core.mobsim.qsim.pt;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.components.QSimComponents;

public class TransitQSimComponentsConfigurator {
	final private Config config;

	public TransitQSimComponentsConfigurator(Config config) {
		this.config = config;
	}

	public void configure(QSimComponents components) {
		if (config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim()) {
			components.activeMobsimEngines.add("TransitEngine");
			components.activeDepartureHandlers.add("TransitEngine");
			components.activeAgentSources.add("TransitEngine");
		}
	}
}
