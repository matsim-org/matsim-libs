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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.events.CarrierEventsReaders;

//import static org.matsim.application.ApplicationUtils.globFile;


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
public class RunFreightAnalysisEventBased {

	private static final Logger log = LogManager.getLogger(RunFreightAnalysisEventBased.class);

	//Where is your simulation output, that should be analysed?
	private String EVENTS_PATH = null;
	private final String ANALYSIS_OUTPUT_PATH;
	private Scenario scenario = null;
	private Carriers carriers = null;
	private final String delimiter = "\t";

	//TODO discuss renaming without EventBased. If this becomes the standard carrier output
	/**
	 * This constructor automatically searches for the necessary output file in a simulation run output.
	 *
	 * @param simOutputPath      The output directory of the simulation run
	 * @param analysisOutputPath The directory where the result of the analysis should go to
	 * @param globalCrs          The CRS of the simulation
	 */
	public RunFreightAnalysisEventBased(String simOutputPath, String analysisOutputPath, String globalCrs) {

		this.ANALYSIS_OUTPUT_PATH = analysisOutputPath;
//		this.EVENTS_PATH = globFile(simOutputPath, "*output_events.*");
//		Path vehiclesPath = globFile(simOutputPath, "*output_allVehicles.*");
//		Path networkPath = globFile(simOutputPath, "*output_network.*");
//		Path carriersPath = globFile(simOutputPath, "*output_carriers.*");
//		Path carriersVehicleTypesPath = globFile(simOutputPath, "*output_carriersVehicleTypes.*");

		// the better version with the globFile method is not available since there is a circular dependency between the modules application and freight

		final Path path = Path.of(simOutputPath);
		this.EVENTS_PATH = path.resolve("output_events.xml.gz").toString();
		String vehiclesPath = path.resolve("output_allVehicles.xml.gz").toString();
		String networkPath = path.resolve("output_network.xml.gz").toString();
		String carriersPath = path.resolve("output_carriers.xml.gz").toString();
		String carriersVehicleTypesPath = path.resolve("output_carriersVehicleTypes.xml.gz").toString();

		createScenarioForFreightAnalysis(vehiclesPath, networkPath, carriersPath, carriersVehicleTypesPath, globalCrs);
	}

	/**
	 * Alternative if you want to set the paths to the necessary resources directly.
	 *
	 * @param networkPath              Path to the network file
	 * @param vehiclesPath             Path to the vehicle file
	 * @param carriersPath             Path to the carriers file
	 * @param carriersVehicleTypesPath Path to the carriersVehicleTypes file
	 * @param eventsPath               Path to the events file
	 * @param analysisOutputPath       Path to the output directory
	 * @param globalCrs                The CRS of the simulation
	 */
	public RunFreightAnalysisEventBased(String networkPath, String vehiclesPath, String carriersPath, String carriersVehicleTypesPath, String eventsPath,
										String analysisOutputPath, String globalCrs) {
		this.EVENTS_PATH = eventsPath;
		this.ANALYSIS_OUTPUT_PATH = analysisOutputPath;

		createScenarioForFreightAnalysis(vehiclesPath, networkPath, carriersPath, carriersVehicleTypesPath, globalCrs);
	}

	/**
	 * Constructor, if you only want to have the carrier analysis.
	 *
	 * @param carriers           The carriers to be analysed
	 * @param analysisOutputPath The directory where the result of the analysis should go to
	 */
	public RunFreightAnalysisEventBased(Carriers carriers, String analysisOutputPath) {
		this.carriers = carriers;
		this.ANALYSIS_OUTPUT_PATH = analysisOutputPath;
	}

	private void createScenarioForFreightAnalysis(String vehiclesPath, String networkPath, String carriersPath, String carriersVehicleTypesPath,
												  String globalCrs) {
		log.info("########## Starting Freight Analysis ##########");

		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(vehiclesPath);
		config.network().setInputFile(networkPath);
		config.plans().setInputFile(null);
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);
		config.global().setCoordinateSystem(globalCrs);

		//freight settings
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersFile(carriersPath);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(carriersVehicleTypesPath);

		scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );
		this.carriers = CarriersUtils.addOrGetCarriers(scenario);
	}

	public void runCarriersAnalysis() throws IOException {

		//Where to store the analysis output?
		File folder = new File(String.valueOf(ANALYSIS_OUTPUT_PATH));
		folder.mkdirs();
		if (allCarriersHavePlans(carriers)) {
			CarrierPlanAnalysis carrierPlanAnalysis = new CarrierPlanAnalysis(delimiter, carriers);
			carrierPlanAnalysis.runAnalysisAndWriteStats(ANALYSIS_OUTPUT_PATH);
		}
		else  {
			log.warn("########## Not all carriers have plans. Skipping CarrierPlanAnalysis."); //TODO perhaps skipp complete analysis
		}
	}
	public void runCompleteAnalysis() throws IOException {
		runCarriersAnalysis();

		// Prepare eventsManager - start of event based Analysis;
		EventsManager eventsManager = EventsUtils.createEventsManager();

		FreightTimeAndDistanceAnalysisEventsHandler freightTimeAndDistanceAnalysisEventsHandler = new FreightTimeAndDistanceAnalysisEventsHandler(delimiter, scenario);
		eventsManager.addHandler(freightTimeAndDistanceAnalysisEventsHandler);

		CarrierLoadAnalysis carrierLoadAnalysis = new CarrierLoadAnalysis(delimiter, CarriersUtils.getCarriers(scenario));
		eventsManager.addHandler(carrierLoadAnalysis);

		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = CarrierEventsReaders.createEventsReader(eventsManager);

		matsimEventsReader.readFile(EVENTS_PATH);
		eventsManager.finishProcessing();

		log.info("Analysis completed.");
		log.info("Writing output...");
		freightTimeAndDistanceAnalysisEventsHandler.writeTravelTimeAndDistancePerVehicle(ANALYSIS_OUTPUT_PATH, scenario);
		freightTimeAndDistanceAnalysisEventsHandler.writeTravelTimeAndDistancePerVehicleType(ANALYSIS_OUTPUT_PATH, scenario);
		carrierLoadAnalysis.writeLoadPerVehicle(ANALYSIS_OUTPUT_PATH, scenario);
	}

	private boolean allCarriersHavePlans(Carriers carriers) {
		for (Carrier carrier : carriers.getCarriers().values())
			if (carrier.getSelectedPlan() == null) return false;

		return true;
	}
}
