package org.matsim.contrib.dynagent.run;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;

public class DynActivityEngineModule extends AbstractQSimModule {
	public final static String DYN_ACTIVITY_ENGINE_NAME = "DynActivityEngine";

	@Override
	protected void configureQSim() {
		bind(DynActivityEngine.class).asEagerSingleton();

		bindNamedMobsimEngine(DYN_ACTIVITY_ENGINE_NAME).to(DynActivityEngine.class);
		bindNamedActivityHandler(DYN_ACTIVITY_ENGINE_NAME).to(DynActivityEngine.class);
	}

	public static void configureComponents(QSimComponents components) {
		components.removeNamedComponent(ActivityEngineModule.COMPONENT_NAME);
		components.addNamedComponent(DYN_ACTIVITY_ENGINE_NAME);
	}
}
