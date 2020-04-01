
/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityEngineModule.java
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

import com.google.inject.Singleton;

public class ActivityEngineModule extends AbstractQSimModule {
	public static final String COMPONENT_NAME = "ActivityEngine";

	@Override
	protected void configureQSim() {
		bind( ActivityEngineDefaultImpl.class ).in( Singleton.class );
		addQSimComponentBinding( COMPONENT_NAME ).to( ActivityEngineDefaultImpl.class );
	}
}
