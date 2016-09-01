package playground.kai.usecases.autosensingmarginalutilities;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import floetteroed.opdyts.searchalgorithms.Simulator;
import opdytsintegration.MATSimSimulator;
import opdytsintegration.MATSimStateFactory;
import opdytsintegration.roadinvestment.RoadInvestmentDecisionVariable;
import opdytsintegration.roadinvestment.RoadInvestmentObjectiveFunction;
import opdytsintegration.roadinvestment.RoadInvestmentStateFactory;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Kai Nagel based on Gunnar Flötteröd
 * 
 */
class KNModeChoiceCalibMain {

	static void solveFictitiousProblem() {

		System.out.println("STARTED ...");

		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler() .setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(100);
		config.global().setRandomSeed(new Random().nextLong());
		
		// ---
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// ---

		final Map<Link, Double> link2freespeed = Collections .unmodifiableMap(link2freespeed(scenario));
		final Map<Link, Double> link2capacity = Collections .unmodifiableMap(link2capacity(scenario));

		@SuppressWarnings("unchecked")
		final MATSimStateFactory<RoadInvestmentDecisionVariable> stateFactory = new RoadInvestmentStateFactory();
		
		final ObjectiveFunction objectiveFunction = new RoadInvestmentObjectiveFunction();

		final FixedIterationNumberConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion( 100, 10);

		Simulator<RoadInvestmentDecisionVariable> simulator = new MATSimSimulator<>( stateFactory, scenario, new TimeDiscretization(5 * 3600, 10 * 60, 18)); 

		DecisionVariableRandomizer<RoadInvestmentDecisionVariable> randomizer = new DecisionVariableRandomizer<RoadInvestmentDecisionVariable>() {
			@Override public List<RoadInvestmentDecisionVariable> newRandomVariations( RoadInvestmentDecisionVariable decisionVariable ) {
				return Arrays.asList(
						new RoadInvestmentDecisionVariable(
								Math.max( 0, Math.min(1, decisionVariable.betaPay() + 0.1 * MatsimRandom.getRandom().nextGaussian())), 
								Math.max( 0, Math.min(1, decisionVariable.betaAlloc() + 0.1 * MatsimRandom.getRandom().nextGaussian())),
								link2freespeed, link2capacity),
						new RoadInvestmentDecisionVariable(
								Math.max( 0, Math.min(1, decisionVariable.betaPay() + 0.1 * MatsimRandom.getRandom().nextGaussian())), 
								Math.max( 0, Math.min(1, decisionVariable.betaAlloc() + 0.1 * MatsimRandom.getRandom().nextGaussian())),
								link2freespeed, link2capacity)
						);
			}
		};

		boolean interpolate = true;
		int maxIterations = 10;
		int maxTransitions = Integer.MAX_VALUE;
		int populationSize = 10;

		final RoadInvestmentDecisionVariable initialDecisionVariable = new RoadInvestmentDecisionVariable( 
				MatsimRandom.getRandom().nextDouble(), MatsimRandom .getRandom().nextDouble(), link2freespeed, link2capacity);
		
		RandomSearch<RoadInvestmentDecisionVariable> randomSearch = new RandomSearch<>( simulator, randomizer,
				initialDecisionVariable, convergenceCriterion, maxIterations, maxTransitions, populationSize,
				MatsimRandom.getRandom(), interpolate, objectiveFunction, false);
		randomSearch.setLogFileName("./randomSearchLog.txt");
		
		// ---
		
		randomSearch.run(new SelfTuner(0.95));

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

	public static void main(String[] args) {

		// enumerateDecisionVariables();
		solveFictitiousProblem();

	}

}
