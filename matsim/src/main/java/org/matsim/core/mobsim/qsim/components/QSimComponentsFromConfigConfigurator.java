package org.matsim.core.mobsim.qsim.components;

import org.matsim.core.config.Config;

public class QSimComponentsFromConfigConfigurator implements QSimComponentsConfigurator {
	final private Config config;

	public QSimComponentsFromConfigConfigurator(Config config) {
		this.config = config;
	}

	@Override
	public void configure(QSimComponents components) {
		QSimComponentsConfigGroup componentsConfig = (QSimComponentsConfigGroup) config.getModules()
				.get(QSimComponentsConfigGroup.GROUP_NAME);

		if (componentsConfig != null) {
			components.activeMobsimEngines.clear();
			components.activeActivityHandlers.clear();
			components.activeDepartureHandlers.clear();
			components.activeAgentSources.clear();
			components.activeMobsimListeners.clear();

			components.activeMobsimEngines.addAll(componentsConfig.getActiveMobsimEngines());
			components.activeActivityHandlers.addAll(componentsConfig.getActiveActivityHandlers());
			components.activeDepartureHandlers.addAll(componentsConfig.getActiveDepartureHandlers());
			components.activeAgentSources.addAll(componentsConfig.getActiveAgentSources());
			components.activeMobsimListeners.addAll(componentsConfig.getActiveMobsimListeners());
		}
	}
}
