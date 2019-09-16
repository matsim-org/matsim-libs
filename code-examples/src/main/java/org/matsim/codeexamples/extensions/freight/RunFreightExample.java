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
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import javax.management.InvalidAttributeValueException;
import java.net.URL;


/**
 * @see org.matsim.contrib.freight
 */
public class RunFreightExample {

	private static URL scenarioUrl ;
	static{
		scenarioUrl = ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ) ;
	}

	public static void main(String[] args) throws InvalidAttributeValueException {

		// ### config stuff: ###
		Config config = createConfig();

		ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "dump");

		// ### scenario stuff: ###
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//MATSim configurations
			// yyyy This needs to be done before the jspritRun, because otherwise it throws an Exception regarding the CarrierModule:
			// "carriers are provided as scenario element AND per the CarrierModule constructor [...]" KMT Nov'20
		final Controler controler = new Controler( scenario ) ;
		Freight.configure( controler );

		//Building the Carriers, running jsprit for solving the VRP:
		jspritRun( scenario );

		//start of the MATSim-Run:
		controler.run();
	}
	

	private static Config createConfig() {

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl(scenarioUrl, "config.xml" ) );

		//more general settings
		config.controler().setOutputDirectory("./output/freight");
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		new OutputDirectoryHierarchy( config.controler().getOutputDirectory(), config.controler().getRunId(),
			  config.controler().getOverwriteFileSetting(), true, ControlerConfigGroup.CompressionType.gzip ) ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );
		// (the directory structure is needed for jsprit output, which is before the controler starts.  Maybe there is a better alternative ...)

		config.global().setRandomSeed(4177);

		config.controler().setLastIteration(0);
		// yyyyyy iterations currently do not work; needs to be fixed.
		
		//freight settings
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightConfigGroup.class ) ;
	    freightConfigGroup.setCarriersFile( "singleCarrierFiveActivitiesWithoutRoutes.xml");
	    freightConfigGroup.setCarriersVehicleTypesFile( "vehicleTypes.xml");
	    //

		return config;
	}

	private static void jspritRun(Scenario scenario) throws InvalidAttributeValueException {

		//load carriers according to freight config
		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

		// set the number of jsprit Iterations per Carrier. This needs to be done here, because the example file
		// of the MATSim release version 12 does _not_ contain this information.
		// If this information is provided in the carrier file, you should skip this step if you do not want to overwrite
		// the values.
		// the number of jsprit iterations set here is just a number. It needs to be adjusted depended to your use case.
		Carriers carriers = FreightUtils.getCarriers(scenario);
		for (Carrier carrier : carriers.getCarriers().values()) {
			CarrierUtils.setJspritIterations(carrier, 10);
		}

		//### Output before jsprit run (not necessary)
		new CarrierPlanXmlWriterV2(FreightUtils.getCarriers(scenario)).write( scenario.getConfig().controler().getOutputDirectory() + "/jsprit_unplannedCarriers.xml") ;

		//Solving the VRP (generate carrier's tour plans)

		FreightUtils.runJsprit(scenario, ConfigUtils.addOrGetModule(scenario.getConfig(), FreightConfigGroup.class));


		//### Output after jsprit run (not necessary)
		new CarrierPlanXmlWriterV2(FreightUtils.getCarriers(scenario)).write( scenario.getConfig().controler().getOutputDirectory() + "/jsprit_plannedCarriers.xml") ;


	}




}
