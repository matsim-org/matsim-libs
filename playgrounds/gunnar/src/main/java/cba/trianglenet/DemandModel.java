package cba.trianglenet;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import com.google.inject.Provider;

import cba.resampling.ResamplingTest;
import cba.resampling.Sampers2MATSimResampler;
import floetteroed.utilities.Tuple;
import floetteroed.utilities.math.BasicStatistics;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class DemandModel {

	static final String choicesetsize = "choicesetsize";
	static final String caravailable = "caravailable";

	static void createPopulation(final Scenario scenario, final double approxPopSize, final int minLocChoiceSetSize,
			final int maxLocChoiceSetSize, final double carAvailProba) {

		/*
		 * Create one home location per person, according to hard-coded logic.
		 */

		final List<Link> homeLocs = new ArrayList<>();

		// fact = 1 yields 10'000 persons; see below.
		final double fact = approxPopSize / 10000.0;

		// fact = 1 => 4000 persons
		for (int i = 0; i < fact * 1000; i++) {
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("1_2")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("2_1")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("1_6")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("6_1")));
		}

		// fact = 1 => 1200 persons
		for (int i = 0; i < fact * 600; i++) {
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("5_6")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("6_5")));
		}

		// fact = 1 => 3000 persons
		for (int i = 0; i < fact * 500; i++) {
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("3_2")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("2_3")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("3_4")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("4_3")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("5_4")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("4_5")));
		}

		// fact = 1 => 1800 persons
		for (int i = 0; i < fact * 100; i++) {
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("2_8")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("8_2")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("2_7")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("7_2")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("6_7")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("7_6")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("6_9")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("9_6")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("4_8")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("8_4")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("4_9")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("9_4")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("7_8")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("8_7")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("8_9")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("9_8")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("9_7")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("7_9")));
		}

		/*
		 * Overwrite the population with corresponding only-home plans.
		 */

		scenario.getPopulation().getPersons().clear();

		for (int personNumber = 0; personNumber < homeLocs.size(); personNumber++) {
			final Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(personNumber));
			scenario.getPopulation().addPerson(person);

			final Activity homeAct = scenario.getPopulation().getFactory().createActivityFromLinkId("home",
					homeLocs.get(personNumber).getId());
			final Plan plan = scenario.getPopulation().getFactory().createPlan();
			plan.addActivity(homeAct);

			person.addPlan(plan);
			plan.setPerson(person);
			person.setSelectedPlan(plan);
		}

		/*
		 * Create AND SAVE new person attributes.
		 */

		final Random rnd = new Random();
		final ObjectAttributes personAttrs = scenario.getPopulation().getPersonAttributes();
		personAttrs.clear();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			personAttrs.putAttribute(person.getId().toString(), choicesetsize,
					minLocChoiceSetSize + rnd.nextInt(1 + maxLocChoiceSetSize - minLocChoiceSetSize));
			personAttrs.putAttribute(person.getId().toString(), caravailable, (rnd.nextDouble() < carAvailProba));
		}
		(new ObjectAttributesXmlWriter(personAttrs)).writeFile("testdata/cba/person-attributes.xml");
	}

	static void replanPopulation(final int sampleCnt, final Random rnd, final Scenario scenario,
			final Provider<TripRouter> tripRouterProvider, final double replanProba, final String expectationFileName,
			final String demandStatsFileName, final int maxTrials, final int maxFailures,
			final Map<String, TravelTime> mode2travelTime, final boolean includeMATSimScore) {

		final ChoiceModel choiceModel = new ChoiceModel(sampleCnt, rnd, scenario, tripRouterProvider, mode2travelTime,
				maxTrials, maxFailures, includeMATSimScore);
		final Map<Person, ChoiceRunnerForResampling> person2choiceRunner = new LinkedHashMap<>();

		// Replan in parallel.

		final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (Math.random() < replanProba) {
				final Link homeLoc = scenario.getNetwork().getLinks()
						.get(((Activity) person.getSelectedPlan().getPlanElements().get(0)).getLinkId());
				final int locChoiceSetSize = (Integer) scenario.getPopulation().getPersonAttributes()
						.getAttribute(person.getId().toString(), choicesetsize);
				final boolean carAvailable = (Boolean) scenario.getPopulation().getPersonAttributes()
						.getAttribute(person.getId().toString(), caravailable);

				final ChoiceRunnerForResampling choiceRunner = choiceModel.newChoiceRunner(homeLoc, person,
						locChoiceSetSize, locChoiceSetSize, carAvailable);
				person2choiceRunner.put(person, choiceRunner);
				threadPool.execute(choiceRunner);
			}
		}
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}

		// Collect replanning results.

		final DemandAnalyzer demandAnalyzer = new DemandAnalyzer();
		final ResamplingTest resamplingTest = new ResamplingTest();
		final BasicStatistics coverageStats = new BasicStatistics();

		final PrintWriter expectationWriter;
		try {
			expectationWriter = new PrintWriter(expectationFileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		System.out.println("<coverage>\ttest-statistic\t95%-int.");
		for (Map.Entry<Person, ChoiceRunnerForResampling> entry : person2choiceRunner.entrySet()) {
			final Person person = entry.getKey();

			coverageStats.add(entry.getValue().getCoverage());

			final Sampers2MATSimResampler resampler = new Sampers2MATSimResampler(rnd,
					entry.getValue().getChosenPlans(), sampleCnt);
			final PlanForResampling planForResampling = (PlanForResampling) resampler.next();

			final List<Double> probabilities = new ArrayList<>(entry.getValue().getChosenPlans().size());
			Integer choiceIndex = null;
			int i = 0;
			for (PlanForResampling planAlternativeForResampling : entry.getValue().getChosenPlans()) {
				probabilities.add(planAlternativeForResampling.getMATSimChoiceProba());
				if (planForResampling == planAlternativeForResampling) {
					assert (choiceIndex == null);
					choiceIndex = i;
				}
				i++;
			}
			resamplingTest.registerChoiceAndDistribution(choiceIndex, probabilities);
			final Tuple<Double, Double> conf = resamplingTest.getBootstrap95Percent(10 * 1000, rnd);
			System.out.println(coverageStats.getAvg() + "\t" + resamplingTest.getStatistic() + "\t[" + conf.getA()
					+ ", " + conf.getB() + "]");

			final Plan plan = planForResampling.plan;
			person.getPlans().clear();
			person.setSelectedPlan(null);
			person.addPlan(plan);
			plan.setPerson(person);
			person.setSelectedPlan(plan);

			/*
			 * The logic
			 */

			if (includeMATSimScore) {
				expectationWriter.println(person.getId() + "\t" + planForResampling.getMATSimTimeScore());
			} else {
				expectationWriter.println(person.getId() + "\t" + planForResampling.getSampersTimeScore());
			}
			demandAnalyzer.registerChoice(plan, planForResampling.getSampersOnlyScore());
		}
		expectationWriter.flush();
		expectationWriter.close();

		final PrintWriter demandStatsWriter;
		try {
			demandStatsWriter = new PrintWriter(demandStatsFileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		demandStatsWriter.println(demandAnalyzer.toString());
		demandStatsWriter.flush();
		demandStatsWriter.close();
	}
}
