package org.matsim.core.mobsim.qsim;

public class ActivityEngineModule extends AbstractQSimModule {
	public static final String COMPONENT_NAME = "ActivityEngine";

	@Override
	protected void configureQSim() {
		bind(ActivityEngine.class).asEagerSingleton();
		addNamedComponent(ActivityEngine.class, COMPONENT_NAME);
	}
}
