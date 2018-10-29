package org.matsim.core.mobsim.qsim.components;

public class DefaultQSimComponentsConfigurator implements QSimComponentsConfigurator {
	@Override
	public void configure(QSimComponentsConfig components) {
		components.clear();
		QSimComponentsConfigGroup.DEFAULT_COMPONENTS.forEach(components::addNamedComponent);
	}
}
