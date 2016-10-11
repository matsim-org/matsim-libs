package cba;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouter;

import com.google.inject.Provider;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ChoiceModel {

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final Provider<TripRouter> tripRouterProvider;

	private final List<TourSequence> alternatives;

	private final int maxTrials;

	private final int maxFailures;

	// -------------------- CONSTRUCTION --------------------

	static List<TourSequence> newTourSeqAlternatives(final Scenario scenario) {
		/*
		 * CREATE ALL ALTERNATIVES ONCE.
		 */

		final List<TourSequence> tmpAlternatives = new ArrayList<>();

		// NO TOUR -- TODO special departure time case, omitted
		// {
		// final TourSequence alternative = new TourSequence();
		// tmpAlternatives.add(alternative);
		// }

		// ONE TOUR (WORK OR OTHER)
		{
			for (Link loc : scenario.getNetwork().getLinks().values()) {
				for (Tour.Act act : new Tour.Act[] { Tour.Act.work, Tour.Act.other }) {
					for (Tour.Mode mode : Tour.Mode.values()) {
						final TourSequence alternative = new TourSequence();
						alternative.tours.add(new Tour(loc, act, mode));
						tmpAlternatives.add(alternative);
					}
				}
			}
		}

		// TWO TOURS (WORK, THEN OTHER)
		{
			for (Link workLoc : scenario.getNetwork().getLinks().values()) {
				for (Tour.Mode workMode : Tour.Mode.values()) {
					for (Link otherLoc : scenario.getNetwork().getLinks().values()) {
						for (Tour.Mode otherMode : Tour.Mode.values()) {
							final TourSequence alternative = new TourSequence();
							alternative.tours.add(new Tour(workLoc, Tour.Act.work, workMode));
							alternative.tours.add(new Tour(otherLoc, Tour.Act.other, otherMode));
							tmpAlternatives.add(alternative);
						}
					}
				}
			}
		}
		return Collections.unmodifiableList(tmpAlternatives);
	}

	ChoiceModel(final Scenario scenario, final Provider<TripRouter> tripRouterProvider, final int maxTrials,
			final int maxFailures) {
		this.scenario = scenario;
		this.tripRouterProvider = tripRouterProvider;
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
		this.alternatives = newTourSeqAlternatives(scenario);

	}

	// -------------------- IMPLEMENTATION --------------------

	ChoiceRunner newChoiceRunner(final Link homeLoc, final Person person) {
		return new ChoiceRunner(this.scenario, this.tripRouterProvider, homeLoc, person, this.alternatives,
				this.maxTrials, this.maxFailures);
	}

	static Plan selectUniformly(final Link homeLoc, final Person person,
			final List<TourSequence> tourSequenceAlternatives, final Scenario scenario) {
		return tourSequenceAlternatives.get(MatsimRandom.getRandom().nextInt(tourSequenceAlternatives.size()))
				.asPlan(scenario, homeLoc.getId(), person);
	}

	// Plan simulateChoice(final Link homeLoc, final Person person) {
	//
	// final List<Plan> planAlternatives = new
	// ArrayList<>(this.alternatives.size());
	//
	// // compute utilities
	// final List<Double> utilities = new ArrayList<>(this.alternatives.size());
	// double maxUtility = Double.NEGATIVE_INFINITY;
	// for (TourSequence alternative : this.alternatives) {
	// final Plan plan = alternative.asPlan(this.scenario, homeLoc.getId(),
	// person);
	// planAlternatives.add(plan);
	// final double utility = this.utilityFunction.getUtility(plan);
	// plan.setScore(utility);
	// utilities.add(utility);
	// maxUtility = Math.max(maxUtility, utility);
	// }
	//
	// // simulate choice
	// final Vector probas = new Vector(utilities.size());
	// for (int i = 0; i < utilities.size(); i++) {
	// probas.set(i, Math.exp(utilities.get(i) - maxUtility));
	// }
	// probas.makeProbability();
	// final int chosenIndex = MathHelpers.draw(probas,
	// MatsimRandom.getRandom());
	//
	// return planAlternatives.get(chosenIndex);
	// }
}
