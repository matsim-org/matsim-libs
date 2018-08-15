package org.matsim.core.mobsim.qsim.components;

public class DefaultQSimComponentsConfigurator implements QSimComponentsConfigurator {
	@Override
	public void configure(QSimComponents components) {
		components.activeMobsimEngines.clear();
		components.activeActivityHandlers.clear();
		components.activeDepartureHandlers.clear();
		components.activeAgentSources.clear();
		components.activeMobsimListeners.clear();

		components.activeMobsimEngines.addAll(QSimComponentsConfigGroup.DEFAULT_MOBSIM_ENGINES);
		components.activeActivityHandlers.addAll(QSimComponentsConfigGroup.DEFAULT_ACTIVITY_HANDLERS);
		components.activeDepartureHandlers.addAll(QSimComponentsConfigGroup.DEFAULT_DEPARTURE_HANDLERS);
		components.activeAgentSources.addAll(QSimComponentsConfigGroup.DEFAULT_AGENT_SOURCES);
		components.activeMobsimListeners.addAll(QSimComponentsConfigGroup.DEFAULT_MOBSIM_LISTENERS);
	}
}
