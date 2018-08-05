package org.matsim.core.mobsim.qsim.pt;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class TransitEngineModule extends AbstractQSimModule {
	public final static String TRANSIT_ENGINE_NAME = "TransitEngine";

	@Override
	protected void configureQSim() {
		bind(TransitQSimEngine.class).asEagerSingleton();

		bindDepartureHandler(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
		bindAgentSource(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
		bindMobsimEngine(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
	}
}
