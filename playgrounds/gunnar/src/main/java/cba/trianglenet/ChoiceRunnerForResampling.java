package cba.trianglenet;

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

import cba.resampling.MyGumbelDistribution;
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

	private final boolean includeMatsimScore;

	private Set<PlanForResampling> chosenPlans = null;

	Set<PlanForResampling> getChosenPlans() {
		return this.chosenPlans;
	}

	double getCoverage() {
		return ((double) this.chosenPlans.size()) / ((double) this.alternatives.size());
	}

	ChoiceRunnerForResampling(final int sampleCnt, final Random rnd, final Scenario scenario,
			final Provider<TripRouter> tripRouterProvider, final Map<String, TravelTime> mode2travelTime,
			final Link homeLoc, final Person person, final List<TourSequence> tourSeqAlternatives, final int maxTrials,
			final int maxFailures, final boolean includeMatsimScore) {
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
		this.includeMatsimScore = includeMatsimScore;
	}

	@Override
	public void run() {

		// using a copy of mode2travelTime for thread safety

		final UtilityFunction utilityFunction = new UtilityFunction(this.scenario, this.tripRouterProvider,
				new LinkedHashMap<>(this.mode2travelTime), this.maxTrials, this.maxFailures);

		// create plan alternatives and Sampers and Matsim choice model

		final List<Plan> planAlternatives = new ArrayList<>(this.alternatives.size());

		final List<Double> sampersOnlyScores = new ArrayList<>(this.alternatives.size());
		final List<Double> teleportationTimeScores = new ArrayList<>(this.alternatives.size());
		final List<Double> matsimTimeScores = new ArrayList<>(this.alternatives.size());

		final MultinomialLogit sampersAndTeleportationMNL = new MultinomialLogit(this.alternatives.size(), 1);
		sampersAndTeleportationMNL.setUtilityScale(1.0);
		sampersAndTeleportationMNL.setCoefficient(0, 1.0);

		final MultinomialLogit sampersAndMatsimMNL = new MultinomialLogit(this.alternatives.size(), 1);
		sampersAndMatsimMNL.setUtilityScale(1.0);
		sampersAndMatsimMNL.setCoefficient(0, 1.0);

		for (int i = 0; i < this.alternatives.size(); i++) {
			final Plan plan = this.alternatives.get(i).asPlan(this.scenario, homeLoc.getId(), this.person);
			planAlternatives.add(plan);
			utilityFunction.evaluate(plan);

			final double sampersOnlyScore = utilityFunction.getSampersOnlyUtility();
			final double teleportationTimeScore = utilityFunction.getMATSimTeleportationTravelTimeUtility();
			final double matsimTimeScore = utilityFunction.getMATSimOnlyUtility();

			sampersOnlyScores.add(sampersOnlyScore);
			teleportationTimeScores.add(teleportationTimeScore);
			matsimTimeScores.add(matsimTimeScore);

			plan.setScore(matsimTimeScore);

			sampersAndTeleportationMNL.setAttribute(i, 0, sampersOnlyScore + teleportationTimeScore);
			sampersAndTeleportationMNL.setASC(i, 0.0);

			sampersAndMatsimMNL.setAttribute(i, 0, sampersOnlyScore + matsimTimeScore);
			sampersAndMatsimMNL.setASC(i, 0.0);
		}
		sampersAndTeleportationMNL.enforcedUpdate();
		sampersAndMatsimMNL.enforcedUpdate();

		// simulate choices

		final Map<Integer, PlanForResampling> plansForResampling = new LinkedHashMap<>(this.sampleCnt);
		for (int i = 0; i < this.sampleCnt; i++) {
			final int planIndex;
			if (this.includeMatsimScore) {
				planIndex = sampersAndMatsimMNL.draw(this.rnd);
			} else {
				planIndex = sampersAndTeleportationMNL.draw(this.rnd);
			}
			final Plan plan = planAlternatives.get(planIndex);
			final PlanForResampling planForResampling = new PlanForResampling(plan, sampersOnlyScores.get(planIndex),
					teleportationTimeScores.get(planIndex), matsimTimeScores.get(planIndex),
					sampersAndTeleportationMNL.getProbs().get(planIndex), new MyGumbelDistribution(1.0));
			planForResampling.setMATSimChoiceProba(sampersAndMatsimMNL.getProbs().get(planIndex));

			plansForResampling.put(planIndex, planForResampling);
		}

		this.chosenPlans = new LinkedHashSet<>(plansForResampling.values());

		System.out.println("computed " + this.chosenPlans.size() + " alternatives for person " + this.person.getId()
				+ ". Coverage = " + (100.0 * (double) this.chosenPlans.size()) / ((double) this.alternatives.size())
				+ " percent.");
	}
}
