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

package org.matsim.freight.carriers.usecases.chessboard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.roadpricing.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierModule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * @see org.matsim.freight.carriers
 */
public class RunFreightExampleWithToll {

	private  static final Logger log = LogManager.getLogger(RunFreightExampleWithToll.class);

	public static void main(String[] args) throws ExecutionException, InterruptedException{

		// ### config stuff: ###
		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ), "config.xml" ) );
			config.plans().setInputFile( null ); // remove passenger input
			config.controller().setOutputDirectory( "./output/freightExampleToll10000" );
			config.controller().setLastIteration( 0 );  // no iterations; for iterations see RunFreightWithIterationsExample.  kai, jan'23

			FreightCarriersConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class );
			freightConfigGroup.setCarriersFile( "singleCarrierFiveActivitiesWithoutRoutes.xml" );
//			freightConfigGroup.setCarriersFile( "singleCarrierFiveActivitiesWithoutRoutes_Shipments.xml" );
			freightConfigGroup.setCarriersVehicleTypesFile( "vehicleTypes.xml" );
		} else {
			config = ConfigUtils.loadConfig( args, new FreightCarriersConfigGroup() );
		}

		// load scenario (this is not loading the freight material):
		Scenario scenario = ScenarioUtils.loadScenario( config );

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		// how to set the capacity of the "heavy" vehicle type to "10":
//		CarriersUtils.getCarrierVehicleTypes( scenario ).getVehicleTypes().get( Id.create("heavy", VehicleType.class ) ).getCapacity().setOther( 10 );


		// Erstelle den Output-Ordner, falls er nicht existiert
		String outputDir = "./output";
		File dir = new File(outputDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}


		//Need to be before done before runJsprit().
		// ## MATSim configuration:  ##
		final Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new CarrierModule() );
		//Ebenfalls neu: RoadPricingModule, damit die Mautdaten berücksichtigt werden.
		RoadPricingSchemeUsingTollFactor schemeUsingTollFactor = setUpRoadpricing(scenario);
		controler.addOverridingModule(new RoadPricingModule(schemeUsingTollFactor));

		// Solving the VRP (generate carrier's tour plans)
		CarriersUtils.runJsprit( scenario );


		// ## Start of the MATSim-Run: ##
		controler.run();
	}


	/*
	 *  Set up roadpricing --- this is a copy paste from KMT lecture in GVSim --> need some adaptions
	 */
	private static RoadPricingSchemeUsingTollFactor setUpRoadpricing(Scenario scenario) {

		// Create Rp Scheme from code.
		RoadPricingSchemeImpl scheme = RoadPricingUtils.createRoadPricingSchemeImpl();

		/* Configure roadpricing scheme. */
		RoadPricingUtils.setName(scheme, "MautFromCode");
		RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_LINK);
		RoadPricingUtils.setDescription(scheme, "Mautdaten erstellt aus Link-Liste.");


		final List<String> TOLLED_LINKS = Arrays.asList("i(3,4)", "i(3,6)", "i(7,5)R", "i(7,7)R", "j(4,8)R", "j(6,8)R", "j(3,4)", "j(5,4)");

		/* Add general toll. */
		for (String linkIdString : TOLLED_LINKS) {
			RoadPricingUtils.addLink(scheme, Id.createLinkId(linkIdString));
		}


		RoadPricingUtils.createAndAddGeneralCost(scheme, Time.parseTime("00:00:00"), Time.parseTime("72:00:00"), 100.);  //
		///___ End creating from Code

		final List<String> TOLLED_VEHICLE_TYPES = List.of("heavy");

		// Wenn FzgTypId in Liste, erfolgt die Bemautung mit dem Kostensatz (Faktor = 1),
		// sonst mit 0 (Faktor = 0). ((MATSim seite)
		TollFactor tollFactor = (personId, vehicleId, linkId, time) -> {
			if (Objects.equals(vehicleId.toString(), "default")) {
				return 0; // if vehicleId is null, return 0
			}
			Vehicle vehicle = VehicleUtils.findVehicle(vehicleId, scenario); //Geht nur, wenn es MATSim-seitig da ist. (also im allVehicles container)

			String vehTypeIdString;
			if (vehicle != null) {
				vehTypeIdString = vehicle.getType().getId().toString(); //Take from vehiclesContainer :)
			} else {
				vehTypeIdString = findVehicleTypeInCarrier(vehicleId, CarriersUtils.getCarriers(scenario)); // Try to find it somewhere in the carriers.
				// Note: This is noct the best solution, because we cannot ensure that it takes it from the correct carrier.
			}
			if (TOLLED_VEHICLE_TYPES.contains(vehTypeIdString)) {
				return 1;
			} else {
				return 0;
			}
		};

		RoadPricingSchemeUsingTollFactor rpSchemeUsingTollFactor = new RoadPricingSchemeUsingTollFactor(scheme, tollFactor);
		RoadPricingUtils.addRoadPricingScheme(scenario, rpSchemeUsingTollFactor);
		return rpSchemeUsingTollFactor;
	}

	private static String findVehicleTypeInCarrier(Id<Vehicle> vehicleId, Carriers carriers) {
		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				if (carrierVehicle.getId().equals(vehicleId)) {
					return carrierVehicle.getType().getId().toString();
				}
			}
		}
		log.warn("Vehicle with ID {} }not found in any carrier's vehicles.", vehicleId);
		return "notFound";
	}

}
