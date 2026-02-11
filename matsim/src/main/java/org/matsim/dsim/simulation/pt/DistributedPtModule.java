package org.matsim.dsim.simulation.pt;

import com.google.inject.Singleton;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;

/**
 * We register ourselfs as a QSimComponentsConfigurator, so that the DistributedPt engine is an active component and is added
 * to the SimProcess by the {@link org.matsim.dsim.simulation.SimProvider}. This mechanism seems to be deprecated but I don't
 * really know what to do instead.
 */
public class DistributedPtModule extends AbstractQSimModule implements QSimComponentsConfigurator{

	public static final String COMPONENT_NAME = "DistributedPtEngine";

	@Override
	protected void configureQSim() {
		bind(DistributedPtEngine.class).in(Singleton.class);
		addQSimComponentBinding(COMPONENT_NAME).to(DistributedPtEngine.class);
	}

	@Override
	public void configure(QSimComponentsConfig components) {
		components.addNamedComponent(COMPONENT_NAME);
	}
}
