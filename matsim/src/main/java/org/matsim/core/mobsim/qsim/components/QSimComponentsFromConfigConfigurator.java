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
			components.clear();

			// TODO: Eventually, here a translation of strings to more specific annotations
			// could happen if we ever want a full config-configurable QSim.
			componentsConfig.getActiveComponents().forEach(components::addNamedComponent);
		}
	}
}
