/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.opdyts.patna;

import java.util.HashSet;
import java.util.Set;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import opdytsintegration.MATSimSimulator;
import opdytsintegration.MATSimStateFactoryImpl;
import opdytsintegration.utils.TimeDiscretization;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.opdyts.ModalStatsControlerListner;
import playground.agarwalamit.opdyts.ModeChoiceObjectiveFunction;
import playground.agarwalamit.opdyts.ModeChoiceRandomizer;
import playground.agarwalamit.opdyts.OpdytsObjectiveFunctionCases;
import playground.agarwalamit.utils.FileUtils;
import playground.kai.usecases.opdytsintegration.modechoice.ModeChoiceDecisionVariable;

/**
 * @author amit
 */

public class PatnaOpdytsCalibrator {

	private static String OUT_DIR = FileUtils.RUNS_SVN+"/patnaIndia/run108/opdyts/output/";
	private static final String configDir = FileUtils.RUNS_SVN+"/patnaIndia/run108/opdyts/input/";

	public static void main(String[] args) {

		Config config;
		int iterationsToConvergence = 100; //
		int averagingIterations = 10;
		boolean isRunningOnCluster = false;

		if (args.length>0) isRunningOnCluster = true;

		if ( isRunningOnCluster ) {
			OUT_DIR = args[0];
			config = ConfigUtils.loadConfig(args[1]);
			averagingIterations = Integer.valueOf(args[2]);
			iterationsToConvergence = Integer.valueOf(args[3]);
		} else {
			config = ConfigUtils.loadConfig(configDir+"/config_urban_1pct.xml");
		}

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn); // must be warn, since opdyts override few things

		config.controler().setOutputDirectory(OUT_DIR);
		config.qsim().setEndTime(50.*3600.);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// == opdyts settings
		// this is something like time bin generator
		int startTime= 0;
		int binSize = 3600; // can this be scenario simulation end time.
		int binCount = 24; // to me, binCount and binSize must be related
		TimeDiscretization timeDiscretization = new TimeDiscretization(startTime, binSize, binCount);

		Set<String> modes2consider = new HashSet<>();
		modes2consider.add("car");
		modes2consider.add("bike");
		modes2consider.add("motorbike");
		modes2consider.add("pt");
		modes2consider.add("walk");

		ModalStatsControlerListner stasControlerListner = new ModalStatsControlerListner(modes2consider);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator<ModeChoiceDecisionVariable> simulator = new MATSimSimulator<>(new MATSimStateFactoryImpl<>(), scenario, timeDiscretization);
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				// add here whatever should be attached to matsim controler
				addTravelTimeBinding("bike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("bike").to(carTravelDisutilityFactoryKey());
				// no need to add travel time and travel disutility for motorbike (same as car), should be inserted auto-magically, now

				// some stats
				addControlerListenerBinding().to(KaiAnalysisListener.class);
				addControlerListenerBinding().toInstance(stasControlerListner);
			}
		});

		// this is the objective Function which returns the value for given SimulatorState
		// in my case, this will be the distance based modal split
		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(OpdytsObjectiveFunctionCases.PATNA); // in this, the method argument (SimulatorStat) is not used.

		//search algorithm
		int maxIterations = 10; // this many times simulator.run(...) and thus controler.run() will be called.
		int maxTransitions = Integer.MAX_VALUE;
		int populationSize = 10; // the number of samples for decision variables, one of them will be drawn randomly for the simulation.

		boolean interpolate = true;
		boolean includeCurrentBest = false;

		// randomize the decision variables (for e.g.\ utility parameters for modes)
		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario);

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario);

		// what would decide the convergence of the objective function
		ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(iterationsToConvergence, averagingIterations);

		RandomSearch<ModeChoiceDecisionVariable> randomSearch = new RandomSearch<>(
				simulator,
				decisionVariableRandomizer,
				initialDecisionVariable,
				convergenceCriterion,
				maxIterations, // this many times simulator.run(...) and thus controler.run() will be called.
				maxTransitions,
				populationSize,
				MatsimRandom.getRandom(),
				interpolate,
				objectiveFunction,
				includeCurrentBest
				);

		// probably, an object which decide about the inertia
		SelfTuner selfTuner = new SelfTuner(0.95);

		randomSearch.setLogPath(OUT_DIR);

		// run it, this will eventually call simulator.run() and thus controler.run
		randomSearch.run(selfTuner );
	}
}