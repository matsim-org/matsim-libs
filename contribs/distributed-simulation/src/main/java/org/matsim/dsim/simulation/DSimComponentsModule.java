package org.matsim.dsim.simulation;

import com.google.inject.Singleton;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.TeleportationModule;
import org.matsim.dsim.simulation.net.*;
import org.matsim.dsim.simulation.pt.DistributedPtEngine;

public class DSimComponentsModule extends AbstractQSimModule {

	@Override
	protected void configureQSim() {

		bind(SimStepMessaging.class).in(Singleton.class);
		bind(AgentSourcesContainer.class).in(Singleton.class);

		bind(DistributedTeleportationEngine.class).in(Singleton.class);

		addQSimComponentBinding(TeleportationModule.COMPONENT_NAME).to(DistributedTeleportationEngine.class);


		if (getConfig().transit().isUseTransit()) {
			bind(Wait2Link.class).to(DistributedPtEngine.class).in(Singleton.class);
		} else {
			bind(Wait2Link.class).to(DefaultWait2Link.class).in(Singleton.class);
		}

	}

}
