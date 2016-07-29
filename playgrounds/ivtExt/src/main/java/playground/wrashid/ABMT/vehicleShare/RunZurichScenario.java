/* *********************************************************************** *
 * project: org.matsim.*
 * RunZurichScenario.java
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
package playground.wrashid.ABMT.vehicleShare;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.parking.lib.EventHandlerAtStartupAdder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;

import java.io.File;

/**
 * @author thibautd
 */
public class RunZurichScenario {
	public static void main(final String[] args) {
		final String configFile = args[0];

		Controler controler = startZHScenario(configFile);
		controler.run();
	}

	public static Controler startZHScenario(final String configFile) {
		// This allows to get a log file containing the log messages happening
		// before controler init.
		OutputDirectoryLogging.catchLogEntries();

		// This is the location choice MultiNodeDijkstra.
		// Suppress all log messages of level below error --- to avoid spaming the config
		// file with zillions of "not route found" messages.
		Logger.getLogger( org.matsim.core.router.MultiNodeDijkstra.class ).setLevel( Level.ERROR ); // this is location choice
		Logger.getLogger( org.matsim.pt.router.MultiNodeDijkstra.class ).setLevel( Level.ERROR );

//		final Config config = ConfigUtils.loadConfig(
//				configFile,
//				// this adds a new config group, used by the specific scoring function
//				// we use
//				new KtiLikeScoringConfigGroup(),
//				new DestinationChoiceConfigGroup());
		
		
		
		final Config config = ConfigUtils.loadConfig(
				configFile,
				// this adds a new config group, used by the specific scoring function
				// we use
				new KtiLikeScoringConfigGroup()
				);

		// This is currently needed for location choice: initializing
		// the location choice writes K-values files to the output directory, which:
		// - fails if the directory does not exist
		// - makes the controler crash latter if the unsafe setOverwriteFiles( true )
		// is not called.
		// This ensures that we get safety with location choice working as expected,
		// before we sort this out and definitely kick out setOverwriteFiles.
		createEmptyDirectoryOrFailIfExists( config.controler().getOutputDirectory() );
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		final Controler controler = new Controler( scenario );
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		connectFacilitiesWithNetwork( controler );
		
		// 
		//initializeLocationChoice( controler );

		// We use a specific scoring function, that uses individual preferences
		// for activity durations.
		
		
		//================ start custom code for EV scenario
		GlobalTESFParameters.init(config);
		
		controler.setScoringFunctionFactory(
			new EVScoringFunctionFactory(
					controler.getScenario(),
					new StageActivityTypesImpl(
						PtConstants.TRANSIT_ACTIVITY_TYPE ) ) ); 	

		if (GlobalTESFParameters.isSingYearScenario){
			controler.addControlerListener(new VehicleInitializer());
		} else {
			
		}
		controler.addControlerListener(new WriteStatistics());
		controler.addControlerListener(new EventHandlerAtStartupAdder(new TollsManager(controler)));
        controler.addControlerListener(new EventHandlerAtStartupAdder(new DistanceTravelledWithCar(controler.getScenario().getNetwork())));
		//================ end custom code for EV scenario
		
		
		return controler;
		
	}

	private static void connectFacilitiesWithNetwork(MatsimServices controler) {
		ActivityFacilities facilities = controler.getScenario().getActivityFacilities();
		// log.warn("number of facilities: "
		// +facilities.getFacilities().size());
		Network network = (Network) controler.getScenario().getNetwork();
		// log.warn("number of links: " +network.getLinks().size());

		WorldConnectLocations wcl = new WorldConnectLocations(controler.getConfig());
		wcl.connectFacilitiesWithLinks(facilities, network);
	}

	private static void initializeLocationChoice(final MatsimServices controler) {
		final Scenario scenario = controler.getScenario();
		final DestinationChoiceBestResponseContext lcContext = new DestinationChoiceBestResponseContext(scenario);
		lcContext.init();

		controler.addControlerListener(new DestinationChoiceInitializer(lcContext));
	}

	private static void createEmptyDirectoryOrFailIfExists(final String directory) {
		final File file = new File(directory + "/");
		if (file.exists() && file.list().length > 0) {
			// throw new UncheckedIOException(
			// "Directory "+directory+" exists and is not empty!" );
		}
		file.mkdirs();
	}
}
