/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions.example;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.MatsimVehicleWriter;


/**
 *
 * Use the config file as created by the 
 * {@link CreateEmissionConfig CreateEmissionConfig} to calculate
 * emissions based on the link leave events of an events file. Resulting emission events are written into an event file.
 *
 * @author benjamin, julia
 */
public final class RunAverageEmissionToolOfflineExample{

//	private static final String configFile = "./scenarios/sampleScenario/testv2_Vehv1/config_average.xml";

	public static final String emissionEventsFilename = "emission.events.offline.xml.gz";

	// (remove dependency of one test/execution path from other. kai/ihab, nov'18)

	private Config config;

	// =======================================================================================================		

	public static void main (String[] args){
		RunAverageEmissionToolOfflineExample emissionToolOfflineExampleV2 = new RunAverageEmissionToolOfflineExample();
		emissionToolOfflineExampleV2.run();
	}

//	public Config prepareConfig() {
//		config = ConfigUtils.loadConfig(configFile, new EmissionsConfigGroup());
//		return config;
//	}

	public Config prepareConfig(String args){
		throw new RuntimeException("execution path no longer exists");
	}
	public Config prepareConfig(String [] args) {
		config = ConfigUtils.loadConfig(args, new EmissionsConfigGroup());
		EmissionsConfigGroup ecg = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		ecg.setHbefaVehicleDescriptionSource(EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription);
		return config;
	}

	public void run() {
		if ( config==null ) {
//			this.prepareConfig() ;
			throw new RuntimeException( "this execution path no longer exists" );
		}
		Scenario scenario = ScenarioUtils.loadScenario(config);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		// If you get an Exception "queue full" with the eventsManager above, please try the "old" single threaded one (below)
		// There is an issue that the ParallelEventsManager has problems if the number of events is to hugh.
		// see also https://github.com/matsim-org/matsim-libs/issues/1091
//		EventsManager eventsManager = new EventsManagerImpl();

		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
//				bind( OutputDirectoryHierarchy.class );
			}
		};;

		com.google.inject.Injector injector = Injector.createInjector(config, module );

		EmissionModule emissionModule = injector.getInstance(EmissionModule.class);
//		OutputDirectoryHierarchy outputDirectoryHierarchy = injector.getInstance( OutputDirectoryHierarchy.class );

		final String outputDirectory = scenario.getConfig().controler().getOutputDirectory();
		EventWriterXML emissionEventWriter = new EventWriterXML( outputDirectory + emissionEventsFilename );
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
//		matsimEventsReader.readFile( "./scenarios/sampleScenario/5.events.xml.gz" );
		matsimEventsReader.readFile( IOUtils.extendUrl( config.getContext(), "../output_events.xml.gz" ).toString() );
		eventsManager.finishProcessing();

		emissionEventWriter.closeFile();

		new MatsimVehicleWriter( scenario.getVehicles() ).writeFile( outputDirectory + "vehicles.xml.gz" );

	}
}
