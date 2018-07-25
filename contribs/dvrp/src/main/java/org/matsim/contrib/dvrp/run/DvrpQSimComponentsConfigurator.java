package org.matsim.contrib.dvrp.run;

import org.matsim.contrib.dynagent.run.DynAgentQSimComponentsConfiurator;
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
		new DynAgentQSimComponentsConfiurator().configure(components);

		if (addPassengerEngine) {
			components.activeMobsimEngines.add("PassengerEngine");
			components.activeDepartureHandlers.add("PassengerEngine");
		}

		components.activeAgentSources.add("VrpAgentSource");
	}
}
