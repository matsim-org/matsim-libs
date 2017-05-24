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
import java.util.HashSet;
import java.util.List;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import opdytsintegration.MATSimSimulator2;
import opdytsintegration.MATSimStateFactoryImpl;
import opdytsintegration.utils.TimeDiscretization;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
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
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.opdyts.*;
import playground.agarwalamit.utils.FileUtils;
import playground.kai.usecases.opdytsintegration.modechoice.EveryIterationScoringParameters;

/**
 * @author amit
 */

public class PatnaUrbanOpdytsCalibrator {

	private static final OpdytsScenario PATNA_1_PCT = OpdytsScenario.PATNA_1Pct;
	private static String OUT_DIR = FileUtils.RUNS_SVN+"/patnaIndia/run111/opdyts/output222/";
	private static final String configDir = FileUtils.RUNS_SVN+"/patnaIndia/run111/opdyts/input/";

	public static void main(String[] args) {

		String configFile;
		int iterationsToConvergence = 10; //
		int averagingIterations = 10;
		boolean isRunningOnCluster = false;
		double randomVariance = 1.0;

		if (args.length>0) isRunningOnCluster = true;

		if ( isRunningOnCluster ) {

			configFile = args[1];
			averagingIterations = Integer.valueOf(args[2]);
			iterationsToConvergence = Integer.valueOf(args[3]);
			randomVariance = Double.valueOf(args[4]);

			OUT_DIR = args[0]+"RV_"+randomVariance+"_AI_"+averagingIterations+"its/";

		} else {
			configFile = configDir+"/config_urban_1pct.xml";
		}

		// relax the plans first.
		PatnaPlansRelaxor relaxor = new PatnaPlansRelaxor();
		relaxor.run(new String[]{configFile, OUT_DIR+"/initialPlans2RelaxedPlans/"});

		Config config = ConfigUtils.loadConfig(configFile);
		config.plans().setInputFile(OUT_DIR+"/initialPlans2RelaxedPlans/output_plans.xml.gz");
		OUT_DIR += "/calibration/";

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn); // must be warn, since opdyts override few things

		config.controler().setOutputDirectory(OUT_DIR);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// adding pt fare system based on distance
		// for above make sure that util_dist and monetary dist rate for pt are zero.
		PlanCalcScoreConfigGroup.ModeParams mp = scenario.getConfig().planCalcScore().getModes().get("pt");
		mp.setMarginalUtilityOfDistance(0.0);
		mp.setMonetaryDistanceRate(0.0);

		// == opdyts settings
		// this is something like time bin generator
		int startTime= 0;
		int binSize = 3600; // can this be scenario simulation end time.
		int binCount = 24; // to me, binCount and binSize must be related
		TimeDiscretization timeDiscretization = new TimeDiscretization(startTime, binSize, binCount);

		List<String> modes2consider = Arrays.asList("car","bike","motorbike","pt","walk");

		DistanceDistribution referenceStudyDistri = new PatnaOneBinDistanceDistribution(PATNA_1_PCT);
		OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(modes2consider,referenceStudyDistri);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator2<ModeChoiceDecisionVariable> simulator = new MATSimSimulator2<>(new MATSimStateFactoryImpl<>(), scenario, timeDiscretization, new HashSet<>(modes2consider));
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				// add here whatever should be attached to matsim controler

				// some stats
				addControlerListenerBinding().to(KaiAnalysisListener.class);
				addControlerListenerBinding().toInstance(stasControlerListner);

				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);

				// adding pt fare system based on distance
				this.addEventHandlerBinding().to(PtFareEventHandler.class);
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
				RandomizedUtilityParametersChoser.ONLY_ASC, randomVariance, PATNA_1_PCT, null);

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario, PATNA_1_PCT);

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
		selfTuner.setNoisySystem(true);
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
