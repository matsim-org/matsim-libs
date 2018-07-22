package org.matsim.contrib.dynagent.run;

import org.matsim.core.mobsim.qsim.components.QSimComponents;

public class DynAgentQSimComponentsConfiurator {
	public void configure(QSimComponents components) {
		components.activeMobsimEngines.remove("ActivityEngine");
		components.activeMobsimEngines.add("DynActivityEngine");

		components.activeActivityHandlers.remove("ActivityEngine");
		components.activeActivityHandlers.add("DynActivityEngine");
	}
}
