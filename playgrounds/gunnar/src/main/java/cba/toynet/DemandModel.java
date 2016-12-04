package cba.toynet;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
			final String demandStatsFileName, final int maxTrials, final int maxFailures,
			final Map<String, TravelTime> mode2travelTime) {

		final ChoiceModel choiceModel = new ChoiceModel(sampleCnt, rnd, scenario, tripRouterProvider, mode2travelTime,
				maxTrials, maxFailures);

		final DemandAnalyzer demandAnalyzer = new DemandAnalyzer();
		final ResamplingTest resamplingTest = new ResamplingTest();
		final BasicStatistics coverageStats = new BasicStatistics();

		final PrintWriter expectationWriter;
		try {
			expectationWriter = new PrintWriter(expectationFileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (Math.random() < replanProba) {

				System.out.println("Replanning " + person.getId());
				
				final Set<PlanForResampling> chosenPlans = choiceModel.choosePlans(person);
				coverageStats.add(((double) chosenPlans.size()) / ((double) TourSequence.Type.values().length));

				final Sampers2MATSimResampler resampler = new Sampers2MATSimResampler(rnd, chosenPlans, sampleCnt);
				final PlanForResampling planForResampling = (PlanForResampling) resampler.next();

				final List<Double> probabilities = new ArrayList<>(chosenPlans.size());
				Integer choiceIndex = null;
				int i = 0;
				for (PlanForResampling planAlternativeForResampling : chosenPlans) {
					probabilities.add(planAlternativeForResampling.getMATSimChoiceProba());
					if (planForResampling == planAlternativeForResampling) {
						assert (choiceIndex == null);
						choiceIndex = i;
					}
					i++;
				}
				// System.out.println("CHOICE: " + choiceIndex + ", PROBAS: " + probabilities);
				resamplingTest.registerChoiceAndDistribution(choiceIndex, probabilities);
				
				final Plan plan = planForResampling.plan;
				person.getPlans().clear();
				person.setSelectedPlan(null);
				person.addPlan(plan);
				plan.setPerson(person);
				plan.setScore(planForResampling.getSampersOnlyScore() + planForResampling.getMATSimTimeScore());
				person.setSelectedPlan(plan);

				expectationWriter.println(person.getId() + "\t" + planForResampling.getSampersTimeScore() + "\t"
						+ planForResampling.getMATSimTimeScore());

				demandAnalyzer.registerChoice(planForResampling);
			}
		}
		expectationWriter.flush();
		expectationWriter.close();

		final Tuple<Double, Double> conf = resamplingTest.getBootstrap95Percent(10 * 1000, rnd);
		System.out.println("<coverage>\ttest-statistic\t95%-int.");
		System.out.println(coverageStats.getAvg() + "\t" + resamplingTest.getStatistic() + "\t[" + conf.getA()
				+ ", " + conf.getB() + "]");

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