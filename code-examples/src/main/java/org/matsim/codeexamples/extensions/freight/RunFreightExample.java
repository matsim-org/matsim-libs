/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.codeexamples.extensions.freight;

import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.CarrierPlanWriter;
import org.matsim.freight.carriers.controler.CarrierModule;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.util.concurrent.ExecutionException;


/**
 * @see org.matsim.freight.carriers
 */
public class RunFreightExample {

	public static void main(String[] args) throws ExecutionException, InterruptedException{
		run(args, false);
	}
	public static void run( String[] args, boolean runWithOTFVis ) throws ExecutionException, InterruptedException{

		// ### config stuff: ###
		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ), "config.xml" ) );
			config.plans().setInputFile( null ); // remove passenger input
			config.controller().setOutputDirectory( "./output/freight" );
			config.controller().setLastIteration( 0 );  // no iterations; for iterations see RunFreightWithIterationsExample.  kai, jan'23

			FreightCarriersConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class );
      freightConfigGroup.setCarriersFile("singleCarrierFiveActivitiesWithoutRoutes_Shipments.xml");
			freightConfigGroup.setCarriersVehicleTypesFile( "vehicleTypes.xml" );
		} else {
			config = ConfigUtils.loadConfig( args, new FreightCarriersConfigGroup() );
		}

		// load scenario (this is not loading the freight material):
		Scenario scenario = ScenarioUtils.loadScenario( config );

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		// how to set the capacity of the "light" vehicle type to "1":
//		CarriersUtils.getCarrierVehicleTypes( scenario ).getVehicleTypes().get( Id.create("light", VehicleType.class ) ).getCapacity().setOther( 1 );

		// output before jsprit run (not necessary)
		new CarrierPlanWriter(CarriersUtils.getCarriers( scenario )).write( "output/jsprit_unplannedCarriers.xml" ) ;
		// (this will go into the standard "output" directory.  note that this may be removed if this is also used as the configured output dir.)

		// Solving the VRP (generate carrier's tour plans)
		CarriersUtils.runJsprit( scenario );

		// Output after jsprit run (not necessary)
		new CarrierPlanWriter(CarriersUtils.getCarriers( scenario )).write( "output/jsprit_plannedCarriers.xml" ) ;
		// (this will go into the standard "output" directory.  note that this may be removed if this is also used as the configured output dir.)

		// ## MATSim configuration:  ##
		final Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new CarrierModule() );

		if ( runWithOTFVis ){
			controler.addOverridingModule( new OTFVisLiveModule() );
		}

		// ## Start of the MATSim-Run: ##
		controler.run();
	}

}
