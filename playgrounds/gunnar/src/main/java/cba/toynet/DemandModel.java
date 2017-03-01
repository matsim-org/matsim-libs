package cba.toynet;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;

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

	static void createPopulation(final Scenario scenario, final int popSize) {
		scenario.getPopulation().getPersons().clear();

		for (int personNumber = 0; personNumber < popSize; personNumber++) {
			final Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(personNumber));
			scenario.getPopulation().addPerson(person);

			final Activity homeAct = scenario.getPopulation().getFactory().createActivityFromLinkId("home",
					Id.createLinkId(TourSequence.homeLoc));
			final Plan plan = scenario.getPopulation().getFactory().createPlan();
			plan.addActivity(homeAct);

			person.addPlan(plan);
			plan.setPerson(person);
			person.setSelectedPlan(plan);
		}
	}

	static void replanPopulation(final int sampleCnt, final Random rnd, final Scenario scenario,
			final Provider<TripRouter> tripRouterProvider, final double replanProba, final String expectationFileName,
			final String demandStatsFileName, final int maxTrials, final int maxFailures, final boolean usePTto1,
			final boolean usePTto2, final Map<String, TravelTime> mode2travelTime, final double betaTravelSampers_1_h,
			final SampersCarDelay sampersCarDelay, final double sampersLogitScale,
			final PopulationAnalyzer populationAnalyzer) {

		final ChoiceModel choiceModel = new ChoiceModel(sampleCnt, rnd, scenario, tripRouterProvider, mode2travelTime,
				maxTrials, maxFailures, usePTto1, usePTto2, betaTravelSampers_1_h, sampersCarDelay, sampersLogitScale);

		// create proposal choice sets (in parallel)

		final Map<Person, ChoiceRunner> person2ChoiceRunner = new LinkedHashMap<>();
		ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (Math.random() < replanProba) {
				final ChoiceRunner choiceRunner = choiceModel.newChoiceRunner(person);
				person2ChoiceRunner.put(person, choiceRunner);
				threadPool.execute(choiceRunner);
			}
		}
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}

		// >>>>>>>>>> NEW FOR TESTING >>>>>>>>>>
		//
		// for (Map.Entry<Person, ChoiceRunner> person2ChoiceRunnerEntry :
		// person2ChoiceRunner.entrySet()) {
		// System.out.println("PERSON " +
		// person2ChoiceRunnerEntry.getKey().getId() + " HAS "
		// + person2ChoiceRunnerEntry.getValue().getResult().size() + "
		// ALTERNATIVES.");
		// System.out.println();
		// for (PlanForResampling planForResampling :
		// person2ChoiceRunnerEntry.getValue().getResult()) {
		// System.out.println(planForResampling);
		// System.out.println();
		// }
		// System.out.println();
		// System.out.println();
		// }
		//
		// <<<<<<<<<< NEW FOR TESTING <<<<<<<<<<

		// simulate choices (in parallel)

		final DemandAnalyzer demandAnalyzer = new DemandAnalyzer();
		final ResamplingTest resamplingTest = new ResamplingTest();
		final BasicStatistics coverageStats = new BasicStatistics();

		final Map<Person, Sampers2MATSimResampler> person2Resampler = new LinkedHashMap<>();
		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		for (Map.Entry<Person, ChoiceRunner> person2ChoiceRunnerEntry : person2ChoiceRunner.entrySet()) {
			final Person person = person2ChoiceRunnerEntry.getKey();
			final Set<PlanForResampling> choiceSet = person2ChoiceRunnerEntry.getValue().getResult();
			coverageStats.add(((double) choiceSet.size()) / ((double) TourSequence.Type.values().length));
			final Sampers2MATSimResampler resampler = new Sampers2MATSimResampler(rnd, choiceSet, sampleCnt);
			person2Resampler.put(person, resampler);
			threadPool.execute(resampler);
		}
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}

		// collect results

		final PrintWriter expectationWriter;
		try {
			expectationWriter = new PrintWriter(expectationFileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		for (Map.Entry<Person, Sampers2MATSimResampler> person2ResamplerEntry : person2Resampler.entrySet()) {
			final Person person = person2ResamplerEntry.getKey();
			System.out.println("Collecting results for " + person.getId());
			final Sampers2MATSimResampler resampler = person2ResamplerEntry.getValue();
			final PlanForResampling planForResampling = (PlanForResampling) resampler.getResult();
			final Set<PlanForResampling> choiceSet = person2ChoiceRunner.get(person).getResult();

			final List<Double> probabilities = new ArrayList<>(choiceSet.size());
			Integer choiceIndex = null;
			int i = 0;
			for (PlanForResampling planAlternativeForResampling : choiceSet) {
				probabilities.add(planAlternativeForResampling.getMATSimChoiceProba());
				if (planForResampling == planAlternativeForResampling) {
					assert (choiceIndex == null);
					choiceIndex = i;
				}
				i++;
			}
			resamplingTest.registerChoiceAndDistribution(choiceIndex, probabilities);

			final Plan plan = planForResampling.plan;
			person.getPlans().clear();
			person.setSelectedPlan(null);
			person.addPlan(plan);
			plan.setPerson(person);
			plan.setScore(planForResampling.getSampersOnlyScore() + planForResampling.getSampersEpsilonRealization()
					+ planForResampling.getMATSimTimeScore());
			person.setSelectedPlan(plan);
			populationAnalyzer.registerChosenPlan(planForResampling);

			expectationWriter.println(person.getId() + "\t" + planForResampling.getSampersTimeScore() + "\t"
					+ planForResampling.getMATSimTimeScore());

			demandAnalyzer.registerChoice(planForResampling);
		}

		expectationWriter.flush();
		expectationWriter.close();

		final Tuple<Double, Double> conf = resamplingTest.getBootstrap95Percent(10 * 1000, rnd);
		System.out.println("<coverage>\ttest-statistic\t95%-int.");
		System.out.println(coverageStats.getAvg() + "\t" + resamplingTest.getStatistic() + "\t[" + conf.getA() + ", "
				+ conf.getB() + "]");

		final PrintWriter demandStatsWriter;
		try {
			demandStatsWriter = new PrintWriter(demandStatsFileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		demandStatsWriter.println(demandAnalyzer.toString());
		demandStatsWriter.flush();
		demandStatsWriter.close();

		// >>>>> TODO NEW >>>>>
		// System.out.println();
		// System.out.println(choiceModel.getLastUtilitiesToString());
		// System.out.println();
		// System.out.println("TERMINATED BY System.exit(0);");
		// System.exit(0);
		// <<<<< TODO NEW <<<<<

	}
}