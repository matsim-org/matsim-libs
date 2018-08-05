package org.matsim.contrib.dynagent.run;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class DynActivityEngineModule extends AbstractQSimModule {
	public final static String DYN_ACTIVITY_ENGINE_NAME = "DynActivityEngine";

	@Override
	protected void configureQSim() {
		bind(DynActivityEngine.class).asEagerSingleton();

		addMobsimEngineBinding(DYN_ACTIVITY_ENGINE_NAME).to(DynActivityEngine.class);
		addActivityHandlerBinding(DYN_ACTIVITY_ENGINE_NAME).to(DynActivityEngine.class);
	}
}
