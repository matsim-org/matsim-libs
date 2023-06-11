
/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayQSimModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.withinday.mobsim;

import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
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

		addQSimComponentBinding( FIXED_ORDER_LISTENER_COMPONENT_NAME ).to( FixedOrderSimulationListener.class );
		addQSimComponentBinding( COMPONENT_NAME ).to( WithinDayTravelTime.class );
		addQSimComponentBinding( COMPONENT_NAME ).to( WithinDayEngine.class );
	}

	static public void configureComponents(QSimComponentsConfig components) {
		components.addNamedComponent(COMPONENT_NAME);
		components.addNamedComponent(FIXED_ORDER_LISTENER_COMPONENT_NAME);
	}
}
