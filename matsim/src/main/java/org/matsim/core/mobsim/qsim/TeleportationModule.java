
/* *********************************************************************** *
 * project: org.matsim.*
 * TeleportationModule.java
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

 package org.matsim.core.mobsim.qsim;

public class TeleportationModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "TeleportationEngine";

	@Override
	protected void configureQSim() {
		bind(DefaultTeleportationEngine.class).asEagerSingleton();
		addQSimComponentBinding( COMPONENT_NAME ).to( DefaultTeleportationEngine.class );
	}
}
