/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.contrib.accidents.data.berlin.PlanfreeLinkIDs;
import org.matsim.contrib.accidents.data.berlin.TunnelLinkIDs;

/**
* @author ikaddoura, mmayobre
*/

public class RunAccidents {
	
	private static final Logger log = Logger.getLogger(RunAccidents.class);

	private static String configFile;
	private static String outputDirectory;
	private static String runId;
	
	private static String landUseFile;
	private static String popDensityFile;
	
	private static boolean internalize;
	
	public static void main(String[] args) throws IOException {
		log.info("Starting simulation run with the following arguments:");
		
		if (args.length > 0) {

			configFile = args[0];		
			log.info("config file: "+ configFile);
			
			outputDirectory = args[1];		
			log.info("output directory: "+ outputDirectory);
			
			runId = args[2];		
			log.info("run Id: "+ runId);
			
			landUseFile = args[3];
			log.info("landUseFile: " + landUseFile);
			
			popDensityFile = args[4];
			log.info("popDensityFile: " + popDensityFile);

			internalize = Boolean.parseBoolean(args[5]);
			log.info("internalize: " + internalize);
			
		} else {
			configFile = "./data/input/be_251/config.xml";
			outputDirectory = "./data/output/be_251/run_onlyBVWP_adjustedCosts30vs50/";
			runId = "run_onlyBVWP_adjustedCosts30vs50";
			landUseFile = "./data/input/osmBerlinBrandenburg/gis.osm_landuse_a_free_1_GK4.shp";
			popDensityFile = "./data/input/osmBerlin/gis.osm_places_a_free_1_GK4.shp";
			internalize = false;
			
//			configFile = "./data/input/internalization_test/internalization_config.xml";
//			outputDirectory = "./data/output/internalization_test/run_1/";
//			runId = "run1";
			
//			configFile = "./data/input/trial_scenario/trial_scenario_config.xml";
//			outputDirectory = "./data/output/trial_scenario/run_1/";
//			runId = "run1";

//			configFile = "./data/input/equil/config.xml";
//			outputDirectory = "./data/output/equil/run_1/";
//			runId = "run1";
		}
		
		RunAccidents main = new RunAccidents();
		main.run();
		
	}

	private void run() {
		log.info("Loading scenario...");
		
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
		accidentsSettings.setEnableAccidentsModule(true);
		accidentsSettings.setTunnelLinksArray(TunnelLinkIDs.getTunnelLinkIDs());
		accidentsSettings.setPlanFreeLinksArray(PlanfreeLinkIDs.getPlanfreeLinkIDs());
		accidentsSettings.setLanduseOSMInputShapeFile(landUseFile);
		accidentsSettings.setPlacesOSMInputFile(popDensityFile);
		accidentsSettings.setInternalizeAccidentCosts(internalize);
		accidentsSettings.setOsmInputFileCRS("EPSG:31468");
		
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AccidentsModule(scenario));
		
		log.info("Loading scenario... Done.");
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		log.info("Simulation run completed.");
	}

}

