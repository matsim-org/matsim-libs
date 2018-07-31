package org.matsim.contrib.dvrp.run;

import org.matsim.contrib.dvrp.passenger.PassengerEnginePlugin;
import org.matsim.contrib.dynagent.run.DynAgentSourcePlugin;
import org.matsim.contrib.dynagent.run.DynAgentQSimComponentsConfigurator;
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
		new DynAgentQSimComponentsConfigurator().configure(components);

		if (addPassengerEngine) {
			components.activeMobsimEngines.add(PassengerEnginePlugin.PASSENGER_ENGINE_NAME);
			components.activeDepartureHandlers.add(PassengerEnginePlugin.PASSENGER_ENGINE_NAME);
		}

		components.activeAgentSources.add(DynAgentSourcePlugin.DYN_AGENT_SOURCE_NAME);
	}
}
