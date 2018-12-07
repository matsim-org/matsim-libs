package org.matsim.contrib.minibus.hook;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class MinibusPopulationModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		if (getConfig().transit().isUseTransit()) {
			bind(AgentFactory.class).to(PTransitAgentFactory.class).asEagerSingleton();
		}
	}

	@Provides
	@Singleton
	PTransitAgentFactory provideAgentFactory(Netsim netsim) {
		return new PTransitAgentFactory(netsim);
	}
}
