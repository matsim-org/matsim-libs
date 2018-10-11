package org.matsim.core.mobsim.qsim;

public class TeleportationModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "TeleportationEngine";

	@Override
	protected void configureQSim() {
		bind(DefaultTeleportationEngine.class).asEagerSingleton();
		bindNamedMobsimEngine(COMPONENT_NAME).to(DefaultTeleportationEngine.class);
	}
}
