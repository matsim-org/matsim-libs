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
package org.matsim.codeexamples.extensions.emissions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.example.CreateEmissionConfig;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleUtils;

import static org.matsim.contrib.emissions.utils.EmissionsConfigGroup.*;


/**
 *
 * Use the config file as created by the
 * {@link CreateEmissionConfig CreateEmissionConfig} to calculate
 * emissions based on the link leave events of an events file. Resulting emission events are written into an event file.
 *
 * @author benjamin, julia
 */
public final class RunAverageEmissionToolOfflineExample{
	private static final Logger log = LogManager.getLogger(RunAverageEmissionToolOfflineExample.class );
	private static final String eventsFile =  "./scenarios/sampleScenario/5.events.xml.gz";

	/* package, for test */ static final String emissionEventOutputFileName = "5.emission.events.offline.xml.gz";

	// =======================================================================================================

	public static void main (String[] args){
		// see testcase for an example
		Config config ;
		if ( args==null || args.length==0 || args[0]==null ) {
			config = ConfigUtils.loadConfig( "./scenarios/sampleScenario/testv2_Vehv2/config_average.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}
		config.plansCalcRoute().clearTeleportedModeParams();

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		{
			// config_average has the emissions config group commented out.  So all that there is to configure (for average emissions)
			// follows here. kai, dec'22

			emissionsConfig.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.directlyTryAverageTable );

//		emissionsConfig.setAverageColdEmissionFactorsFile( "../sample_EFA_ColdStart_vehcat_2005average.csv" );
//		emissionsConfig.setAverageWarmEmissionFactorsFile( "../sample_EFA_HOT_vehcat_2005average.csv" );

			emissionsConfig.setAverageColdEmissionFactorsFile( "../sample_EFA_ColdStart_vehcat_2020_average_withHGVetc.csv" );
			emissionsConfig.setAverageWarmEmissionFactorsFile( "../sample_41_EFA_HOT_vehcat_2020average.csv" );

			emissionsConfig.setNonScenarioVehicles( NonScenarioVehicles.abort );

		}
		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// examples for how to set attributes to links and vehicles in order to make this work (already there for example scenario):

//		for( Link link : scenario.getNetwork().getLinks().values() ){
//			if ( true ) {
//				EmissionUtils.setHbefaRoadType( link, "URB/Local/50" );
//			}
//		}
//
//		for( VehicleType vehicleType : scenario.getVehicles().getVehicleTypes().values() ){
//			if ( true ){
//				VehicleUtils.setHbefaVehicleCategory( vehicleType.getEngineInformation(), HbefaVehicleCategory.PASSENGER_CAR.toString() );
//				VehicleUtils.setHbefaTechnology( vehicleType.getEngineInformation(), "average" );
//				VehicleUtils.setHbefaEmissionsConcept( vehicleType.getEngineInformation(), "average" );
//				VehicleUtils.setHbefaSizeClass( vehicleType.getEngineInformation(), "average");
//			}
//		}

		// ---

		// we do not want to run the full Controler.  In consequence, we plug together the infrastructure one needs in order to run the emissions contrib:

		EventsManager eventsManager = EventsUtils.createEventsManager();

		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager ) ;
				bind( EmissionModule.class ) ;
			}
		};

		com.google.inject.Injector injector = Injector.createInjector( config, module );

		// the EmissionModule must be instantiated, otherwise it does not work:
		injector.getInstance(EmissionModule.class);

		// ---

		// add events writer into emissions event handler
		final EventWriterXML eventWriterXML = new EventWriterXML( config.controler().getOutputDirectory() + '/' + emissionEventOutputFileName );
		eventsManager.addHandler( eventWriterXML );

		// read events file into the events reader.  EmissionsModule and events writer have been added as handlers, and will act accordingly.
		EventsUtils.readEvents( eventsManager, eventsFile );

		// events writer needs to be explicitly closed, otherwise it does not work:
		eventWriterXML.closeFile();

		// also write vehicles and network and config as a service so we have all out files in one directory:
		new MatsimVehicleWriter( scenario.getVehicles() ).writeFile( config.controler().getOutputDirectory() + "/output_vehicles.xml.gz" );
		NetworkUtils.writeNetwork( scenario.getNetwork(), config.controler().getOutputDirectory() + "/output_network.xml.gz" );
		ConfigUtils.writeConfig( config, config.controler().getOutputDirectory() + "/output_config.xml" );
		ConfigUtils.writeMinimalConfig( config, config.controler().getOutputDirectory() + "/output_config_reduced.xml" );

	}

}
