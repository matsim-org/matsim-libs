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

package playground.agarwalamit.opdyts;

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
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import playground.kai.usecases.opdytsintegration.modechoice.EveryIterationScoringParameters;
import playground.kairuns.run.KNBerlinControler;

/**
 * @author amit
 */

public class MatsimOpdytsEquilMixedTrafficIntegration {

	private static final String EQUIL_DIR = "./matsim/examples/equil-mixedTraffic/";
	private static final String OUT_DIR = "./playgrounds/agarwalamit/output/equil-mixedTraffic/";
	public static final OpdytsObjectiveFunctionCases EQUIL_MIXEDTRAFFIC = OpdytsObjectiveFunctionCases.EQUIL_MIXEDTRAFFIC;

	public static void main(String[] args) {
		//see an example with detailed explanations -- package opdytsintegration.example.networkparameters.RunNetworkParameters 
		Config config = ConfigUtils.loadConfig(EQUIL_DIR+"/config.xml");

		config.controler().setOutputDirectory(OUT_DIR);
		config.plans().setInputFile("plans2000.xml.gz");

		//== default config has limited inputs
		StrategyConfigGroup strategies = config.strategy();
		strategies.clearStrategySettings();

		config.changeMode().setModes(new String [] {"car","bicycle"});
		StrategySettings modeChoice = new StrategySettings();
		modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode.name()); // dont know, how it will work
		modeChoice.setWeight(0.1);
		config.strategy().addStrategySettings(modeChoice);

		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.name());
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

		planCalcScoreConfigGroup.addModeParams(mpCar);
		planCalcScoreConfigGroup.addModeParams(mpBike);
		//==

		//==
		config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.withHoles );

		if ( config.qsim().getTrafficDynamics()== QSimConfigGroup.TrafficDynamics.withHoles ) {
			config.qsim().setInflowConstraint(QSimConfigGroup.InflowConstraint.maxflowFromFdiag);
		}

		config.qsim().setUsingFastCapacityUpdate(true);
		//==

		Scenario scenario = KNBerlinControler.prepareScenario(true, false, config);
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		double time = 6*3600. ;
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			Plan plan = person.getSelectedPlan() ;
			Activity activity = (Activity) plan.getPlanElements().get(0) ;
			activity.setEndTime(time);
			time++ ;
		}


		// this is something like time bin generator
		int startTime= 0;
		int binSize = 3600; // can this be scenario simulation end time.
		int binCount = 24; // to me, binCount and binSize must be related
		TimeDiscretization timeDiscretization = new TimeDiscretization(startTime, binSize, binCount);

		Set<String> modes2consider = new HashSet<>();
		modes2consider.add("car");
		modes2consider.add("bike");

		ModalStatsControlerListner stasControlerListner = new ModalStatsControlerListner(modes2consider,EQUIL_MIXEDTRAFFIC);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator<ModeChoiceDecisionVariable> simulator = new MATSimSimulator<>(new MATSimStateFactoryImpl<>(), scenario, timeDiscretization);
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				// add here whatever should be attached to matsim controler
				addTravelTimeBinding("bicycle").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("bicycle").to(carTravelDisutilityFactoryKey());

				// some stats
				addControlerListenerBinding().to(KaiAnalysisListener.class);
				addControlerListenerBinding().toInstance(stasControlerListner);

				bind(CharyparNagelScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
			}
		});

		// this is the objective Function which returns the value for given SimulatorState
		// in my case, this will be the distance based modal split
		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(EQUIL_MIXEDTRAFFIC); // in this, the method argument (SimulatorStat) is not used.

		//search algorithm
		int maxIterations = 10; // this many times simulator.run(...) and thus controler.run() will be called.
		int maxTransitions = Integer.MAX_VALUE;
		int populationSize = 10; // the number of samples for decision variables, one of them will be drawn randomly for the simulation.

		boolean interpolate = true;
		boolean includeCurrentBest = false;

		// randomize the decision variables (for e.g.\ utility parameters for modes)
		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario, RandomizedUtilityParametersChoser.ONLY_ASC, EQUIL_MIXEDTRAFFIC);

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario, EQUIL_MIXEDTRAFFIC);

		// what would decide the convergence of the objective function
		final int iterationsToConvergence = 100; //
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
	}
}
