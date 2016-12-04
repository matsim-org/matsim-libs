package cba.trianglenet;

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

	static final int outerIts = 1;
	static final int popSize = 100;
	static final double replanProba = 1.0;
	static final String expectationFilePrefix = "./testdata/cba/expectation-before-it";
	static final String experienceFilePrefix = "./testdata/cba/experience-after-it";
	static final String demandStatsFilePrefix = "./testdata/cba/demandStats-in-it";

	static final int resampleCnt = 10 * 1000;
	static final Random rnd = new Random();
	static final boolean useRealMATSimScore = false;

	static final int maxTrials = 10;
	static final int maxFailures = 3;

	final static int minLocChoiceSetSize = 1; // 1;
	final static int maxLocChoiceSetSize = 10; // 10;

	final static double carAvailProba = 0.5;

	static final int ttAvgIts = 1;

	/*-
	 * ============================================================ 
	 *      DEMAND MODEL
	 * ============================================================
	 */

	private static AverageTravelTime avgTravelTimes = null;

	private static void runDemandModel(final Scenario scenario, final int outerIt) {

		if (outerIt == 1) {
			DemandModel.createPopulation(scenario, popSize, minLocChoiceSetSize, maxLocChoiceSetSize, carAvailProba);
		}

		final TravelTime carTravelTime;
		if (outerIt == 1) {
			carTravelTime = new FreeSpeedTravelTime();
		} else {
			if (avgTravelTimes == null) {
				assert (outerIt != 2);
				avgTravelTimes = new AverageTravelTime(scenario, ttAvgIts);
			} else {
				avgTravelTimes.addData(scenario, ttAvgIts);
			}
			carTravelTime = avgTravelTimes;
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

		DemandModel.replanPopulation(resampleCnt, rnd, scenario, factory, outerIt == 1 ? 1.0 : replanProba,
				expectationFilePrefix + outerIt + ".txt", demandStatsFilePrefix + outerIt + ".txt", maxTrials,
				maxFailures, mode2tt, useRealMATSimScore);

		final PopulationWriter popwriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		popwriter.write("testdata/cba/triangle-population.xml");
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

	public static void main(String[] args) {

		// (new ObjectAttributesXmlWriter(new
		// ObjectAttributes())).writeFile("./testdata/cba/person-attributes.xml");

		System.out.println("STARTED");

		final ExperiencedScoreAnalyzer experiencedScoreAnalyzer = new ExperiencedScoreAnalyzer();

		for (int outerIt = 1; outerIt <= outerIts; outerIt++) {

			{
				System.out.println("OUTER ITERATION " + outerIt + ", running DEMAND model");

				final Config config = ConfigUtils.loadConfig("./testdata/cba/config.xml");
				config.global().setRandomSeed(new Random().nextLong());
				if (outerIt > 1) {
					config.getModule("plans").addParam("inputPlansFile", "triangle-population.xml");
				}
				final Scenario scenario = ScenarioUtils.loadScenario(config);
				runDemandModel(scenario, outerIt);
			}

			{
				System.out.println("OUTER ITERATION " + outerIt + ", running SUPPLY model");

				final Config config = ConfigUtils.loadConfig("./testdata/cba/config.xml");
				config.global().setRandomSeed(new Random().nextLong());
				config.controler()
						.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
				config.getModule("plans").addParam("inputPlansFile", "triangle-population.xml");
				final Scenario scenario = ScenarioUtils.loadScenario(config);
				runSupplyModel(scenario, outerIt, experiencedScoreAnalyzer);
			}

			System.out.println("DONE");
		}
	}
}
