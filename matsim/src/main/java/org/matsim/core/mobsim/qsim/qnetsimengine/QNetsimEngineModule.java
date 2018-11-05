package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class QNetsimEngineModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "NetsimEngine";

	@Override
	protected void configureQSim() {
		bind(QNetsimEngine.class).asEagerSingleton();
		bind(VehicularDepartureHandler.class).toProvider(QNetsimEngineDepartureHandlerProvider.class)
				.asEagerSingleton();

		addNamedComponent(VehicularDepartureHandler.class, COMPONENT_NAME);
		addNamedComponent(QNetsimEngine.class, COMPONENT_NAME);
	}
}
