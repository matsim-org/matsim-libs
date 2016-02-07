package floetteroed.opdyts.example.roadpricing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import opdytsintegration.MATSimSimulator;
import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.RandomizedCharyparNagelScoringFunctionFactory;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.utilities.Units;
import floetteroed.utilities.config.ConfigReader;
import floetteroed.utilities.math.MathHelpers;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class OptimizeRoadpricing {

	static TollLevels randomInitialTollLevels(final double deltaTime_s,
			final double deltaCost_money, final Scenario scenario) {

		final List<Double> times = new ArrayList<>();
		{
			final int timeBinCnt = MathHelpers.round(Units.S_PER_D
					/ deltaTime_s);
			for (int i = 0; i < 10; i++) {
				times.add(deltaTime_s
						* MatsimRandom.getRandom().nextInt(timeBinCnt + 1));
			}
			Collections.sort(times);
		}

		final List<Double> costs = new ArrayList<>();
		{
			final int costBinCnt = MathHelpers.round(5.0 / deltaCost_money);
			for (int i = 0; i < 3; i++) {
				costs.add(deltaCost_money
						* MatsimRandom.getRandom().nextInt(costBinCnt + 1));
			}
			Collections.sort(costs);
		}

		return new TollLevels(times, costs, scenario);
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		/*
		 * Build the test scenario.
		 */
		final Config config = ConfigUtils.loadConfig(
				"./input/roadpricing/config.xml", new RoadPricingConfigGroup());

		// final boolean split = false;
		// final int detourOffset = 8;
		// final int outerPopSize = 5000;
		// final int innerPopSize = 5000;
		// final int otherPopSize = 0;
		// final double linkLength = 2000;
		// final RoadpricingScenarioBuilder builder = new
		// RoadpricingScenarioBuilder(
		// config, linkLength, outerPopSize, innerPopSize, otherPopSize,
		// split, detourOffset);
		// builder.build();
		// final NetworkWriter netWriter = new
		// NetworkWriter(builder.getNetwork());
		// netWriter.write("./input/roadpricing/network.xml");
		// final PopulationWriter popWriter = new PopulationWriter(
		// builder.getPopulation(), builder.getNetwork());
		// popWriter.write("./input/roadpricing/plans.xml");
		// final ObjectAttributesXmlWriter popAttrWriter = new
		// ObjectAttributesXmlWriter(
		// builder.getPopulation().getPersonAttributes());
		// popAttrWriter
		// .writeFile("./input/roadpricing/population-attributes.xml");

		final floetteroed.utilities.config.Config myConfig = (new ConfigReader())
				.read(args[0]);
		final int randomSearchPopulationSize = Integer.parseInt(myConfig.get(
				"opdyts", "popsize"));
		final boolean includeCurrentBest = (randomSearchPopulationSize == 1);
		final double deltaCost_money = Double.parseDouble(myConfig.get(
				"opdyts", "deltacost"));
		final int maxSimIterations = Integer.parseInt(myConfig.get("opdyts",
				"maxsimiterations"));
		final int averageIterations = Integer.parseInt(myConfig.get("opdyts",
				"simavgiterations"));
		final boolean randomInitialPoint = Boolean.parseBoolean(myConfig.get(
				"opdyts", "randominitialpoint"));
		final boolean parallelSampling = Boolean.parseBoolean(myConfig.get(
				"opdyts", "parallelsampling"));

		final double initialEquilibriumWeight = Double.parseDouble(myConfig
				.get("opdyts", "equilibriumweight"));
		final double initialUniformityWeight = Double.parseDouble(myConfig.get(
				"opdyts", "uniformityweight"));
		final boolean adjustWeights = Boolean.parseBoolean(myConfig.get(
				"opdyts", "adjustweights"));

		/*
		 * Create the MATSim scenario.
		 */
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final String originalOutputDirectory = scenario.getConfig().controler()
				.getOutputDirectory(); // gets otherwise overwritten in config
		final RoadPricingConfigGroup roadPricingConfigGroup = ConfigUtils
				.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME,
						RoadPricingConfigGroup.class);
		final RoadPricingSchemeImpl roadPricingScheme = new RoadPricingSchemeImpl();
		new RoadPricingReaderXMLv1(roadPricingScheme)
				.parse(roadPricingConfigGroup.getTollLinksFile());
		final AbstractModule roadpricingModule = new ControlerDefaultsWithRoadPricingModule(
				roadPricingScheme);

		/*
		 * Create initial toll levels.
		 */
		final double deltaTime_s = 1800;
		final TollLevels initialTollLevels;
		if (randomInitialPoint) {
			initialTollLevels = randomInitialTollLevels(deltaTime_s,
					deltaCost_money, scenario);
		} else {
			initialTollLevels = new TollLevels(8 * 3600, 9 * 3600, 10 * 3600,
					11 * 3600, 12 * 3600, 14 * 3600, 15 * 3600, 16 * 3600,
					17 * 3600, 18 * 3600, 0.0, 0.0, 0.0, scenario);
		}
		final double changeTimeProba = 2.0 / 3.0;
		final double changeCostProba = 2.0 / 3.0;
		final DecisionVariableRandomizer<TollLevels> decisionVariableRandomizer = new TollLevelsRandomizer(
				initialTollLevels, changeTimeProba, changeCostProba,
				deltaTime_s, deltaCost_money);

		/*
		 * Problem specification.
		 */
		final TimeDiscretization timeDiscretization = new TimeDiscretization(0,
				1800, 48);
		final Set<Id<Link>> relevantLinkIds = scenario.getNetwork().getLinks()
				.keySet();
		final double tollEffectivity = 0.9;
		final ObjectiveFunction objectiveFunction = new RoadpricingObjectiveFunction(
				tollEffectivity);
		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				maxSimIterations, averageIterations);
		final double occupancyScale = 1.0;
		final double tollScale = 0.0;
		final MATSimSimulator<TollLevels> matsimSimulator = new MATSimSimulator<>(
				new RoadpricingStateFactory(timeDiscretization, occupancyScale,
						tollScale), scenario, timeDiscretization,
				relevantLinkIds, roadpricingModule);
		matsimSimulator
				.setScoringFunctionFactory(new RandomizedCharyparNagelScoringFunctionFactory(
						scenario));

		/*
		 * RandomSearch specification.
		 */
		final int maxMemorizedTrajectoryLength = Integer.MAX_VALUE;
		final int maxRandomSearchIterations = 1000;
		final int maxRandomSearchTransitions = Integer.MAX_VALUE;
		final RandomSearch<TollLevels> randomSearch = new RandomSearch<>(
				matsimSimulator, decisionVariableRandomizer, initialTollLevels,
				convergenceCriterion, maxRandomSearchIterations,
				maxRandomSearchTransitions, randomSearchPopulationSize,
				MatsimRandom.getRandom(), parallelSampling, objectiveFunction,
				maxMemorizedTrajectoryLength, includeCurrentBest);
		randomSearch.setLogFileName(originalOutputDirectory + "opdyts.log");

		/*
		 * Run it.
		 */
		randomSearch.run(initialEquilibriumWeight, initialUniformityWeight,
				adjustWeights);

		System.out.println("... DONE.");
	}
}
