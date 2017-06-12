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

package playground.agarwalamit.opdyts.equil;

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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.opdyts.*;
import playground.agarwalamit.opdyts.analysis.DecisionVariableAndBestSolutionPlotter;
import playground.agarwalamit.opdyts.analysis.OpdytsConvergencePlotter;
import playground.agarwalamit.utils.FileUtils;
import playground.kai.usecases.opdytsintegration.modechoice.EveryIterationScoringParameters;

/**
 * @author amit
 */

public class MatsimOpdytsEquilMixedTrafficIntegration {

	private static double randomVariance = 7;
	private static int iterationsToConvergence = 400;

	private static String EQUIL_DIR = "./examples/scenarios/equil-mixedTraffic/";
	private static String OUT_DIR = "./playgrounds/agarwalamit/output/equil_car,bicycle_holes_KWM_variance"+randomVariance+"_"+iterationsToConvergence+"its/";
	private static final OpdytsScenario EQUIL_MIXEDTRAFFIC = OpdytsScenario.EQUIL_MIXEDTRAFFIC;

	private static final boolean isPlansRelaxed = false;

	public static void main(String[] args) {

		if (args.length > 0) {
			randomVariance = Double.valueOf(args[0]);
			iterationsToConvergence = Integer.valueOf(args[1]);
			EQUIL_DIR = args[2];
			OUT_DIR = args[3]+"/equil_car,bicycle_holes_variance"+randomVariance+"_"+iterationsToConvergence+"its/";
		}

		List<String> modes2consider = Arrays.asList("car","bicycle");

		//see an example with detailed explanations -- package opdytsintegration.example.networkparameters.RunNetworkParameters 
		Config config = ConfigUtils.loadConfig(EQUIL_DIR+"/config.xml");

		config.plans().setInputFile("plans2000.xml.gz");

		//== default config has limited inputs
		StrategyConfigGroup strategies = config.strategy();
		strategies.clearStrategySettings();

		config.changeMode().setModes( modes2consider.toArray(new String [modes2consider.size()]));
		StrategySettings modeChoice = new StrategySettings();
		modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.name());
		modeChoice.setWeight(0.1);
		config.strategy().addStrategySettings(modeChoice);

		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		expChangeBeta.setWeight(0.9);
		config.strategy().addStrategySettings(expChangeBeta);

		//==

		//== planCalcScore params (initialize will all defaults).
		for ( PlanCalcScoreConfigGroup.ActivityParams params : config.planCalcScore().getActivityParams() ) {
			params.setTypicalDurationScoreComputation( PlanCalcScoreConfigGroup.TypicalDurationScoreComputation.relative );
		}

		// remove other mode params
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
		for ( PlanCalcScoreConfigGroup.ModeParams params : planCalcScoreConfigGroup.getModes().values() ) {
			planCalcScoreConfigGroup.removeParameterSet(params);
		}

		PlanCalcScoreConfigGroup.ModeParams mpCar = new PlanCalcScoreConfigGroup.ModeParams("car");
		PlanCalcScoreConfigGroup.ModeParams mpBike = new PlanCalcScoreConfigGroup.ModeParams("bicycle");
		mpBike.setMarginalUtilityOfTraveling(0.);


		planCalcScoreConfigGroup.addModeParams(mpCar);
		planCalcScoreConfigGroup.addModeParams(mpBike);
		//==

		//==
		config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.withHoles );
		config.qsim().setUsingFastCapacityUpdate(true);

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		//==

		if(! isPlansRelaxed) {

			config.controler().setOutputDirectory(OUT_DIR+"/relaxingPlans/");
			config.controler().setLastIteration(50);
			config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

			Scenario scenarioPlansRelaxor = ScenarioUtils.loadScenario(config);
			// following is taken from KNBerlinControler.prepareScenario(...);
			// modify equil plans:
			double time = 6*3600. ;
			for ( Person person : scenarioPlansRelaxor.getPopulation().getPersons().values() ) {
				Plan plan = person.getSelectedPlan() ;
				Activity activity = (Activity) plan.getPlanElements().get(0) ;
				activity.setEndTime(time);
				time++ ;
			}

			Controler controler = new Controler(scenarioPlansRelaxor);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addControlerListenerBinding().toInstance(new OpdytsModalStatsControlerListener(modes2consider, new EquilDistanceDistribution(EQUIL_MIXEDTRAFFIC)));
				}
			});
			controler.run();

			FileUtils.deleteIntermediateIterations(OUT_DIR,controler.getConfig().controler().getFirstIteration(), controler.getConfig().controler().getLastIteration());

			// set back settings for opdyts
			File file = new File(config.controler().getOutputDirectory()+"/output_plans.xml.gz");
			config.plans().setInputFile(file.getAbsoluteFile().getAbsolutePath());
			config.controler().setOutputDirectory(OUT_DIR);
			config.strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//****************************** mainly opdyts settings ******************************

		// this is something like time bin generator
		int startTime= 0;
		int binSize = 3600; // can this be scenario simulation end time.
		int binCount = 24; // to me, binCount and binSize must be related
		TimeDiscretization timeDiscretization = new TimeDiscretization(startTime, binSize, binCount);

		DistanceDistribution distanceDistribution = new EquilDistanceDistribution(EQUIL_MIXEDTRAFFIC);
		OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(modes2consider,distanceDistribution);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator2<ModeChoiceDecisionVariable> simulator = new MATSimSimulator2<>(new MATSimStateFactoryImpl<>(), scenario, timeDiscretization, new HashSet<>(modes2consider));
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				// add here whatever should be attached to matsim controler
//				addTravelTimeBinding("bicycle").to(networkTravelTime());
//				addTravelDisutilityFactoryBinding("bicycle").to(carTravelDisutilityFactoryKey());

				// some stats
				addControlerListenerBinding().to(KaiAnalysisListener.class);
				addControlerListenerBinding().toInstance(stasControlerListner);

				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
			}
		});

		// this is the objective Function which returns the value for given SimulatorState
		// in my case, this will be the distance based modal split
		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(distanceDistribution); // in this, the method argument (SimulatorStat) is not used.

		//search algorithm
		int maxIterations = 10; // this many times simulator.run(...) and thus controler.run() will be called.
		int maxTransitions = Integer.MAX_VALUE;
		int populationSize = 10; // the number of samples for decision variables, one of them will be drawn randomly for the simulation.

		boolean interpolate = true;
		boolean includeCurrentBest = false;

		// randomize the decision variables (for e.g.\ utility parameters for modes)
		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario,
				RandomizedUtilityParametersChoser.ONLY_ASC,  Double.valueOf(randomVariance),  EQUIL_MIXEDTRAFFIC, null);

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario, EQUIL_MIXEDTRAFFIC);

		// what would decide the convergence of the objective function
//		final int iterationsToConvergence = 600; //
		final int averagingIterations = 10;
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

		OpdytsConvergencePlotter opdytsConvergencePlotter = new OpdytsConvergencePlotter();
		opdytsConvergencePlotter.readFile(OUT_DIR+"/opdyts.con");
		opdytsConvergencePlotter.plotData(OUT_DIR+"/convergence.png");

		DecisionVariableAndBestSolutionPlotter decisionVariableAndBestSolutionPlotter = new DecisionVariableAndBestSolutionPlotter("bicycle");
		decisionVariableAndBestSolutionPlotter.readFile(OUT_DIR+"/opdyts.log");
		decisionVariableAndBestSolutionPlotter.plotData(OUT_DIR+"/decisionVariableVsASC.png");
	}
}
