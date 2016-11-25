package cba;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;

import cba.resampling.LogitEpsilonDistribution;
import floetteroed.utilities.math.MultinomialLogit;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ChoiceRunnerForResampling implements Runnable {

	private final int sampleCnt;

	private final Random rnd;

	private final Scenario scenario;

	private final Link homeLoc;

	private final Person person;

	private final List<TourSequence> alternatives;

	private final Provider<TripRouter> tripRouterProvider;

	private final Map<String, TravelTime> mode2travelTime;

	private final int maxTrials;

	private final int maxFailures;

	private Set<PlanForResampling> chosenPlans = null;

	ChoiceRunnerForResampling(final int sampleCnt, final Random rnd, final Scenario scenario,
			final Provider<TripRouter> tripRouterProvider, final Map<String, TravelTime> mode2travelTime,
			final Link homeLoc, final Person person, final List<TourSequence> tourSeqAlternatives, final int maxTrials,
			final int maxFailures) {
		this.sampleCnt = sampleCnt;
		this.rnd = rnd;
		this.scenario = scenario;
		this.homeLoc = homeLoc;
		this.person = person;
		this.alternatives = tourSeqAlternatives;
		this.tripRouterProvider = tripRouterProvider;
		this.mode2travelTime = mode2travelTime;
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
	}

	@Override
	public void run() {

		// using a copy of mode2travelTime for thread safety

		final UtilityFunction utilityFunction = new UtilityFunction(this.scenario, this.tripRouterProvider,
				new LinkedHashMap<>(this.mode2travelTime), this.maxTrials, this.maxFailures);

		// create plan alternatives as well as Sampers and Matsim choice model

		final List<Plan> planAlternatives = new ArrayList<>(this.alternatives.size());
		final List<Double> sampersOnlyScores = new ArrayList<>(this.alternatives.size());

		final double scale = 1.0;
		final MultinomialLogit sampersOnlyMNL = new MultinomialLogit(this.alternatives.size(), 1);
		sampersOnlyMNL.setUtilityScale(scale);
		sampersOnlyMNL.setCoefficient(0, 1.0);

		final MultinomialLogit sampersAndMatsimMNL = new MultinomialLogit(this.alternatives.size(), 1);
		sampersAndMatsimMNL.setUtilityScale(scale);
		sampersAndMatsimMNL.setCoefficient(0, 1.0);

		for (int i = 0; i < planAlternatives.size(); i++) {
			final Plan plan = this.alternatives.get(i).asPlan(this.scenario, homeLoc.getId(), this.person);
			planAlternatives.add(plan);

			utilityFunction.evaluate(plan);
			sampersOnlyScores.add(utilityFunction.getSampersOnlyUtility());
			plan.setScore(utilityFunction.getMATSimOnlyUtility());

			sampersOnlyMNL.setAttribute(i, 0, utilityFunction.getSampersOnlyUtility());
			sampersOnlyMNL.setASC(i, 0.0);

			sampersAndMatsimMNL.setAttribute(i, 0,
					utilityFunction.getSampersOnlyUtility() + utilityFunction.getMATSimOnlyUtility());
			sampersAndMatsimMNL.setASC(i, 0.0);
		}
		sampersOnlyMNL.enforcedUpdate();
		sampersAndMatsimMNL.enforcedUpdate();

		// simulate choices

		final Map<Integer, PlanForResampling> plansForResampling = new LinkedHashMap<>(this.sampleCnt);
		for (int i = 0; i < this.sampleCnt; i++) {
			final int planIndex = sampersAndMatsimMNL.draw(this.rnd);
			final Plan plan = planAlternatives.get(i);
			plansForResampling.put(planIndex,
					new PlanForResampling(plan, sampersOnlyScores.get(planIndex),
							sampersOnlyScores.get(planIndex) + plan.getScore(),
							sampersOnlyMNL.getProbs().get(planIndex), new LogitEpsilonDistribution(scale)));
		}

		this.chosenPlans = new LinkedHashSet<>(plansForResampling.values());

		System.out.println("computed " + this.chosenPlans + " alternatives for person " + this.person.getId());
	}
}
