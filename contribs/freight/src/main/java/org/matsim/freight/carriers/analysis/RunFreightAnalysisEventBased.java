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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.events.CarrierEventsReaders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

//import static org.matsim.application.ApplicationUtils.globFile; //TODO, this introduces a circular dependency. Resolve it

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
	private final Path SIM_OUTPUT_PATH;
	private final Path EVENTS_PATH;
	private final Path ANALYSIS_OUTPUT_PATH;
	private final String GLOBAL_CRS;

	private final Scenario scenario;

	//Removed this temporarily, as the import of globFile() causes a circular dependency
	/*/**
	 * This constructor automatically searches for the needed output file in a simulation run output.
	 *
	 * @param simOutputPath      The output directory of the simulation run
	 * @param analysisOutputPath The directory where the result of the analysis should go to
	 * @param globalCrs          The CRS of the simulation
	 */
	/*public RunFreightAnalysisEventBased(Path simOutputPath, Path analysisOutputPath, String globalCrs) {
		this.SIM_OUTPUT_PATH = simOutputPath;
		this.EVENTS_PATH = globFile(SIM_OUTPUT_PATH, "*output_events.*");
		this.ANALYSIS_OUTPUT_PATH = analysisOutputPath;
		this.GLOBAL_CRS = globalCrs;

		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(globFile(SIM_OUTPUT_PATH, "*output_allVehicles.*").toString());
		config.network().setInputFile(globFile(SIM_OUTPUT_PATH, "*output_network.*").toString());
		//freight settings
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersFile(globFile(SIM_OUTPUT_PATH, "*output_carriers.*").toString());
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(globFile(SIM_OUTPUT_PATH, "*output_carriersVehicleTypes.*").toString());

		prepareConfig(config);
		scenario = ScenarioUtils.loadScenario(config);
	}*/

	/**
	 * Alternative if you want to set the paths to the needed resources directly.
	 *
	 * @param analysisOutputPath The directory where the result of the analysis should go to
	 * @param globalCrs          The CRS of the simulation
	 */
	public RunFreightAnalysisEventBased(Path networkPath, Path vehiclesPath, Path carriersPath, Path carriersVehicleTypesPath, Path eventsPath, Path analysisOutputPath, String globalCrs) {
		this.SIM_OUTPUT_PATH = null;
		this.EVENTS_PATH = eventsPath;
		this.ANALYSIS_OUTPUT_PATH = analysisOutputPath;
		this.GLOBAL_CRS = globalCrs;

		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(vehiclesPath.toString());
		config.network().setInputFile(networkPath.toString());
		//freight settings
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersFile(carriersPath.toString());
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(carriersVehicleTypesPath.toString());

		prepareConfig(config);
		scenario = ScenarioUtils.loadScenario(config);
	}

	private void prepareConfig(Config config) {
		config.plans().setInputFile(null);
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);
		config.global().setCoordinateSystem(GLOBAL_CRS);
	}

	public void runAnalysis() throws IOException {
		//Where to store the analysis output?
		File folder = new File(String.valueOf(ANALYSIS_OUTPUT_PATH));
		folder.mkdirs();

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);


		// CarrierPlanAnalysis
		if (allCarriersHavePlans()) {
			CarrierPlanAnalysis carrierPlanAnalysis = new CarrierPlanAnalysis(CarriersUtils.getCarriers(scenario));
			carrierPlanAnalysis.runAnalysisAndWriteStats(ANALYSIS_OUTPUT_PATH);
		}

		// Prepare eventsManager - start of event based Analysis;
		EventsManager eventsManager = EventsUtils.createEventsManager();

		FreightTimeAndDistanceAnalysisEventsHandler freightTimeAndDistanceAnalysisEventsHandler = new FreightTimeAndDistanceAnalysisEventsHandler(scenario);
		eventsManager.addHandler(freightTimeAndDistanceAnalysisEventsHandler);

		CarrierLoadAnalysis carrierLoadAnalysis = new CarrierLoadAnalysis(CarriersUtils.getCarriers(scenario));
		eventsManager.addHandler(carrierLoadAnalysis);

		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = CarrierEventsReaders.createEventsReader(eventsManager);

		matsimEventsReader.readFile(EVENTS_PATH.toString());
		eventsManager.finishProcessing();

		log.info("Analysis completed.");
		log.info("Writing output...");
		freightTimeAndDistanceAnalysisEventsHandler.writeTravelTimeAndDistancePerVehicle(ANALYSIS_OUTPUT_PATH, scenario);
		freightTimeAndDistanceAnalysisEventsHandler.writeTravelTimeAndDistancePerVehicleType(ANALYSIS_OUTPUT_PATH, scenario);
		carrierLoadAnalysis.writeLoadPerVehicle(ANALYSIS_OUTPUT_PATH, scenario);
	}

	private boolean allCarriersHavePlans() {
		for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values())
			if (carrier.getSelectedPlan() == null) return false;

		return true;
	}
}
