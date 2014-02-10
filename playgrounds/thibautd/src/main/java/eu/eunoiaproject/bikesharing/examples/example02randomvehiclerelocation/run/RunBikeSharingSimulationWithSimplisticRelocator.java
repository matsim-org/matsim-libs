/* *********************************************************************** *
 * project: org.matsim.*
 * RunBikeSharingSimulationWithSimplisticRelocator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.examples.example02randomvehiclerelocation.run;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;

import eu.eunoiaproject.bikesharing.examples.example02randomvehiclerelocation.qsim.BikeSharingWithSimplisticRelocationQSimFactory;
import eu.eunoiaproject.bikesharing.examples.example02randomvehiclerelocation.qsim.SimplisticRelocatorManagerEngine;
import eu.eunoiaproject.bikesharing.framework.router.BikeSharingTripRouterFactory;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingScenarioUtils;

/**
 * @author thibautd
 */
public class RunBikeSharingSimulationWithSimplisticRelocator {
	// this increases the level of output of the relocating engine for debugging
	private static final boolean TRACE_RELOCATION = false;
	
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];
		
		if ( TRACE_RELOCATION ) Logger.getLogger( SimplisticRelocatorManagerEngine.class ).setLevel( Level.TRACE );

		final Scenario sc = BikeSharingScenarioUtils.loadScenario( configFile );
		final Controler controler = new Controler( sc );

		controler.setTripRouterFactory( new BikeSharingTripRouterFactory( sc ) );
		controler.setMobsimFactory(
				new BikeSharingWithSimplisticRelocationQSimFactory(
					10 ) );

		controler.run();
	}
}

