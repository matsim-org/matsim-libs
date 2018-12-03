package org.matsim.core.mobsim.qsim;

public class ActivityEngineModule extends AbstractQSimModule {
	public static final String COMPONENT_NAME = "ActivityEngine";

	@Override
	protected void configureQSim() {
		bind(ActivityEngine.class).asEagerSingleton();
		this.addQSimComponentBinding( COMPONENT_NAME ).to( ActivityEngine.class ) ;
	}
}
