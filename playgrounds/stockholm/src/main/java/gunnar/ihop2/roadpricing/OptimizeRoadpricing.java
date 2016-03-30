package gunnar.ihop2.roadpricing;

import java.util.Set;
import java.util.logging.Logger;

import opdytsintegration.DistanceBasedFilter;
import opdytsintegration.MATSimSimulator;
import opdytsintegration.MATSimStateFactoryImpl;
import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class OptimizeRoadpricing {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		/*
		 * Create the MATSim scenario.
		 */
		final String configFileName = "./input/matsim-config.xml";
		final Config config = ConfigUtils.loadConfig(configFileName,
				new RoadPricingConfigGroup());
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
		 * Create initial toll levels and their randomization.
		 */
		// NO TOLL
		final TollLevels initialTollLevels = new TollLevels(6 * 3600 + 1800,
				7 * 3600, 7 * 3600 + 1800, 8 * 3600 + 1800, 9 * 3600,
				15 * 3600 + 1800, 16 * 3600, 17 * 3600 + 1800, 18 * 3600,
				18 * 3600 + 1800, 0.0, 0.0, 0.0, scenario);
		// THE ORIGINAL
		// final TollLevels initialTollLevels = new TollLevels(6 * 3600 + 1800,
		// 7 * 3600, 7 * 3600 + 1800, 8 * 3600 + 1800, 9 * 3600,
		// 15 * 3600 + 1800, 16 * 3600, 17 * 3600 + 1800, 18 * 3600,
		// 18 * 3600 + 1800, 10.0, 15.0, 20.0, scenario);
		// OPTIMIZED
		// final TollLevels initialTollLevels = new TollLevels(25200.0, 25200.0,
		// 28800.0, 30600.0, 32400.0, 55800.0, 59400.0, 59400.0, 61200.0,
		// 63000.0, 0.0, 10.0, 30.0, scenario);
		// THE ORIGINAL TIMES 10
		// final TollLevels initialTollLevels = new TollLevels(6 * 3600 + 1800,
		// 7 * 3600, 7 * 3600 + 1800, 8 * 3600 + 1800, 9 * 3600,
		// 15 * 3600 + 1800, 16 * 3600, 17 * 3600 + 1800, 18 * 3600,
		// 18 * 3600 + 1800, 10.0 * 10.0, 10.0 * 15.0, 10.0 * 20.0,
		//	scenario);

		final double changeTimeProba = 2.0 / 3.0;
		final double changeCostProba = 2.0 / 3.0;
		final double deltaTime_s = 1800;
		final double deltaCost_money = 10.0; // doubled
		final DecisionVariableRandomizer<TollLevels> decisionVariableRandomizer = new TollLevelsRandomizer(
				initialTollLevels, changeTimeProba, changeCostProba,
				deltaTime_s, deltaCost_money);

		/*
		 * Problem specification.
		 */
		final TimeDiscretization timeDiscretization = new TimeDiscretization(0,
				1800, 48);
		final Set<Id<Link>> relevantLinkIds = (new DistanceBasedFilter(674000,
				6581000, 6000)).allAcceptedLinkIds(scenario.getNetwork()
				.getLinks().values());
		Logger.getLogger(OptimizeRoadpricing.class.getName()).info(
				"Selected " + relevantLinkIds.size() + " out of "
						+ scenario.getNetwork().getLinks().size() + " links.");
		final ObjectiveFunction objectiveFunction = new TotalScoreObjectiveFunction();
		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				1000, 100);
		final MATSimSimulator<TollLevels> matsimSimulator = new MATSimSimulator<>(
				new MATSimStateFactoryImpl<TollLevels>(), scenario,
				timeDiscretization, relevantLinkIds, roadpricingModule);

		/*
		 * RandomSearch specification.
		 */
		final boolean interpolate = true;
		final int maxRandomSearchIterations = 1000;
		final int maxRandomSearchTransitions = Integer.MAX_VALUE;
		final int randomSearchPopulationSize = 32;
		final boolean includeCurrentBest = false;
		final RandomSearch<TollLevels> randomSearch = new RandomSearch<>(
				matsimSimulator, decisionVariableRandomizer, initialTollLevels,
				convergenceCriterion, maxRandomSearchIterations,
				maxRandomSearchTransitions, randomSearchPopulationSize,
				MatsimRandom.getRandom(), interpolate, objectiveFunction,
				includeCurrentBest);
		randomSearch.setLogFileName(originalOutputDirectory + "opdyts.log");

		/*
		 * Run it.
		 */
		randomSearch.run(0, 0);

		System.out.println("... DONE.");
	}
}
