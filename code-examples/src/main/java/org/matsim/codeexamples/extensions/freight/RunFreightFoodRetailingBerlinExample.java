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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.CarrierPlanWriter;
import org.matsim.freight.carriers.controler.CarrierModule;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.analysis.RunFreightAnalysisEventBased;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


/**
 * @see org.matsim.freight.carriers
 */
public class RunFreightFoodRetailingBerlinExample {

	public static void main(String[] args) throws ExecutionException, InterruptedException{
		run(args, false);
	}
	public static void run( String[] args, boolean runWithOTFVis ) throws ExecutionException, InterruptedException{

		// Path to public repo:
		String pathToInput = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/";
		// ### config stuff: ###
		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.createConfig();
			config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/output-berlinv5.5/berlin-v5.5.3-10pct.output_network.xml.gz");
			config.controller().setOutputDirectory( "./output/freight3" );
			config.controller().setLastIteration( 0 );  // no iterations; for iterations see RunFreightWithIterationsExample.  kai, jan'23
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			config.global().setCoordinateSystem("EPSG:31468");

			FreightCarriersConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class );
			freightConfigGroup.setCarriersFile(  pathToInput + "CarrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEV.xml" );
			freightConfigGroup.setCarriersVehicleTypesFile( pathToInput + "vehicleTypesBVWP100_DC_noTax.xml" );
		} else {
			config = ConfigUtils.loadConfig( args, new FreightCarriersConfigGroup() );
		}

		// load scenario (this is not loading the freight material):
		Scenario scenario = ScenarioUtils.loadScenario( config );

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		//Filter out only one carrier and reduce number of jsprit iteration to 1. Both for computational reasons.
		Carriers carriers = CarriersUtils.getCarriers(scenario);
		var carrier = carriers.getCarriers().get(Id.create("rewe_DISCOUNTER_TROCKEN", Carrier.class));
		CarriersUtils.setJspritIterations(carrier, 1);
		carriers.getCarriers().clear();
		carriers.addCarrier(carrier);

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

		var analysis = new RunFreightAnalysisEventBased(config.controller().getOutputDirectory()+"/", config.controller().getOutputDirectory()+"/analysis", "EPSG:31468");
		try {
			analysis.runAnalysis();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
