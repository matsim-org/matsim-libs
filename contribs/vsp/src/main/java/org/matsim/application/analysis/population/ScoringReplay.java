package org.matsim.application.analysis.population;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import java.util.List;

/** A person's day, handed to a new scoring function of the run the way the run's scoring does. */
final class ScoringReplay {

	private ScoringReplay() {
	}

	/**
	 * Main activities, legs and trips in plan order, and finish() once. Stage activities are left out: they are
	 * not scored, and a plan read from file has no times for them. Money and score events are not replayed.
	 */
	static double score(ScoringFunctionFactory factory, Person person, List<TripStructureUtils.Trip> trips) {
		ScoringFunction scorer = factory.createNewScoringFunction(person);
		for (var trip : trips) {
			scorer.handleActivity(trip.getOriginActivity());
			trip.getLegsOnly().forEach(scorer::handleLeg);
			scorer.handleTrip(trip);
		}
		scorer.handleActivity(trips.getLast().getDestinationActivity());
		scorer.finish();
		return scorer.getScore();
	}
}
