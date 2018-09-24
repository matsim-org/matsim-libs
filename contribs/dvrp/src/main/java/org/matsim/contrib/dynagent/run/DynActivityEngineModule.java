package org.matsim.contrib.dynagent.run;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;

public class DynActivityEngineModule extends AbstractQSimModule {
	public final static String DYN_ACTIVITY_ENGINE_NAME = "DynActivityEngine";

	@Override
	protected void configureQSim() {
		bind(DynActivityEngine.class).asEagerSingleton();

		bindMobsimEngine(DYN_ACTIVITY_ENGINE_NAME).to(DynActivityEngine.class);
		bindActivityHandler(DYN_ACTIVITY_ENGINE_NAME).to(DynActivityEngine.class);
	}

	public static void configureComponents(QSimComponents components) {
		components.activeMobsimEngines.remove(ActivityEngineModule.ACTIVITY_ENGINE_NAME);
		components.activeMobsimEngines.add(DynActivityEngineModule.DYN_ACTIVITY_ENGINE_NAME);

		components.activeActivityHandlers.remove(ActivityEngineModule.ACTIVITY_ENGINE_NAME);
		components.activeActivityHandlers.add(DynActivityEngineModule.DYN_ACTIVITY_ENGINE_NAME);
	}
}
