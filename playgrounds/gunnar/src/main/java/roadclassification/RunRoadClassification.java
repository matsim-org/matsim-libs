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
import java.util.List;

import opdytsintegration.MATSimSimulator;
import opdytsintegration.TimeDiscretization;

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

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;

public class RunRoadClassification {

	static Logger log = Logger.getLogger(RunRoadClassification.class);

	static void justRun() {

		final String path = "./test/input/berlin_145f/";
		final String configFileName = path + "run_145f.output_config.xml";

		final Config config = ConfigUtils.loadConfig(configFileName);
		final File out = new File(config.controler().getOutputDirectory());
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
		almostRealLinkSettings.add(addNoise(new LinkSettings(14.0, 2000.0, 2.0)));
		almostRealLinkSettings.add(addNoise(new LinkSettings(14.0, 900.0, 1.0)));
		almostRealLinkSettings.add(addNoise(new LinkSettings(28.0, 6000.0, 3.0)));
		almostRealLinkSettings.add(addNoise(new LinkSettings(16.0, 3000.0, 2.0)));
		almostRealLinkSettings.add(addNoise(new LinkSettings(25.0, 4000.0, 2.5)));

		int maxMemoryLength = 100;
		boolean interpolate = false;
		int maxIterations = 100;
		int maxTransitions = 1000;
		int populationSize = 5;
		DecisionVariableRandomizer<RoadClassificationDecisionVariable> randomizer = new DecisionVariableRandomizer<RoadClassificationDecisionVariable>() {
//			@Override
//			public RoadClassificationDecisionVariable newRandomDecisionVariable() {
//				ArrayList<LinkSettings> linkSettingses = new ArrayList<>(almostRealLinkSettings);
//				return new RoadClassificationDecisionVariable(scenario.getNetwork(), linkAttributes, linkSettingses);
//			}

			@Override
			public List<RoadClassificationDecisionVariable> newRandomVariations(RoadClassificationDecisionVariable decisionVariable) {
				/*
				 * The algorithm performs best if this function returns
				 * two symmetric variations of the decision variable.
				 */
				RoadClassificationDecisionVariable var1 = new RoadClassificationDecisionVariable(scenario.getNetwork(), linkAttributes, new ArrayList<>(decisionVariable.getLinkSettingses()));
				RoadClassificationDecisionVariable var2 = new RoadClassificationDecisionVariable(scenario.getNetwork(), linkAttributes, new ArrayList<>(decisionVariable.getLinkSettingses()));
				for (int i=0; i<decisionVariable.getLinkSettingses().size();i++) {
					if (MatsimRandom.getRandom().nextDouble() < 2.0/decisionVariable.getLinkSettingses().size()) {
						double offset = MatsimRandom.getRandom().nextGaussian() * 100.0;
						var1.getLinkSettingses()
								.set(i, new LinkSettings(var1.getLinkSettingses().get(i).getFreespeed(),
										Math.max(var1.getLinkSettingses().get(i).getCapacity() + offset,0.0),
										var1.getLinkSettingses().get(i).getNofLanes()));
						var2.getLinkSettingses()
								.set(i, new LinkSettings(var2.getLinkSettingses().get(i).getFreespeed(),
										Math.max(var2.getLinkSettingses().get(i).getCapacity() - offset,0.0),
												var2.getLinkSettingses().get(i).getNofLanes()));
					}
				}
				return Arrays.asList(var1, var2);
			}
		};

//		final TrajectorySamplingSelfTuner selfTuner = new TrajectorySamplingSelfTuner(
//				0.0, 0.0, 0.0, 0.95, 1.0);
		final int minimumAverageIterations = 5;

		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(100, 10);
//		final ObjectiveFunctionChangeConvergenceCriterion convergenceCriterion = new ObjectiveFunctionChangeConvergenceCriterion(
//				1e-1, 1e-1, minimumAverageIterations);

		// Discretizize the day into 24 one-hour time bins, starting at midnight.
		final TimeDiscretization timeDiscretization = new TimeDiscretization(0, 3600, 24);
		
		RandomSearch<RoadClassificationDecisionVariable> randomSearch = new RandomSearch<>(new MATSimSimulator<RoadClassificationDecisionVariable>(stateFactory, scenario, timeDiscretization, null), randomizer, 
				new RoadClassificationDecisionVariable(scenario.getNetwork(), linkAttributes, new ArrayList<>(almostRealLinkSettings)),
				convergenceCriterion, 
				// selfTuner, 
				maxIterations, maxTransitions, populationSize,
				MatsimRandom.getRandom(), interpolate, objectiveFunction, maxMemoryLength, false);
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

	private static LinkSettings addNoise(LinkSettings linkSettings) {
		double noise = linkSettings.getCapacity() * MatsimRandom.getRandom().nextGaussian();
		return new LinkSettings(linkSettings.getFreespeed(), linkSettings.getCapacity() + noise, linkSettings.getNofLanes());
	}

	private static List<LinkSettings> shiftDown(int bubbleIndex, List<LinkSettings> linkSettingses) {
		int targetIndex = MatsimRandom.getRandom().nextInt(linkSettingses.size()-bubbleIndex)+bubbleIndex;
		ArrayList<LinkSettings> result = new ArrayList<>(linkSettingses);
		LinkSettings element = result.remove(bubbleIndex);
		result.add(targetIndex, element);
		return result;
	}

	private static List<LinkSettings> shiftUp(int bubbleIndex, List<LinkSettings> linkSettingses) {
		int targetIndex = MatsimRandom.getRandom().nextInt(bubbleIndex+1);
		ArrayList<LinkSettings> result = new ArrayList<>(linkSettingses);
		LinkSettings element = result.remove(bubbleIndex);
		result.add(targetIndex, element);
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
