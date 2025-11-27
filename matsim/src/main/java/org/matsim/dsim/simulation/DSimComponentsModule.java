package org.matsim.dsim.simulation;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.TeleportationModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.dsim.simulation.net.DefaultWait2Link;
import org.matsim.dsim.simulation.net.Wait2Link;
import org.matsim.dsim.simulation.pt.DistributedPtEngine;
import org.matsim.pt.config.TransitConfigGroup;

public class DSimComponentsModule extends AbstractQSimModule {

	@Override
	protected void configureQSim() {

		bind(SimStepMessaging.class).in(Singleton.class);
		bind(AgentSourcesContainer.class).in(Singleton.class);

		bind(TeleportationEngine.class).to(DistributedTeleportationEngine.class).in(Singleton.class);
		addQSimComponentBinding(TeleportationModule.COMPONENT_NAME).to(DistributedTeleportationEngine.class);

		bind(PopulationAgentSource.class).asEagerSingleton();
		addQSimComponentBinding(PopulationModule.COMPONENT_NAME).to(PopulationAgentSource.class);

		if (getConfig().transit().isUseTransit()) {
			bind(Wait2Link.class).to(DistributedPtEngine.class).in(Singleton.class);
		} else {
			bind(Wait2Link.class).to(DefaultWait2Link.class).in(Singleton.class);
		}

		bind(QVehicleFactory.class).to(ScaledQVehicleFactory.class).in(Singleton.class);
	}

	@Provides
	@Singleton
	AgentFactory provideAgentFactory(TransitConfigGroup config, Netsim simulation, TimeInterpretation timeInterpretation) {
		if (config.isUseTransit()) {
			return new TransitAgentFactory(simulation, timeInterpretation);
		} else {
			return new DefaultAgentFactory(simulation, timeInterpretation);
		}
	}
}
