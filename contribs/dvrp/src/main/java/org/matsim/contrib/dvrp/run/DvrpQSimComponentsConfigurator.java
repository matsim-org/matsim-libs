package org.matsim.contrib.dvrp.run;

import org.matsim.core.mobsim.qsim.components.QSimComponents;

public class DvrpQSimComponentsConfigurator {
	final private boolean addPassengerEngine;

	public DvrpQSimComponentsConfigurator() {
		this(true);
	}

	public DvrpQSimComponentsConfigurator(boolean addPassengerEngine) {
		this.addPassengerEngine = addPassengerEngine;
	}

	public void configure(QSimComponents components) {
		components.activeMobsimEngines.remove("ActivityEngine");
		components.activeMobsimEngines.add("DynActivityEngine");

		components.activeActivityHandlers.remove("ActivityEngine");
		components.activeActivityHandlers.add("DynActivityEngine");

		if (addPassengerEngine) {
			components.activeMobsimEngines.add("PassengerEngine");
			components.activeDepartureHandlers.add("PassengerEngine");
		}

		components.activeAgentSources.add("VrpAgentSource");
	}
}
