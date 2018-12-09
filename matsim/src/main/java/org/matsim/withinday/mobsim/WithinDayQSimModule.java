package org.matsim.withinday.mobsim;

import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentKeysRegistry;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

public class WithinDayQSimModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "WithinDay";
	public final static String FIXED_ORDER_LISTENER_COMPONENT_NAME = "FixedOrderSimulationListener";

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

		this.addQSimComponentBinding( FIXED_ORDER_LISTENER_COMPONENT_NAME ).to( FixedOrderSimulationListener.class ) ;
		this.addQSimComponentBinding( COMPONENT_NAME ).to( WithinDayTravelTime.class ) ;
		this.addQSimComponentBinding( COMPONENT_NAME ).to( WithinDayEngine.class ) ;
	}

	static public void configureComponents( QSimComponentKeysRegistry components ) {
		components.addNamedAnnotation(COMPONENT_NAME );
		components.addNamedAnnotation(FIXED_ORDER_LISTENER_COMPONENT_NAME );
	}
}
