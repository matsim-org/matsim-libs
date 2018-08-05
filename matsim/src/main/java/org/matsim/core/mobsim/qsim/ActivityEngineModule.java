package org.matsim.core.mobsim.qsim;

public class ActivityEngineModule extends AbstractQSimModule {
	public static final String ACTIVITY_ENGINE_NAME = "ActivityEngine";

	@Override
	protected void configureQSim() {
		bind(ActivityEngine.class).asEagerSingleton();

		bindActivityHandler(ACTIVITY_ENGINE_NAME).to(ActivityEngine.class);
		bindMobsimEngine(ACTIVITY_ENGINE_NAME).to(ActivityEngine.class);
	}
}
