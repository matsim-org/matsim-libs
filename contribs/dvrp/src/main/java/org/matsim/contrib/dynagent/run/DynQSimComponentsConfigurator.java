package org.matsim.contrib.dynagent.run;

import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.components.QSimComponents;

public class DynQSimComponentsConfigurator {
	public void configure(QSimComponents components) {
		components.activeMobsimEngines.remove(ActivityEnginePlugin.ACTIVITY_ENGINE_NAME);
		components.activeMobsimEngines.add(DynActivityEnginePlugin.DYN_ACTIVITY_ENGINE_NAME);

		components.activeActivityHandlers.remove(ActivityEnginePlugin.ACTIVITY_ENGINE_NAME);
		components.activeActivityHandlers.add(DynActivityEnginePlugin.DYN_ACTIVITY_ENGINE_NAME);

		components.activeAgentSources.add(DynAgentSourcePlugin.DYN_AGENT_SOURCE_NAME);
	}
}
