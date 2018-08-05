package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class QNetsimEngineModule extends AbstractQSimModule {
	public final static String NETSIM_ENGINE_NAME = "NetsimEngine";

	@Override
	protected void configureQSim() {
		bind(QNetsimEngine.class).asEagerSingleton();
		bind(VehicularDepartureHandler.class).toProvider(QNetsimEngineDepartureHandlerProvider.class)
				.asEagerSingleton();

		bindDepartureHandler(NETSIM_ENGINE_NAME).to(VehicularDepartureHandler.class);
		bindMobsimEngine(NETSIM_ENGINE_NAME).to(QNetsimEngine.class);
	}
}
