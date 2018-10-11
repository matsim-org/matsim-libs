package org.matsim.core.mobsim.qsim.pt;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class TransitEngineModule extends AbstractQSimModule {
	public final static String TRANSIT_ENGINE_NAME = "TransitEngine";

	@Override
	protected void configureQSim() {
		bind(TransitQSimEngine.class).asEagerSingleton();

		bindNamedDepartureHandler(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
		bindNamedAgentSource(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
		bindNamedMobsimEngine(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
	}
}
