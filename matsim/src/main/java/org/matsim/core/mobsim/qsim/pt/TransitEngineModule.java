package org.matsim.core.mobsim.qsim.pt;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class TransitEngineModule extends AbstractQSimModule {
	public final static String TRANSIT_ENGINE_NAME = "TransitEngine";

	@Override
	protected void configureQSim() {
		bind(TransitQSimEngine.class).asEagerSingleton();

		addDepartureHandlerBinding(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
		addAgentSourceBinding(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
		addMobsimEngineBinding(TRANSIT_ENGINE_NAME).to(TransitQSimEngine.class);
	}
}
