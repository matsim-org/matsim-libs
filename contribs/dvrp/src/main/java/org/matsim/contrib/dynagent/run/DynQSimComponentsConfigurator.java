package org.matsim.contrib.dynagent.run;

import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

public class DynQSimComponentsConfigurator implements QSimComponentsConfigurator {
	public void configure(QSimComponents components) {
		components.activeMobsimEngines.remove(ActivityEngineModule.ACTIVITY_ENGINE_NAME);
		components.activeMobsimEngines.add(DynActivityEngineModule.DYN_ACTIVITY_ENGINE_NAME);

		components.activeActivityHandlers.remove(ActivityEngineModule.ACTIVITY_ENGINE_NAME);
		components.activeActivityHandlers.add(DynActivityEngineModule.DYN_ACTIVITY_ENGINE_NAME);

		components.activeAgentSources.add(DynAgentSourceModule.DYN_AGENT_SOURCE_NAME);
	}
}
