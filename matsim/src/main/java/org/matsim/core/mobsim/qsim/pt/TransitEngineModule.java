package org.matsim.core.mobsim.qsim.pt;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class TransitEngineModule extends AbstractQSimModule {
	public final static String TRANSIT_ENGINE_NAME = "TransitEngine";

	@Override
	protected void configureQSim() {
		bind(TransitQSimEngine.class).asEagerSingleton();

		addDepartureHandler(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
		addAgentSource(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
		addMobsimEngine(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
	}
}
