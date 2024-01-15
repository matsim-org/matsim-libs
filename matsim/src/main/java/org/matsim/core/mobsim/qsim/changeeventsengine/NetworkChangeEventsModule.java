
/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkChangeEventsModule.java
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

 package org.matsim.core.mobsim.qsim.changeeventsengine;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class NetworkChangeEventsModule extends AbstractQSimModule {
	public final static String NETWORK_CHANGE_EVENTS_ENGINE_NAME = "NetworkChangeEventsEngine";
	
	@Override
	protected void configureQSim() {
		bind(NetworkChangeEventsEngine.class).asEagerSingleton();
		addQSimComponentBinding( NETWORK_CHANGE_EVENTS_ENGINE_NAME ).to( NetworkChangeEventsEngine.class );
	}
}
