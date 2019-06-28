
/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioByConfigModule.java
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

 package org.matsim.core.scenario;


import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

public final class ScenarioByConfigModule extends AbstractModule {
	@Override
	public void install() {
		bind( ScenarioLoaderImpl.class ).toInstance( new ScenarioLoaderImpl( getConfig() ) );
		install( new ScenarioByInstanceModule( null ) );
	}

	@Provides
	@Singleton
	private Scenario createScenario( final ScenarioLoaderImpl loader ) {
		return loader.loadScenario();
	}
}
