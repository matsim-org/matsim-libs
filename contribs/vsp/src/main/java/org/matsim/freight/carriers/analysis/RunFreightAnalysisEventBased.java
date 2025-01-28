/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
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

package org.matsim.freight.carriers.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.events.CarrierEventsReaders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;


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
@Deprecated(since = "apr23", forRemoval = true)
public class RunFreightAnalysisEventBased {

	//What are the settings?
	protected static final String fileExtension = ".csv";
	protected static final String delimiter = ";";
	private static final Logger log = LogManager.getLogger(RunFreightAnalysisEventBased.class);

	//Where is your simulation output, that should be analysed?
	private final String SIM_OUTPUT_PATH ;
	private final String ANALYSIS_OUTPUT_PATH;
	private final String GLOBAL_CRS;

	/**
	 * @param simOutputPath      The output directory of the simulation run
	 * @param analysisOutputPath The directory where the result of the analysis should go to
	 * @param globalCrs
	 */
	public RunFreightAnalysisEventBased(String simOutputPath, String analysisOutputPath, String globalCrs) {
		this.SIM_OUTPUT_PATH = simOutputPath;
		this.ANALYSIS_OUTPUT_PATH = analysisOutputPath;
		this.GLOBAL_CRS = globalCrs;
	}

	public void runAnalysis() throws Exception {

		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(SIM_OUTPUT_PATH + "output_allVehicles.xml.gz");
		config.network().setInputFile(SIM_OUTPUT_PATH + "output_network.xml.gz");
		config.global().setCoordinateSystem(GLOBAL_CRS);
		config.plans().setInputFile(null);
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);
		//freight settings
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class ) ;
		freightCarriersConfigGroup.setCarriersFile( SIM_OUTPUT_PATH + "output_carriers.xml.gz");
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(SIM_OUTPUT_PATH + "output_carriersVehicleTypes.xml.gz");

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
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		//Log analysis
		//added bei AUE
		//ToDo: add log analysis for jsprit
		LogFileAnalysis logFileAnalysis = new LogFileAnalysis(log,SIM_OUTPUT_PATH,analysisOutputDirectory);
		logFileAnalysis.runLogFileAnalysis();

		// CarrierPlanAnalysis
		//CarrierPlanAnalysis carrierPlanAnalysis = new CarrierPlanAnalysis(CarriersUtils.getCarriers(scenario));
		//carrierPlanAnalysis.runAnalysisAndWriteStats(analysisOutputDirectory);

		// Prepare eventsManager - start of event based Analysis;
		EventsManager eventsManager = EventsUtils.createEventsManager();

		FreightTimeAndDistanceAnalysisEventsHandler freightTimeAndDistanceAnalysisEventsHandler =
				new FreightTimeAndDistanceAnalysisEventsHandler(delimiter, scenario);
		eventsManager.addHandler(freightTimeAndDistanceAnalysisEventsHandler);

		CarrierLoadAnalysis carrierLoadAnalysis = new CarrierLoadAnalysis(delimiter, CarriersUtils.getCarriers(scenario));
		eventsManager.addHandler(carrierLoadAnalysis);

		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = CarrierEventsReaders.createEventsReader(eventsManager);

		matsimEventsReader.readFile(eventsFile);
		eventsManager.finishProcessing();

		log.info("Analysis completed.");
		log.info("Writing output...");
		freightTimeAndDistanceAnalysisEventsHandler.writeTravelTimeAndDistancePerVehicle(analysisOutputDirectory, scenario);
		freightTimeAndDistanceAnalysisEventsHandler.writeTravelTimeAndDistancePerVehicleType(analysisOutputDirectory, scenario);
		freightTimeAndDistanceAnalysisEventsHandler.writeTravelTimeAndDistancePerCarrier(analysisOutputDirectory, scenario);
		CarrierPlanAnalysis carrierPlanAnalysis = new CarrierPlanAnalysis(delimiter, CarriersUtils.getCarriers(scenario));
		carrierPlanAnalysis.runAnalysisAndWriteStats(analysisOutputDirectory, CarriersAnalysis.CarrierAnalysisType.carriersPlans);

		carrierLoadAnalysis.writeLoadPerVehicle(analysisOutputDirectory, scenario);
	}

}
