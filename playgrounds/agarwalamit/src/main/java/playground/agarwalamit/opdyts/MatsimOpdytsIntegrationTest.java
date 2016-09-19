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

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterionResult;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import floetteroed.opdyts.trajectorysampling.Transition;
import opdytsintegration.MATSimSimulator;
import opdytsintegration.MATSimStateFactoryImpl;
import opdytsintegration.utils.TimeDiscretization;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.util.List;

/**
 * @author amit
 */

public class MatsimOpdytsIntegrationTest {

	private static final String EQUIL_DIR = "../../matsim/examples/equil-mixedTraffic/";

	public static void main(String[] args) {
		//see an example with detailed explanations -- package opdytsintegration.example.networkparameters.RunNetworkParameters 
		Config config = ConfigUtils.loadConfig(EQUIL_DIR+"/config.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// this is something like time bin generator
		int startTime= 0;
		int binSize = 24*3600; // can this be scenario simulation end time.
		int binCount = 1; // to me, binCount and binSize must be related
		TimeDiscretization timeDiscretization = new TimeDiscretization(startTime, binSize, binCount);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator<ModeChoiceDecisionVariable> simulator = new MATSimSimulator<>(new MATSimStateFactoryImpl<>(), scenario, timeDiscretization);
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				// add here whatever should be attached to matsim controler
				addTravelTimeBinding("bicycle").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("bicycle").to(carTravelDisutilityFactoryKey());
			}
		});


		// this is the objective Function which returns the value for given SimulatorState
		// in my case, this will be the distance based modal split
		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(); // in this, the method argument (SimulatorStat) is not used.

		//search algorithm
		int maxIterations = 20; // idk, if this could be same as the matsim last iteration
		int maxTransitions = Integer.MAX_VALUE;
		int populationSize = 10; // idk, if this could be same as the number of persons

		boolean interpolate = false;
		boolean includeCurrentBest = false;

		// randomize the decision variables (for e.g.\ utility parameters for modes)
		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario);

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario);

		// what would decide the convergence of the objective function
		final int iterationsToConvergence = 10; // a matsim simulation will run for iterationsToConvergence iterations.
		final int averagingIterations = 2;
		ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(iterationsToConvergence, averagingIterations);

		RandomSearch<ModeChoiceDecisionVariable> randomSearch = new RandomSearch<>(
				simulator,
				decisionVariableRandomizer,
				initialDecisionVariable,
				convergenceCriterion,
				maxIterations,
				maxTransitions,
				populationSize,
				MatsimRandom.getRandom(),
				interpolate,
				objectiveFunction,
				includeCurrentBest
				);

		// probably, an object which decide about the inertia
		SelfTuner selfTuner = new SelfTuner(0.95);

		// run it, this will eventually call simulator.run() and thus controler.run
		randomSearch.run(selfTuner );
	}
}
