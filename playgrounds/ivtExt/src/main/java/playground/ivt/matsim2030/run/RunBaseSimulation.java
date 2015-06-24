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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;

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
		// This is the location choice MultiNodeDijkstra.
		// Suppress all log messages of level below error --- to avoid spaming the config
		// file with zillions of "not route found" messages.
		Logger.getLogger( org.matsim.core.router.MultiNodeDijkstra.class ).setLevel( Level.ERROR ); // this is location choice
		Logger.getLogger( org.matsim.pt.router.MultiNodeDijkstra.class ).setLevel( Level.ERROR ); // this is "core"

		final Config config = Matsim2030Utils.loadConfig( configFile );
		// This is ugly, but is currently needed for location choice: initializing
		// the location choice writes K-values files to the output directory, which:
		// - fails if the directory does not exist
		// - makes the controler crash latter if the unsafe setOverwriteFiles( true )
		// is not called.
		// This ensures that we get safety with location choice working as expected,
		// before we sort this out and definitely kick out setOverwriteFiles.
		Matsim2030Utils.createEmptyDirectoryOrFailIfExists( config.controler().getOutputDirectory() );
		final Scenario scenario = Matsim2030Utils.loadScenario( config );

		final Controler controler = new Controler( scenario );
		controler.getConfig().controler().setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		Matsim2030Utils.initializeLocationChoice( controler );
		controler.setTripRouterFactory(
				Matsim2030Utils.createTripRouterFactory(
					scenario ) );

		Matsim2030Utils.initializeScoring( controler );
		Matsim2030Utils.loadControlerListeners( controler );
		
		// Code from Alex called Controler.setCreateGraphs( true ), but
		// it sets a config option, and config options should be set from config, not code.
		// So I do not it them here.
		controler.run();
	}
}

