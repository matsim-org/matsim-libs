package org.matsim.withinday.mobsim;

import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

public class WithinDayQSimModule extends AbstractQSimModule {
	public static String WITHIN_DAY_ENGINE_NAME = "WithinDayEngine";
	public static String WITHIN_TRAVEL_TIME_NAME = "WithinDayTravelTime";
	public static String FIXED_ORDER_SIMULATION_LISTENER = "FixedOrderSimulationListener";

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

		addMobsimListener(FIXED_ORDER_SIMULATION_LISTENER).to(FixedOrderSimulationListener.class);
		addMobsimListener(WITHIN_TRAVEL_TIME_NAME).to(WithinDayTravelTime.class);
		addMobsimEngine(WITHIN_DAY_ENGINE_NAME).to(WithinDayEngine.class);
	}
}
