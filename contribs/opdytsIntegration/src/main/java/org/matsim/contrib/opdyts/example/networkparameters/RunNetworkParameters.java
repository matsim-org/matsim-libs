package org.matsim.contrib.opdyts.example.networkparameters;

import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.Set;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.opdyts.MATSimSimulator2;
import org.matsim.contrib.opdyts.car.DifferentiatedLinkOccupancyAnalyzer;
import org.matsim.contrib.opdyts.utils.MATSimOpdytsControler;
import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;
import org.matsim.contrib.opdyts.utils.TimeDiscretization;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

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

		// >>>>>>>>>> SHOULD COME FROM FILE >>>>>>>>>>

		final OpdytsConfigGroup opdytsConfig = new OpdytsConfigGroup();
		opdytsConfig.setNumberOfIterationsForConvergence(10);
		opdytsConfig.setNumberOfIterationsForAveraging(5);
		opdytsConfig.setPopulationSize(4);
		opdytsConfig.setBinCount(24);
		opdytsConfig.setBinSize(3600);
		opdytsConfig.setStartTime(0);
		opdytsConfig.setMaxIteration(3);
		opdytsConfig.setWarmUpIterations(3);
		opdytsConfig.setUseAllWarmUpIterations(true);
		config.addModule(opdytsConfig);

		// <<<<<<<<<< SHOULD COME FROM FILE <<<<<<<<<<

		MATSimOpdytsControler<NetworkParameters> factories = new MATSimOpdytsControler<>(scenario);

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

		//optional (if not set, will be used a default) :Amit July'17
		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				opdytsConfig.getNumberOfIterationsForConvergence(),
				opdytsConfig.getNumberOfIterationsForAveraging());
		factories.setFixedIterationNumberConvergenceCriterion(convergenceCriterion);

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

		final DecisionVariableRandomizer<NetworkParameters> randomizer = new NetworkParametersRandomizer(
				opdytsConfig.getPopulationSize());

		/*
		 * The optimization extracts simulation information on a fixed time
		 * grid, which is defined here. The time grid should be small enough to
		 * capture all dynamics that are relevant to the problem at hand, but
		 * otherwise it should be chosen as coarse as possible to save computing
		 * resources.
		 */

		//optional (if not set, will be used a default) :Amit July'17
		final TimeDiscretization timeDiscretization = new TimeDiscretization(opdytsConfig.getStartTime(),
				opdytsConfig.getBinSize(),
				opdytsConfig.getBinCount());
		factories.setTimeDiscretization(timeDiscretization);

		/*
		 * Packages MATSim for use with Opdyts.
		 */

		final MATSimSimulator2<NetworkParameters> matsim = new MATSimSimulator2<NetworkParameters>(stateFactory,
				scenario);
		final Set<String> modes = new LinkedHashSet<>();
		modes.add("car");
		matsim.addSimulationStateAnalyzer(new DifferentiatedLinkOccupancyAnalyzer.Provider(timeDiscretization, modes,
				new LinkedHashSet<>(scenario.getNetwork().getLinks().keySet())));

		/*
		 * Further parameters needed to run the optimization.
		 * 
		 * An initial decision variable (recall that the initial plans file
		 * should be converged given this decision variable) and the number of
		 * search iterations (improvement steps).
		 */

		final NetworkParameters initialDecisionVariable = new NetworkParameters(scenario.getNetwork());

		/*
		 * Create the search algorithm.
		 */

		// changed it a bit.
//		final RandomSearch<NetworkParameters> randomSearch = factories.newRandomSearch(matsim, randomizer,
//				initialDecisionVariable, convergenceCriterion, objectiveFunction);

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

//		randomSearch.run();
		factories.run(matsim, randomizer, initialDecisionVariable, objectiveFunction);

		System.out.println("... DONE.");
	}
}
