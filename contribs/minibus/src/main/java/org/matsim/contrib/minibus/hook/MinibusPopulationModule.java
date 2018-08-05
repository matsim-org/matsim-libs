package org.matsim.contrib.minibus.hook;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class MinibusPopulationModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		bind(PopulationAgentSource.class).asEagerSingleton();

		if (getConfig().transit().isUseTransit()) {
			bind(AgentFactory.class).to(PTransitAgentFactory.class).asEagerSingleton();
		} else {
			bind(AgentFactory.class).to(DefaultAgentFactory.class).asEagerSingleton();
		}

		bindAgentSource(PopulationModule.POPULATION_AGENT_SOURCE_NAME).to(PopulationAgentSource.class);
	}

	@Provides
	@Singleton
	PTransitAgentFactory provideAgentFactory(Netsim netsim) {
		return new PTransitAgentFactory(netsim);
	}
}
