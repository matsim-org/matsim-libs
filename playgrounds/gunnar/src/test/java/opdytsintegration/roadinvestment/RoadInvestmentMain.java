package opdytsintegration.roadinvestment;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import opdytsintegration.MATSimSimulator;
import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterionResult;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.SingleTrajectorySampler;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class RoadInvestmentMain {

	static Map.Entry<DecisionVariable, Double> evaluateSingleDecisionVariable(
			ObjectiveFunction objectiveFunction, Simulator system,
			Scenario scenario, final double betaPay, final double betaAlloc) {

		final Map<Link, Double> link2freespeed = Collections
				.unmodifiableMap(link2freespeed(scenario));
		final Map<Link, Double> link2capacity = Collections
				.unmodifiableMap(link2capacity(scenario));

		final DecisionVariable decisionVariable = new RoadInvestmentDecisionVariable(
				betaPay, betaAlloc, link2freespeed, link2capacity);

		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				100, 10);
		// final ObjectiveFunctionChangeConvergenceCriterion
		// convergenceCriterion =
		// new ObjectiveFunctionChangeConvergenceCriterion(
		// 1e-3, 1e-3, 5);

		SingleTrajectorySampler<DecisionVariable> sampler = new SingleTrajectorySampler<>(
				decisionVariable, objectiveFunction, convergenceCriterion);
		system.run(sampler);

		final Map.Entry<DecisionVariable, ConvergenceCriterionResult> decVar2convRes = sampler
				.getDecisionVariable2convergenceResultView().entrySet()
				.iterator().next();
		return new Map.Entry<DecisionVariable, Double>() {

			@Override
			public DecisionVariable getKey() {
				return decVar2convRes.getKey();
			}

			@Override
			public Double getValue() {
				return decVar2convRes.getValue().finalObjectiveFunctionValue;
			}

			@Override
			public Double setValue(Double value) {
				throw new UnsupportedOperationException();
			}
		};

		// return sampler.getDecisionVariable2finalObjectiveFunctionValueView()
		// .entrySet().iterator().next();
	}

	static void enumerateDecisionVariables() {

		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler()
				.setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(100);
		config.global().setRandomSeed(new Random().nextLong());
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final RoadInvestmentStateFactory stateFactory = new RoadInvestmentStateFactory();
		Simulator system = new MATSimSimulator(stateFactory, scenario,
				new TimeDiscretization(5 * 3600, 10 * 60, 18), null);
		final RoadInvestmentObjectiveFunction objectiveFunction = new RoadInvestmentObjectiveFunction();

		Map<DecisionVariable, Double> decVar2objFct = new LinkedHashMap<>();
		for (double betaPay : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
			for (double betaAlloc : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
				Map.Entry<DecisionVariable, Double> entry = evaluateSingleDecisionVariable(
						objectiveFunction, system, scenario, betaPay, betaAlloc);
				decVar2objFct.put(entry.getKey(), entry.getValue());
			}
		}

		for (Map.Entry<DecisionVariable, Double> entry : decVar2objFct
				.entrySet()) {
			System.out.println(entry);
		}
	}

	static void optimize() {

		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler()
				.setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(100);
		config.global().setRandomSeed(new Random().nextLong());
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final RoadInvestmentStateFactory stateFactory = new RoadInvestmentStateFactory();
		Simulator system = new MATSimSimulator(stateFactory, scenario,
				new TimeDiscretization(5 * 3600, 10 * 60, 18), null);
		final RoadInvestmentObjectiveFunction objectiveFunction = new RoadInvestmentObjectiveFunction();

		Map<DecisionVariable, Double> decVar2objFct = new LinkedHashMap<>();
		for (double betaPay : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
			for (double betaAlloc : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
				Map.Entry<DecisionVariable, Double> entry = evaluateSingleDecisionVariable(
						objectiveFunction, system, scenario, betaPay, betaAlloc);
				decVar2objFct.put(entry.getKey(), entry.getValue());
			}
		}

		for (Map.Entry<DecisionVariable, Double> entry : decVar2objFct
				.entrySet()) {
			System.out.println(entry);
		}
	}

	static void solveFictitiousProblem() throws FileNotFoundException {

		System.out.println("STARTED ...");

		// final TrajectorySamplingSelfTuner selfTuner = new
		// TrajectorySamplingSelfTuner(
		// 0.0, 0.0, 0.0, 0.95, 1.0);

		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler()
				.setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(100);
		config.global().setRandomSeed(new Random().nextLong());
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Map<Link, Double> link2freespeed = Collections
				.unmodifiableMap(link2freespeed(scenario));
		final Map<Link, Double> link2capacity = Collections
				.unmodifiableMap(link2capacity(scenario));

		final RoadInvestmentStateFactory stateFactory = new RoadInvestmentStateFactory();
		final RoadInvestmentObjectiveFunction objectiveFunction = new RoadInvestmentObjectiveFunction();

		final int minimumAverageIterations = 5;
		final FixedIterationNumberConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				100, 10);
		// final ObjectiveFunctionChangeConvergenceCriterion
		// convergenceCriterion = new
		// ObjectiveFunctionChangeConvergenceCriterion(
		// 1e-1, 1e-1, minimumAverageIterations);

		Simulator system = new MATSimSimulator(// decisionVariables,
				stateFactory, scenario, new TimeDiscretization(5 * 3600,
						10 * 60, 18), null);
		DecisionVariableRandomizer<RoadInvestmentDecisionVariable> randomizer = new DecisionVariableRandomizer<RoadInvestmentDecisionVariable>() {
			// public RoadInvestmentDecisionVariable newRandomDecisionVariable()
			// {
			// return new RoadInvestmentDecisionVariable(MatsimRandom
			// .getRandom().nextDouble(), MatsimRandom.getRandom()
			// .nextDouble(), link2freespeed, link2capacity);
			// }

			@Override
			public List<RoadInvestmentDecisionVariable> newRandomVariations(
					RoadInvestmentDecisionVariable decisionVariable) {
				return Arrays.asList(
						new RoadInvestmentDecisionVariable(Math.max(
								0,
								Math.min(1, decisionVariable.betaPay()
										+ 0.1
										* MatsimRandom.getRandom()
												.nextGaussian())), Math.max(
								0,
								Math.min(1, decisionVariable.betaAlloc()
										+ 0.1
										* MatsimRandom.getRandom()
												.nextGaussian())),
								link2freespeed, link2capacity),
						new RoadInvestmentDecisionVariable(Math.max(
								0,
								Math.min(1, decisionVariable.betaPay()
										+ 0.1
										* MatsimRandom.getRandom()
												.nextGaussian())), Math.max(
								0,
								Math.min(1, decisionVariable.betaAlloc()
										+ 0.1
										* MatsimRandom.getRandom()
												.nextGaussian())),
								link2freespeed, link2capacity));
			}
		};
		boolean keepBestSolution = true;
		boolean interpolate = true;
		int maxIterations = 10;
		int maxTransitions = Integer.MAX_VALUE;
		int populationSize = 10;
		RandomSearch<RoadInvestmentDecisionVariable> randomSearch = new RandomSearch<>(
				system, randomizer, new RoadInvestmentDecisionVariable(
						MatsimRandom.getRandom().nextDouble(), MatsimRandom
								.getRandom().nextDouble(), link2freespeed,
						link2capacity),
				convergenceCriterion,
				// selfTuner,
				maxIterations, maxTransitions, populationSize,
				MatsimRandom.getRandom(), interpolate, objectiveFunction,
				false);
		randomSearch.setLogFileName("./randomSearchLog.txt");
		randomSearch.run();

		System.out.println("... DONE.");

	}

	private static Map<Link, Double> link2capacity(Scenario scenario) {
		Map<Link, Double> link2capacity = new LinkedHashMap<>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link2capacity.put(link, link.getCapacity());
		}
		return link2capacity;
	}

	private static Map<Link, Double> link2freespeed(Scenario scenario) {
		Map<Link, Double> link2freespeed = new LinkedHashMap<>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link2freespeed.put(link, link.getFreespeed());
		}
		return link2freespeed;
	}

	public static void main(String[] args) throws FileNotFoundException {

		// enumerateDecisionVariables();
		solveFictitiousProblem();

	}

}
