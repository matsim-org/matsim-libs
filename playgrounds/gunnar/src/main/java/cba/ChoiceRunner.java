package cba;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;

import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ChoiceRunner implements Runnable {

	private final Scenario scenario;

	private final Link homeLoc;

	private final Person person;

	private final List<TourSequence> alternatives;

	private final UtilityFunction utilityFunction;

	private Plan chosenPlan = null;

	ChoiceRunner(final Scenario scenario, final Provider<TripRouter> tripRouterProvider,
			final Map<String, TravelTime> mode2travelTime, final Link homeLoc, final Person person,
			final List<TourSequence> tourSeqAlternatives, final int maxTrials, final int maxFailures) {
		this.scenario = scenario;
		this.homeLoc = homeLoc;
		this.person = person;
		this.alternatives = tourSeqAlternatives;
		this.utilityFunction = new UtilityFunction(scenario, tripRouterProvider, mode2travelTime, maxTrials,
				maxFailures);
	}

	Plan getChosenPlan() {
		return this.chosenPlan;
	}

	@Override
	public void run() {

		final List<Plan> planAlternatives = new ArrayList<>(this.alternatives.size());

		// compute utilities
		final List<Double> utilities = new ArrayList<>(this.alternatives.size());
		double maxUtility = Double.NEGATIVE_INFINITY;
		for (TourSequence alternative : this.alternatives) {
			final Plan plan = alternative.asPlan(this.scenario, homeLoc.getId(), this.person);
			planAlternatives.add(plan);
			this.utilityFunction.evaluate(plan);
			plan.setScore(this.utilityFunction.getMATSimUtility());
			final double utility = this.utilityFunction.getTotalUtility();
			utilities.add(utility);
			maxUtility = Math.max(maxUtility, utility);
		}

		// simulate choice
		final Vector probas = new Vector(utilities.size());
		for (int i = 0; i < utilities.size(); i++) {
			probas.set(i, Math.exp(utilities.get(i) - maxUtility));
		}
		probas.makeProbability();
		final int chosenIndex = MathHelpers.draw(probas, MatsimRandom.getRandom());

		this.chosenPlan = planAlternatives.get(chosenIndex);

		System.out.println("replanned person " + this.person.getId());
	}

}
