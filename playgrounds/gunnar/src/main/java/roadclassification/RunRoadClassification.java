/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TunnelMain.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package roadclassification;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import floetteroed.opdyts.DecisionVariable;
import opdytsintegration.MATSimSimulator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ObjectiveFunctionChangeConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;

public class RunRoadClassification {

	static Logger log = Logger.getLogger(RunRoadClassification.class);

	static void justRun() {

		final String path = "./test/input/berlin_145f/";
		final String configFileName = path + "run_145f.output_config.xml";

		final Config config = ConfigUtils.loadConfig(configFileName);
		final File out = new File(config.findParam("controler",
				"outputDirectory"));
		if (out.exists()) {
			IOUtils.deleteDirectory(out);
		}

		final Controler controler = new Controler(config);
		controler.run();

	}

	public static void optimize(final Scenario scenario) {

		System.out.println("STARTED ...");

		final ObjectAttributes linkAttributes = (ObjectAttributes) scenario.getScenarioElement("linkAttributes");
		Counts counts = (Counts) scenario.getScenarioElement(Counts.ELEMENT_NAME);
		final RoadClassificationStateFactory stateFactory = new RoadClassificationStateFactory(scenario);
		final ObjectiveFunction objectiveFunction = new RoadClassificationObjectiveFunction(
				counts);
		final List<LinkSettings> almostRealLinkSettings = new ArrayList<>();
		almostRealLinkSettings.add(new LinkSettings(14.0, 2000.0, 2.0));
		almostRealLinkSettings.add(new LinkSettings(14.0, 900.0, 1.0));
		almostRealLinkSettings.add(new LinkSettings(28.0, 6000.0, 3.0));
		almostRealLinkSettings.add(new LinkSettings(16.0, 3000.0, 2.0));
		almostRealLinkSettings.add(new LinkSettings(25.0, 4000.0, 2.5));

		int maxMemoryLength = 100;
		boolean keepBestSolution = true;
		boolean interpolate = false;
		int maxIterations = 100;
		int maxTransitions = 100;
		int populationSize = 10;
		DecisionVariableRandomizer<RoadClassificationDecisionVariable> randomizer = new DecisionVariableRandomizer<RoadClassificationDecisionVariable>() {
			@Override
			public RoadClassificationDecisionVariable newRandomDecisionVariable() {
				ArrayList<LinkSettings> linkSettingses = new ArrayList<>(almostRealLinkSettings);
				Collections.shuffle(linkSettingses);
				return new RoadClassificationDecisionVariable(scenario.getNetwork(), linkAttributes, linkSettingses);
			}

			@Override
			public List<RoadClassificationDecisionVariable> newRandomVariations(RoadClassificationDecisionVariable decisionVariable) {
				/*
				 * The algorithm performs best if this function returns
				 * two symmetric variations of the decision variable.
				 */
				// random index, but not the first or last element:
				int bubbleIndex = MatsimRandom.getRandom().nextInt(decisionVariable.getLinkSettingses().size()-2)+1;
				RoadClassificationDecisionVariable var1 = new RoadClassificationDecisionVariable(scenario.getNetwork(), linkAttributes, shiftUp(bubbleIndex, decisionVariable.getLinkSettingses()));
				RoadClassificationDecisionVariable var2 = new RoadClassificationDecisionVariable(scenario.getNetwork(), linkAttributes, shiftDown(bubbleIndex, decisionVariable.getLinkSettingses()));
				return Arrays.asList(var1, var2);
			}
		};

//		final TrajectorySamplingSelfTuner selfTuner = new TrajectorySamplingSelfTuner(
//				0.0, 0.0, 0.0, 0.95, 1.0);
		final int minimumAverageIterations = 5;

		final ObjectiveFunctionChangeConvergenceCriterion convergenceCriterion = new ObjectiveFunctionChangeConvergenceCriterion(
				1e-1, 1e-1, minimumAverageIterations);

		RandomSearch<RoadClassificationDecisionVariable> randomSearch = new RandomSearch<>(new MATSimSimulator<RoadClassificationDecisionVariable>(stateFactory, scenario), randomizer, convergenceCriterion, 
				// selfTuner, 
				maxIterations, maxTransitions, populationSize,
				MatsimRandom.getRandom(), interpolate, keepBestSolution, objectiveFunction, maxMemoryLength);
		randomSearch.setLogFileName(scenario.getConfig().controler().getOutputDirectory() + "optimization.log");
		randomSearch.run();
		for (DecisionVariable decisionVariable : randomSearch.getBestDecisionVariablesView()) {
			log.info("--DecisionVariable follows--");
			RoadClassificationDecisionVariable rcdv = (RoadClassificationDecisionVariable) decisionVariable;
			for (LinkSettings linkSettings : rcdv.getLinkSettingses()) {
				log.info(String.format("%d %d %d\n", (int) linkSettings.getCapacity(), (int) linkSettings.getFreespeed(), (int) linkSettings.getNofLanes()));
			}
		}

		// AND RUN THE ENTIRE THING

//		final double maximumRelativeGap = 0.05;
//		final MATSimDecisionVariableSetEvaluator<RoadClassificationState, RoadClassificationDecisionVariable> predictor = new MATSimDecisionVariableSetEvaluator<RoadClassificationState, RoadClassificationDecisionVariable>(
//				decisionVariables, objectiveFunction,
//				// convergenceNoiseVarianceScale,
//				stateFactory, 5, maximumRelativeGap);
//		predictor.setStandardLogFileName("roadclassification-log.txt");
//		predictor.setMemory(1);
//		predictor.setBinSize_s(15 * 60);
//		predictor.setBinCnt(24 * 4);
//
//		controler.addControlerListener(predictor);
//		controler.run();

		System.out.println("... DONE.");
	}

	private static List<LinkSettings> shiftDown(int bubbleIndex, List<LinkSettings> linkSettingses) {
		ArrayList<LinkSettings> result = new ArrayList<>(linkSettingses);
		LinkSettings element = result.remove(bubbleIndex);
		result.add(bubbleIndex+1, element);
		return result;
	}

	private static List<LinkSettings> shiftUp(int bubbleIndex, List<LinkSettings> linkSettingses) {
		ArrayList<LinkSettings> result = new ArrayList<>(linkSettingses);
		LinkSettings element = result.remove(bubbleIndex);
		result.add(bubbleIndex-1, element);
		return result;
	}

	public static void main(String[] args) throws FileNotFoundException {
		final String path = "./test/input/berlin_145f/";
		final String configFileName = path + "run_145f.output_config.xml";
		final String countsFileName = path + "vmz_di-do.xml";

		final Config config = ConfigUtils.loadConfig(configFileName);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		final Counts counts = new Counts();
		final CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(
				counts);
		countsReader.parse(countsFileName);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(Counts.ELEMENT_NAME, counts);
		// justRun();
		optimize(scenario);

	}

	private static Config createConfig() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("output/roadclassification");
		config.controler()
				.setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		return config;
	}

}
