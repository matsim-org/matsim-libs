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
import java.util.*;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectBasedObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ObjectiveFunctionChangeConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;

import opdytsintegration.MATSimSimulator;
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

public class RunRoadClassification {

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
		final ObjectBasedObjectiveFunction objectiveFunction = new RoadClassificationObjectiveFunction(
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
		int maxIterations = 1;
		int maxTransitions = 100;
		int populationSize = 100;
		DecisionVariableRandomizer<RoadClassificationDecisionVariable> randomizer = new DecisionVariableRandomizer<RoadClassificationDecisionVariable>() {
			@Override
			public RoadClassificationDecisionVariable newRandomDecisionVariable() {
				ArrayList<LinkSettings> linkSettingses = new ArrayList<>(almostRealLinkSettings);
				Collections.shuffle(linkSettingses);
				return new RoadClassificationDecisionVariable(scenario.getNetwork(), linkAttributes, linkSettingses);
			}

			@Override
			public RoadClassificationDecisionVariable newRandomVariation(RoadClassificationDecisionVariable decisionVariable) {
				return newRandomDecisionVariable();
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
		randomSearch.run();


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
