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

import java.io.File;
import java.util.Arrays;
import java.util.List;
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
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.opdyts.*;
import playground.agarwalamit.utils.FileUtils;
import playground.kai.usecases.opdytsintegration.modechoice.EveryIterationScoringParameters;

/**
 * @author amit
 */

public class PatnaJointOpdytsCalibrator {

	private static final String SUB_POP_NAME = PatnaPersonFilter.PatnaUserGroup.urban.toString();
	private static final OpdytsScenario PATNA_10_PCT = OpdytsScenario.PATNA_10Pct;
	private static String OUT_DIR = FileUtils.RUNS_SVN+"/patnaIndia/run108/opdyts/output222/";
	private static final String configDir = FileUtils.RUNS_SVN+"/patnaIndia/run108/opdyts/input/";

	public static void main(String[] args) {

		String configFile;
		int iterationsToConvergence = 300; //
		int averagingIterations = 100;
		double randomVariance = 0.1;

		if ( args.length>0 ) {
			OUT_DIR = args[0];
			configFile = args[1];
			averagingIterations = Integer.valueOf(args[2]);
			iterationsToConvergence = Integer.valueOf(args[3]);
			randomVariance = Double.valueOf(args[4]);
		} else {
			configFile = configDir+"/config_urban_1pct.xml";
		}

		Config config = ConfigUtils.loadConfig(configFile);
		OUT_DIR += "/calibration/";

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn); // must be warn, since opdyts override few things

		config.controler().setOutputDirectory(OUT_DIR);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// == opdyts settings
		// this is something like time bin generator
		int startTime= 0;
		int binSize = 3600; // can this be scenario simulation end time.
		int binCount = 24; // to me, binCount and binSize must be related
		TimeDiscretization timeDiscretization = new TimeDiscretization(startTime, binSize, binCount);

		List<String> modes2consider = Arrays.asList("car","bike","motorbike","pt","walk");

		DistanceDistribution referenceStudyDistri = new PatnaCMPDistanceDistribution(PATNA_10_PCT);
		OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(modes2consider,referenceStudyDistri);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator<ModeChoiceDecisionVariable> simulator = new MATSimSimulator<>(new MATSimStateFactoryImpl<>(), scenario, timeDiscretization);
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {

				// some stats
				addControlerListenerBinding().to(KaiAnalysisListener.class);
				addControlerListenerBinding().toInstance(stasControlerListner);

				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
			}
		});

		// this is the objective Function which returns the value for given SimulatorState
		// in my case, this will be the distance based modal split
		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(referenceStudyDistri); // in this, the method argument (SimulatorStat) is not used.

		//search algorithm
		int maxIterations = 10; // this many times simulator.run(...) and thus controler.run() will be called.
		int maxTransitions = Integer.MAX_VALUE;
		int populationSize = 10; // the number of samples for decision variables, one of them will be drawn randomly for the simulation.

		boolean interpolate = true;
		boolean includeCurrentBest = false;

		// randomize the decision variables (for e.g.\ utility parameters for modes)
		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario,
				RandomizedUtilityParametersChoser.ONLY_ASC, randomVariance, PATNA_10_PCT, SUB_POP_NAME);

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario, PATNA_10_PCT, SUB_POP_NAME);

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

		// remove the unused iterations
		for (int index =0; index < maxIterations; index++) {
			String dir2remove = OUT_DIR+"_"+index+"/ITERS/";
			IOUtils.deleteDirectoryRecursively(new File(dir2remove).toPath());
		}
	}
}
