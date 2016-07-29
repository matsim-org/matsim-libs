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

package opdytsintegration.example.networkparameters;

import java.io.FileNotFoundException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import opdytsintegration.MATSimSimulator;
import opdytsintegration.utils.TimeDiscretization;

/**
 * Simple optimization example. Selects flow capacity, number of lanes and max.
 * speed for all links in the network with the objective of maximizing the
 * average score.
 * 
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RunNetworkParameters {

	public static void main(String[] args) throws FileNotFoundException {

		/*
		 * Create the MATSim scenario of interest.
		 * 
		 * For serious applications, it is strongly recommended to use an
		 * initial plans file that results from a converged simulation with the
		 * initial decision variables.
		 */

		final String configPath = "./examples/equil/config.xml";
		final Config config = ConfigUtils.loadConfig(configPath);
		config.controler()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final String outputDirectory = scenario.getConfig().controler().getOutputDirectory();

		/*
		 * Define convergence criterion.
		 * 
		 * This requires to set (i) the number of iterations until the
		 * simulation has converged and (ii) the number of iterations over which
		 * to average to get rid of the simulation noise.
		 * 
		 * (i) The number of iterations until the simulation has converged is
		 * relative to the amount of variability in the decision variable
		 * randomization. Let X be any decision variable and Y be a random
		 * variation thereof. Let the simulation start with a converged plans
		 * file obtained with decision variable X. The number of iterations must
		 * then be large enough to reach a new converged state for any decision
		 * variable Y.
		 * 
		 * (ii) The number of iterations over which to average should be large
		 * enough to make the remaining simulation noise small compared to the
		 * expected difference between the objective function values of any
		 * decision variable and its random variation.
		 */

		int maxIterations = 10;
		int averageIterations = maxIterations / 2;
		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(maxIterations,
				averageIterations);

		/*
		 * Creation of MATSim state objects. A basic MATSim state class
		 * (MATSimState) is available, but one often needs to memorize
		 * additional information that is not strictly part of the state but
		 * still helpful when, for example, evaluating the objective function.
		 */

		final NetworkParametersStateFactory stateFactory = new NetworkParametersStateFactory();

		/*
		 * The objective function: a quantitative measure of what one wants to
		 * achieve. To be minimized.
		 */

		final ObjectiveFunction objectiveFunction = new NetworkParametersObjectiveFunction();

		/*
		 * Very problem-specific: given a decision variable, create trial
		 * variations thereof. These variations should be large enough to yield
		 * a measurable change in objective function value but they should still
		 * be relatively small (in the sense of a local search).
		 * 
		 * From the experiments performed so far, it appears as if the number of
		 * trial decision variables should be as large as memory allows.
		 */

		int numberOfTrialDecisionVariables = 4;
		final DecisionVariableRandomizer<NetworkParameters> randomizer = new NetworkParametersRandomizer(
				numberOfTrialDecisionVariables);

		/*
		 * The optimization extracts simulation information on a fixed time
		 * grid, which is defined here. The time grid should be small enough to
		 * capture all dynamics that are relevant to the problem at hand, but
		 * otherwise it should be chosen as coarse as possible to save computing
		 * resources.
		 */

		final int timeBinSize_s = 3600;
		final int timeBinCnt = 24;
		final TimeDiscretization timeDiscretization = new TimeDiscretization(0, timeBinSize_s, timeBinCnt);

		/*
		 * Packages MATSim for use with Opdyts.
		 */

		final MATSimSimulator<NetworkParameters> matsim = new MATSimSimulator<NetworkParameters>(stateFactory, scenario,
				timeDiscretization);

		/*
		 * Further parameters needed to run the optimization.
		 * 
		 * An initial decision variable (recall that the initial plans file
		 * should be converged given this decision variable) and the number of
		 * search iterations (improvement steps).
		 */

		final NetworkParameters initialDecisionVariable = new NetworkParameters(scenario.getNetwork());
		int maxSearchIterations = 3;

		/*
		 * Create the search algorithm.
		 * 
		 * The max. total memory should be reduced only if memory issues arise.
		 */

		final RandomSearch<NetworkParameters> randomSearch = new RandomSearch<>(matsim, randomizer,
				initialDecisionVariable, convergenceCriterion, maxSearchIterations, Integer.MAX_VALUE,
				numberOfTrialDecisionVariables, MatsimRandom.getRandom(), true, objectiveFunction, false);
		randomSearch.setLogPath(outputDirectory);
		randomSearch.setMaxTotalMemory(Integer.MAX_VALUE);
		final SelfTuner selfTuner = new SelfTuner(0.95);
		selfTuner.setNoisySystem(true);

		/*
		 * Finally, run it.
		 * 
		 * Several log files should appear in the output directory.
		 * 
		 * opdyts.con helps to assess if the simulation really converges and if
		 * stochasticity is sufficiently averaged out.
		 * 
		 * opdyts.sum gives summary statistics.
		 *
		 * opdyts.log gives very detailed statistics, also about the internals
		 * of the algorithm.
		 * 
		 */

		randomSearch.run(selfTuner);

		System.out.println("... DONE.");
	}
}
