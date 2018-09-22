package org.matsim.withinday.mobsim;

import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

public class WithinDayQSimModule extends AbstractQSimModule {
	public static final String WITHIN_DAY_ENGINE_NAME = "WithinDayEngine";
	public static final String WITHIN_TRAVEL_TIME_NAME = "WithinDayTravelTime";
	public static final String FIXED_ORDER_SIMULATION_LISTENER = "FixedOrderSimulationListener";

	private final WithinDayEngine withinDayEngine;
	private final FixedOrderSimulationListener fixedOrderSimulationListener;
	private final WithinDayTravelTime withinDayTravelTime;

	public WithinDayQSimModule(WithinDayEngine withinDayEngine,
			FixedOrderSimulationListener fixedOrderSimulationListener, WithinDayTravelTime withinDayTravelTime) {
		this.withinDayEngine = withinDayEngine;
		this.fixedOrderSimulationListener = fixedOrderSimulationListener;
		this.withinDayTravelTime = withinDayTravelTime;
	}

	@Override
	protected void configureQSim() {
		bind(WithinDayEngine.class).toInstance(withinDayEngine);
		bind(FixedOrderSimulationListener.class).toInstance(fixedOrderSimulationListener);
		bind(WithinDayTravelTime.class).toInstance(withinDayTravelTime);

		bindMobsimListener(FIXED_ORDER_SIMULATION_LISTENER).to(FixedOrderSimulationListener.class);
		bindMobsimListener(WITHIN_TRAVEL_TIME_NAME).to(WithinDayTravelTime.class);
		bindMobsimEngine(WITHIN_DAY_ENGINE_NAME).to(WithinDayEngine.class);
	}

	static public void configureComponents(QSimComponents components) {
		components.activeMobsimEngines.add(WITHIN_DAY_ENGINE_NAME);
		components.activeMobsimListeners.add(FIXED_ORDER_SIMULATION_LISTENER);
		components.activeMobsimListeners.add(WITHIN_TRAVEL_TIME_NAME);
	}
}
