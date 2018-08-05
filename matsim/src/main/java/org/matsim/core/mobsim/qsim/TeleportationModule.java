package org.matsim.core.mobsim.qsim;

public class TeleportationModule extends AbstractQSimModule {
	public final static String TELEPORATION_ENGINE_NAME = "TeleportationEngine";

	@Override
	protected void configureQSim() {
		bind(DefaultTeleportationEngine.class).asEagerSingleton();
		addMobsimEngineBinding(TELEPORATION_ENGINE_NAME).to(DefaultTeleportationEngine.class);
	}
}
