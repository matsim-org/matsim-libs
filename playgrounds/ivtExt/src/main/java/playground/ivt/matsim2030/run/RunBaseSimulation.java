/* *********************************************************************** *
 * project: org.matsim.*
 * RunBaseSimulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ivt.matsim2030.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ivt.matsim2030.Matsim2030Utils;

/**
 * @author thibautd
 */
public class RunBaseSimulation {
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		// This allows to get a log file containing the log messages happening
		// before controler init.
		OutputDirectoryLogging.catchLogEntries();

		final Config config = Matsim2030Utils.loadConfig( configFile );
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		Matsim2030Utils.connectFacilitiesWithLinks( scenario );

		final Controler controler = new Controler( scenario );

		Matsim2030Utils.initializeLocationChoice( controler );
		controler.setTripRouterFactory(
				Matsim2030Utils.createTripRouterFactory(
					scenario ) );
		
		// Code from Alex called (i) Controler.setOverrideFiles( true ) and
		// (ii) Controler.setCreateGraphs( true ), but
		// - (i) is wrong. Whatever you expect it to do, it does something different.
		// - (ii) sets a config option, and config options should be set from config, not code.
		// So I do not call them here.
		controler.run();
	}
}

