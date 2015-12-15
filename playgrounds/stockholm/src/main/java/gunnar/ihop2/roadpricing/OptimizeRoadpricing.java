package gunnar.ihop2.roadpricing;

import opdytsintegration.MATSimSimulator;
import opdytsintegration.MATSimState;
import opdytsintegration.MATSimStateFactory;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class OptimizeRoadpricing {

	OptimizeRoadpricing() {
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String configFileName = "./input/matsim-config.xml";

		final Config config = ConfigUtils.loadConfig(configFileName,
				new RoadPricingConfigGroup());
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final int decisionVariableCnt = 10 + 3;
		final double changeTimeProba = 2.0 / decisionVariableCnt;
		final double changeCostProba = 2.0 / decisionVariableCnt;
		final double deltaTime_s = 1800;
		final double deltaCost_money = 10.0;
		final DecisionVariableRandomizer<TollLevels> decisionVariableRandomizer = new TollLevelsRandomizer(
				changeTimeProba, changeCostProba, deltaTime_s, deltaCost_money,
				MatsimRandom.getRandom(), scenario);

		int maxMemoryLength = 10;
		boolean keepBestSolution = true;
		boolean interpolate = true;
		int maxIterations = 3;
		int maxTransitions = 100;
		int populationSize = 5;

		final ObjectiveFunction objectiveFunction = new TotalScoreObjectiveFunction();
		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				3, 1);
		final RandomSearch<TollLevels> randomSearch = new RandomSearch<>(
				new MATSimSimulator<TollLevels>(new MATSimStateFactory() {
					@Override
					public MATSimState newState(final Population population,
							final Vector stateVector,
							final DecisionVariable decisionVariable) {
						return new MATSimState(population, stateVector);
					}
				}, scenario, new ControlerDefaultsWithRoadPricingModule()),
				decisionVariableRandomizer, convergenceCriterion,
				maxIterations, maxTransitions, populationSize,
				MatsimRandom.getRandom(), interpolate, keepBestSolution,
				objectiveFunction, maxMemoryLength);
		randomSearch.setLogFileName(scenario.getConfig().controler()
				.getOutputDirectory()
				+ "optimization.log");
		randomSearch.run();

		System.out.println("... DONE.");
	}
}
