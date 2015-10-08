package opdytsintegration.roadinvestment;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import opdytsintegration.MATSimDecisionVariableSetEvaluator;
import opdytsintegration.MATSimState;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controler.TerminationCriterion;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.convergencecriteria.ObjectiveFunctionChangeConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.searchalgorithms.TrajectorySamplingSelfTuner;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class RoadInvestmentMain {

	static void solveFictitiousProblem() throws FileNotFoundException {

		System.out.println("STARTED ...");

		final TrajectorySamplingSelfTuner selfTuner = new TrajectorySamplingSelfTuner(
				0.0, 0.0, 0.0, 0.95, 1.0);


		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler()
				.setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(100);
		config.global().setRandomSeed(new Random().nextLong());
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Map<Link, Double> link2freespeed = Collections.unmodifiableMap(link2freespeed(scenario));
		final Map<Link, Double> link2capacity = Collections.unmodifiableMap(link2capacity(scenario));

		final RoadInvestmentStateFactory stateFactory = new RoadInvestmentStateFactory();
		final RoadInvestmentObjectiveFunction objectiveFunction = new RoadInvestmentObjectiveFunction();
		final Set<RoadInvestmentDecisionVariable> decisionVariables = new LinkedHashSet<>();

		// 9 DECISION VARIABLES

		// decisionVariables.add(new EquilnetDecisionVariable(0.25, 0.25,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.25, 0.5,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.25, 0.75,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.5, 0.25,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.5, 0.5,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.5, 0.75,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.75, 0.25,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.75, 0.5,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.75, 0.75,
		// link2freespeed, link2capacity));

		// 16 DECISION VARIABLES

		// decisionVariables.add(new EquilnetDecisionVariable(0.0, 0.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.33, 0.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.67, 0.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(1.0, 0.0,
		// link2freespeed, link2capacity));
		//
		// decisionVariables.add(new EquilnetDecisionVariable(0.0, 0.33,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.33, 0.33,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.67, 0.33,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(1.0, 0.33,
		// link2freespeed, link2capacity));
		//
		// decisionVariables.add(new EquilnetDecisionVariable(0.0, 0.67,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.33, 0.67,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.67, 0.67,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(1.0, 0.67,
		// link2freespeed, link2capacity));
		//
		// decisionVariables.add(new EquilnetDecisionVariable(0.0, 1.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.33, 1.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(0.67, 1.0,
		// link2freespeed, link2capacity));
		// decisionVariables.add(new EquilnetDecisionVariable(1.0, 1.0,
		// link2freespeed, link2capacity));

		// 36 DECISION VARIABLES

		decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 0.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 0.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 0.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 0.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 0.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 0.0,
				link2freespeed, link2capacity));

		decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 0.2,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 0.2,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 0.2,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 0.2,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 0.2,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 0.2,
				link2freespeed, link2capacity));

		decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 0.4,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 0.4,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 0.4,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 0.4,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 0.4,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 0.4,
				link2freespeed, link2capacity));

		decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 0.6,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 0.6,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 0.6,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 0.6,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 0.6,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 0.6,
				link2freespeed, link2capacity));

		decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 0.8,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 0.8,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 0.8,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 0.8,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 0.8,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 0.8,
				link2freespeed, link2capacity));

		decisionVariables.add(new RoadInvestmentDecisionVariable(0.0, 1.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.2, 1.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.4, 1.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.6, 1.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(0.8, 1.0,
				link2freespeed, link2capacity));
		decisionVariables.add(new RoadInvestmentDecisionVariable(1.0, 1.0,
				link2freespeed, link2capacity));

		final List<RoadInvestmentDecisionVariable> shuffle = new ArrayList<>(
				decisionVariables);
		Collections.shuffle(shuffle);
		decisionVariables.clear();
		decisionVariables.addAll(shuffle);

		final int minimumAverageIterations = 5;
		final ObjectiveFunctionChangeConvergenceCriterion convergenceCriterion = new ObjectiveFunctionChangeConvergenceCriterion(
				1e-1, 1e-1, minimumAverageIterations);

		Simulator system = new Simulator() {
			@Override
			public SimulatorState run(TrajectorySampler evaluator) {
//				evaluator.addStatistic("./mylog.txt", new InterpolatedObjectiveFunctionValue());
//				evaluator.addStatistic("./mylog.txt", new AlphaStatistic(decisionVariables));

				final MATSimDecisionVariableSetEvaluator predictor
						= new MATSimDecisionVariableSetEvaluator(evaluator, decisionVariables, stateFactory);
				predictor.setMemory(1);
				predictor.setBinSize_s(10 * 60);
				predictor.setStartBin(6 * 5);
				predictor.setBinCnt(6 * 20);

				final Controler controler = new Controler(scenario);
				controler.addControlerListener(predictor);
				controler.setTerminationCriterion(new TerminationCriterion() {
					@Override
					public boolean continueIterations(int iteration) {
						return !predictor.foundSolution();
					}
				});
				controler.run();

				return predictor.getFinalState();
			}

			@Override
			public SimulatorState run(TrajectorySampler evaluator, SimulatorState initialState) {
				if (initialState != null) {
					((MATSimState) initialState).setPopulation(scenario.getPopulation());
					initialState.implementInSimulation();
				}
				return run(evaluator);
			}
		};
		DecisionVariableRandomizer randomizer = new DecisionVariableRandomizer() {
			@Override
			public DecisionVariable newRandomDecisionVariable() {
				return new RoadInvestmentDecisionVariable(MatsimRandom.getRandom().nextDouble(), MatsimRandom.getRandom().nextDouble(),
						link2freespeed, link2capacity);
			}

			@Override
			public DecisionVariable newRandomVariation(DecisionVariable decisionVariable) {
				return newRandomDecisionVariable();
			}
		};
		int maxMemoryLength = 100;
		boolean keepBestSolution = true;
		boolean interpolate = false;
		int maxIterations = 100;
		int maxTransitions = 100;
		int populationSize = 100;
		RandomSearch randomSearch = new RandomSearch(system, randomizer, convergenceCriterion, selfTuner, maxIterations, maxTransitions, populationSize,
				MatsimRandom.getRandom(), interpolate, keepBestSolution, objectiveFunction, maxMemoryLength);
		randomSearch.run();

		System.out.println("... DONE.");

	}

	private static Map<Link, Double> link2capacity(Scenario scenario) {
		Map<Link, Double> link2capacity = new LinkedHashMap<>();
		for (Link link : scenario.getNetwork().getLinks()
				.values()) {
			link2capacity.put(link, link.getCapacity());
		}
		return link2capacity;
	}

	private static Map<Link, Double> link2freespeed(Scenario scenario) {
		Map<Link, Double> link2freespeed = new LinkedHashMap<>();
		for (Link link : scenario.getNetwork().getLinks()
				.values()) {
			link2freespeed.put(link, link.getFreespeed());
		}
		return link2freespeed;
	}

	public static void main(String[] args) throws FileNotFoundException {

		solveFictitiousProblem();

	}
}
