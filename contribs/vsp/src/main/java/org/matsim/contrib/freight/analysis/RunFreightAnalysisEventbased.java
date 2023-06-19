/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2021 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.contrib.freight.events.CarrierEventsReaders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;


/**
 * A first approach for some analysis based on the freight events introduced in 2022/23.
 * This class comes from teaching SimGV in the winter term 2022/23.
 * <p>
 * This class should get extended and prepared as a standardized analysis for freight output.
 * This should also get aligned with the current development in Simwrapper.
 * Todo: Add some tests.
 *
 * @author kturner (Kai Martins-Turner)
 */
public class RunFreightAnalysisEventbased {

	private static final Logger log = LogManager.getLogger(RunFreightAnalysisEventbased.class);

	//Were is your simulation output, that should be analysed?
	private final String SIM_OUTPUT_PATH ;
	private final String ANALYSIS_OUTPUT_PATH;
	private final String GLOBAL_CRS;

	/**
	 * @param simOutputPath      The output directory of the simulation run
	 * @param analysisOutputPath The directory where the result of the analysis should go to
	 * @param globalCrs
	 */
	public RunFreightAnalysisEventbased(String simOutputPath, String analysisOutputPath, String globalCrs) {
		this.SIM_OUTPUT_PATH = simOutputPath;
		this.ANALYSIS_OUTPUT_PATH = analysisOutputPath;
		this.GLOBAL_CRS = globalCrs;
	}

	public void runAnalysis() throws IOException {

		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(SIM_OUTPUT_PATH + "output_allVehicles.xml.gz");
		config.network().setInputFile(SIM_OUTPUT_PATH + "output_network.xml.gz");
		config.global().setCoordinateSystem(GLOBAL_CRS);
		config.plans().setInputFile(null);
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);
		//freight settings
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightConfigGroup.class ) ;
		freightConfigGroup.setCarriersFile( SIM_OUTPUT_PATH + "output_carriers.xml.gz");
		freightConfigGroup.setCarriersVehicleTypesFile(SIM_OUTPUT_PATH + "output_carriersVehicleTypes.xml.gz");

		//Were to store the analysis output?
		String analysisOutputDirectory = ANALYSIS_OUTPUT_PATH;
		if (!analysisOutputDirectory.endsWith("/")) {
			analysisOutputDirectory = analysisOutputDirectory + "/";
		}
		File folder = new File(analysisOutputDirectory);
		folder.mkdirs();

		final String eventsFile = SIM_OUTPUT_PATH + "output_events.xml.gz";

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		FreightUtils.loadCarriersAccordingToFreightConfig( scenario );


		// CarrierPlanAnalysis
		CarrierPlanAnalysis carrierPlanAnalysis = new CarrierPlanAnalysis(FreightUtils.getCarriers(scenario));
		carrierPlanAnalysis.runAnalysisAndWriteStats(analysisOutputDirectory);

		// Prepare eventsManager - start of event based Analysis;
		EventsManager eventsManager = EventsUtils.createEventsManager();

		FreightTimeAndDistanceAnalysisEventsHandler freightTimeAndDistanceAnalysisEventsHandler = new FreightTimeAndDistanceAnalysisEventsHandler(scenario);
		eventsManager.addHandler(freightTimeAndDistanceAnalysisEventsHandler);

		CarrierLoadAnalysis carrierLoadAnalysis = new CarrierLoadAnalysis(FreightUtils.getCarriers(scenario));
		eventsManager.addHandler(carrierLoadAnalysis);

		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = CarrierEventsReaders.createEventsReader(eventsManager);

		matsimEventsReader.readFile(eventsFile);
		eventsManager.finishProcessing();

		log.info("Analysis completed.");
		log.info("Writing output...");
		freightTimeAndDistanceAnalysisEventsHandler.writeTravelTimeAndDistance(analysisOutputDirectory, scenario);
		freightTimeAndDistanceAnalysisEventsHandler.writeTravelTimeAndDistancePerVehicleType(analysisOutputDirectory, scenario);
		carrierLoadAnalysis.writeLoadPerVehicle(analysisOutputDirectory, scenario);
	}

}
