package org.matsim.contrib.minibus.hook;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.utils.timing.TimeInterpretation;

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
	PTransitAgentFactory provideAgentFactory(Netsim netsim, TimeInterpretation timeInterpretation) {
		return new PTransitAgentFactory(netsim, timeInterpretation);
	}
}
