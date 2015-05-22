/* *********************************************************************** *
 * project: org.matsim.*
 * DetailedAgentsTrackerRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.analysis.postprocessing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.analysis.DetailedAgentsTracker;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataGrabber;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel;
import playground.christoph.evacuation.mobsim.decisionmodel.PanicModel;
import playground.christoph.evacuation.mobsim.decisionmodel.PickupModel;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;

import com.vividsolutions.jts.geom.Geometry;

public class DetailedAgentsTrackerPostProcessing {

	public static void main(String[] args) {

		Logger root = Logger.getRootLogger();
		root.setLevel(Level.ALL);
		
		String configFile;
		String evacuationConfigFile;
		String outputPath;
		
//		configFile = "../../matsim/mysimulations/census2000V2/output_10pct_evac/evac.1.output_config.xml";
//		evacuationConfigFile = "../../matsim/mysimulations/census2000V2/config_evacuation.xml";
//		outputPath = "../../matsim/mysimulations/census2000V2/output_10pct_evac/";
		
//		configFile = "/data/matsim/cdobler/sandbox00/results_goesgen/output_census2000V2_goesgen_evacuation_DoE_run_33/config.xml";
//		evacuationConfigFile = "/data/matsim/cdobler/sandbox00/results_goesgen/output_census2000V2_goesgen_evacuation_DoE_run_33/config_evacuation_run_33.xml";
//		outputPath = "/data/matsim/cdobler/sandbox00/results_goesgen/output_census2000V2_goesgen_evacuation_DoE_run_33/";
		
		if (args.length != 3) return;
		else {
			configFile = args[0];
			evacuationConfigFile = args[1];
			outputPath = args[2];
		}

		new EvacuationConfigReader().readFile(evacuationConfigFile);
		EvacuationConfig.printConfig();
		
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// load household object attributes
		ObjectAttributes householdObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(householdObjectAttributes).parse(EvacuationConfig.householdObjectAttributesFile);
		
		// TODO: prepare scenario
		
		/*
		 * Create two OutputDirectoryHierarchies that point to the analyzed run's output directory.
		 * Since we do not want to overwrite existing results we add an additional prefix
		 * to the re-created outputs.
		 */
		OutputDirectoryHierarchy dummyInputDirectoryHierarchy = initOutputDirectoryHierarchy(scenario, outputPath, "");
		OutputDirectoryHierarchy dummyOutputDirectoryHierarchy = initOutputDirectoryHierarchy(scenario, outputPath, ".postprocessed");
		
		CoordAnalyzer coordAnalyzer = initCoordAnalyzer();
		HouseholdsTracker householdsTracker = initHouseholdsTracker(scenario);
		DecisionDataProvider decisionDataProvider = initDecisionDataProvider(scenario, coordAnalyzer, householdsTracker,
				householdObjectAttributes, dummyInputDirectoryHierarchy);
		
		DetailedAgentsTracker detailedAgentsTracker = new DetailedAgentsTracker(scenario, householdsTracker, decisionDataProvider, coordAnalyzer);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		List<MobsimListener> mobsimListeners = new ArrayList<MobsimListener>();
		mobsimListeners.add(householdsTracker);
		mobsimListeners.add(detailedAgentsTracker);
		SimStepEventsCreator simStepEventsCreator = new SimStepEventsCreator(mobsimListeners);
		
		eventsManager.addHandler(simStepEventsCreator);
		eventsManager.addHandler(detailedAgentsTracker);
		
		String eventsFile = dummyInputDirectoryHierarchy.getIterationFilename(0, Controler.FILENAME_EVENTS_XML);
		new EventsReaderXMLv1(eventsManager).parse(eventsFile);
		
		simStepEventsCreator.allEventsProcessed();

		/*
		 * Create results and write them to files.
		 */
		detailedAgentsTracker.analyzeResultsAndWriteFiles(dummyOutputDirectoryHierarchy);
	}
	
	private static CoordAnalyzer initCoordAnalyzer() {
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(ShapeFileReader.getAllFeatures(file));		
		}
		Geometry affectedArea = util.mergeGeometries(features);
		
		return new CoordAnalyzer(affectedArea);
	}
	
	private static HouseholdsTracker initHouseholdsTracker(Scenario scenario) {
		
		HouseholdsTracker householdsTracker = new HouseholdsTracker(scenario);
		householdsTracker.notifyBeforeMobsim(null);
				
		return householdsTracker;
	}
	
	private static DecisionDataProvider initDecisionDataProvider(Scenario scenario, CoordAnalyzer coordAnalyzer,
			HouseholdsTracker householdsTracker, ObjectAttributes householdObjectAttributes,
			OutputDirectoryHierarchy dummyInputDirectoryHierarchy) {
		
		DecisionDataProvider decisionDataProvider = new DecisionDataProvider();
		
		DecisionDataGrabber decisionDataGrabber = new DecisionDataGrabber(scenario, coordAnalyzer, 
				householdsTracker, householdObjectAttributes);
		decisionDataGrabber.grabDecisionData(decisionDataProvider);
		
		// read people in panic from file
		String panicFile = dummyInputDirectoryHierarchy.getIterationFilename(0, PanicModel.panicModelFile);
		PanicModel panicModel = new PanicModel(decisionDataProvider, EvacuationConfig.panicShare);
		panicModel.readDecisionsFromFile(panicFile);
		panicModel.printStatistics();
		
		// read pickup behavior from file
		String pickupFile = dummyInputDirectoryHierarchy.getIterationFilename(0, PickupModel.pickupModelFile);
		PickupModel pickupModel = new PickupModel(decisionDataProvider);
		pickupModel.readDecisionsFromFile(pickupFile);
		pickupModel.printStatistics();
		
		// read evacuation decisions from file
		String evacuationDecisionFile = dummyInputDirectoryHierarchy.getIterationFilename(0, EvacuationDecisionModel.evacuationDecisionModelFile);
		EvacuationDecisionModel evacuationDecisionModel = new EvacuationDecisionModel(scenario, MatsimRandom.getLocalInstance(), decisionDataProvider);
		evacuationDecisionModel.readDecisionsFromFile(evacuationDecisionFile);
		evacuationDecisionModel.printStatistics();
		
		return decisionDataProvider;
	}

	private static OutputDirectoryHierarchy initOutputDirectoryHierarchy(Scenario scenario, String outputPath, String postfix) {

		// add another string to the runId to not overwrite old files
		String runId = scenario.getConfig().controler().getRunId();
		scenario.getConfig().controler().setRunId(runId + postfix);
		OutputDirectoryHierarchy dummyOutputDirectoryHierarchy;
		if (outputPath == null) outputPath = scenario.getConfig().controler().getOutputDirectory();
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length() - 1);
		}
		if (scenario.getConfig().controler().getRunId() != null) {
			dummyOutputDirectoryHierarchy = new OutputDirectoryHierarchy(
					outputPath,
					scenario.getConfig().controler().getRunId(),
							true ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		} else {
			dummyOutputDirectoryHierarchy = new OutputDirectoryHierarchy(
					outputPath,
					null,
							true ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		}
		
		return dummyOutputDirectoryHierarchy;
	}

}
