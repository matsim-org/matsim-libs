package org.matsim.contrib.dynagent.run;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class DynActivityEngineModule extends AbstractQSimModule {
	public final static String DYN_ACTIVITY_ENGINE_NAME = "DynActivityEngine";

	@Override
	protected void configureQSim() {
		bind(DynActivityEngine.class).asEagerSingleton();

		addMobsimEngine(DYN_ACTIVITY_ENGINE_NAME).to(DynActivityEngine.class);
		addActivityHandler(DYN_ACTIVITY_ENGINE_NAME).to(DynActivityEngine.class);
	}
}
