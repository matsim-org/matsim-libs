package org.matsim.core.mobsim.qsim;

public class ActivityEngineModule extends AbstractQSimModule {
	public static final String COMPONENT_NAME = "ActivityEngine";

	@Override
	protected void configureQSim() {
		bind( DefaultActivityEngine.class ).asEagerSingleton();
		addNamedComponent( DefaultActivityEngine.class, COMPONENT_NAME );
	}
}
