package cba.toynet;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.Provider;

import matsimintegration.TimeDiscretizationInjection;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Main {

	/*-
	 * ============================================================ 
	 *      PARAMETER SETTINGS
	 * ============================================================
	 */

	static final double sampersLogitScale = 0.1;
	static final double betaTravelSampers_1_h = -12.0;

	static final boolean usePTto1 = true; // true in base case
	static final boolean usePTto2 = false; // false in base case

	static final int outerIts = 100;
	static final int popSize = 1000;
	static final Double replanProba = null; // NULL means 1/outerIteration
	static final String expectationFilePrefix = "./output/cba/toynet/expectation-before-it";
	static final String experienceFilePrefix = "./output/cba/toynet/experience-after-it";
	static final String demandStatsFilePrefix = "./output/cba/toynet/demandStats-in-it";
	static final String populationLogFileName = "./output/cba/toynet/populationLog.txt";
	static final String travelTimeLogFileName = "./output/cba/toynet/traveltimeLog.txt";

	static final int resampleCnt = 10000; // resample cnt 1 yields "plain
											// sampers"; 10000 yields
											// "corrected"
	static final Random rnd = new Random();

	static final int maxTrials = 10;
	static final int maxFailures = 3;

	static final int ttAvgIts = Integer.MAX_VALUE;

	/*-
	 * ============================================================ 
	 *      DEMAND MODEL
	 * ============================================================
	 */

	private static void runDemandModel(final Scenario scenario, final int outerIt) {

		if (outerIt == 1) {
			DemandModel.createPopulation(scenario, popSize);
		}

		final TravelTime carTravelTime;
		if (outerIt == 1) {
			carTravelTime = new FreeSpeedTravelTime();
		} else {
			if (avgTTsAcrossRuns == null) {
				assert (outerIt == 2);
				avgTTsAcrossRuns = new AverageTravelTimeAcrossRuns(ttAvgIts, travelTimeLogFileName);
			}
			avgTTsAcrossRuns.addData(scenario);
			carTravelTime = avgTTsAcrossRuns;
		}

		final com.google.inject.Injector injector = org.matsim.core.controler.Injector
				.createInjector(scenario.getConfig(), new AbstractModule() {
					@Override
					public void install() {
						install(AbstractModule.override(Arrays.asList(new TripRouterModule()), new AbstractModule() {
							@Override
							public void install() {
								install(new ScenarioByInstanceModule(scenario));
								addTravelTimeBinding("car").toInstance(carTravelTime);
								install(new TravelDisutilityModule());
							}
						}));
					}
				});
		final Provider<TripRouter> factory = injector.getProvider(TripRouter.class);

		final Map<String, TravelTime> mode2tt = new LinkedHashMap<>();
		mode2tt.put("car", carTravelTime);

		final SampersCarDelay sampersCarDelay = new SampersCarDelay(new TimeDiscretization(6 * 3600, 3600, 16),
				carTravelTime, scenario.getNetwork());

		final double usedReplanProba;
		if (replanProba == null) {
			usedReplanProba = 1.0 / outerIt;
		} else if (outerIt == 1) {
			usedReplanProba = 1.0;
		} else {
			usedReplanProba = replanProba;
		}

		DemandModel.replanPopulation(resampleCnt, rnd, scenario, factory, usedReplanProba,
				expectationFilePrefix + outerIt + ".txt", demandStatsFilePrefix + outerIt + ".txt", maxTrials,
				maxFailures, usePTto1, usePTto2, mode2tt, betaTravelSampers_1_h, sampersCarDelay, sampersLogitScale,
				populationAnalyzer);

		final PopulationWriter popwriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		popwriter.write("./input/cba/toynet/population.xml");
	}

	/*-
	 * ============================================================ 
	 *      SUPPLY MODEL
	 * ============================================================
	 */

	private static void runSupplyModel(final Scenario scenario, final int outerIt,
			final ExperiencedScoreAnalyzer experiencedScoreAnalyzer) {
		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TimeDiscretizationInjection.class);
			}
		});
		controler.run();

		for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			experiencedScoreAnalyzer.add(person.getId(), person.getSelectedPlan().getScore());
		}
		final PrintWriter writer;
		try {
			writer = new PrintWriter(experienceFilePrefix + outerIt + ".txt");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		writer.println(experiencedScoreAnalyzer.toString());
		writer.flush();
		writer.close();
	}

	/*-
	 * ============================================================ 
	 *      MAIN LOOP
	 * ============================================================
	 */

	private static AverageTravelTimeAcrossRuns avgTTsAcrossRuns = null;

	private static PopulationAnalyzer populationAnalyzer = null;

	public static void main(String[] args) {

		System.out.println("STARTED");

		final ExperiencedScoreAnalyzer experiencedScoreAnalyzer = new ExperiencedScoreAnalyzer();

		populationAnalyzer = new PopulationAnalyzer(populationLogFileName);

		for (int outerIt = 1; outerIt <= outerIts; outerIt++) {

			{
				System.out.println("OUTER ITERATION " + outerIt + ", running DEMAND model");

				final Config config = ConfigUtils.loadConfig("./input/cba/toynet/config.xml");
				config.global().setRandomSeed(new Random().nextLong());
				if (outerIt > 1) {
					config.getModule("plans").addParam("inputPlansFile", "population.xml");
				}
				final Scenario scenario = ScenarioUtils.loadScenario(config);
				runDemandModel(scenario, outerIt);
			}

			{
				System.out.println("OUTER ITERATION " + outerIt + ", running SUPPLY model");

				final Config config = ConfigUtils.loadConfig("./input/cba/toynet/config.xml");
				config.global().setRandomSeed(new Random().nextLong());
				config.controler()
						.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
				config.getModule("plans").addParam("inputPlansFile", "population.xml");
				final Scenario scenario = ScenarioUtils.loadScenario(config);
				runSupplyModel(scenario, outerIt, experiencedScoreAnalyzer);

				for (Person person : scenario.getPopulation().getPersons().values()) {
					populationAnalyzer.registerExperiencedScore(person);
				}
				populationAnalyzer.dumpAnalysis();
				populationAnalyzer.clear();
			}

			System.out.println("DONE");
		}
	}
}
