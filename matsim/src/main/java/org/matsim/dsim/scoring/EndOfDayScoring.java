package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.NewScoreAssigner;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.dsim.simulation.IterationInformation;

public class EndOfDayScoring {

	private final ScoringFunctionFactory scoringFunctionFactory;
	private final Population population;
	private final NewScoreAssigner newScoreAssigner;
	private final IterationInformation iterationInformation;

	@Inject
	public EndOfDayScoring(Population population, ScoringFunctionFactory scoringFunctionFactory, NewScoreAssigner newScoreAssigner, IterationInformation iterationInformation) {
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.population = population;
		this.newScoreAssigner = newScoreAssigner;
		this.iterationInformation = iterationInformation;
	}

	public void score(BackPack backPack) {

		var person = population.getPersons().get(backPack.personId());

		if (person == null) {
			return;
		}

		var scoringFunction = scoringFunctionFactory.createNewScoringFunction(person);

		// replay events relevant for scoring function first
		for (var e : backPack.specialScoringEvents()) {
			if (e instanceof PersonMoneyEvent pme) {
				scoringFunction.addMoney(pme.getAmount());
			} else if (e instanceof PersonScoreEvent pse) {
				scoringFunction.addScore(pse.getAmount());
			} else if (e instanceof PersonStuckEvent pse) {
				scoringFunction.agentStuck(pse.getTime());
			}
		}

		// We pass activities and legs as well as trips. The scoring function can decide whether to use it.
		var experiencedPlan = backPack.backpackPlan().experiencedPlan();
		var trips = TripStructureUtils.getTrips(experiencedPlan);
		for (var trip : trips) {
			// pass all elements except the last activity, as it will be included in the next trip as well
			scoringFunction.handleActivity(trip.getOriginActivity());
			for (var e : trip.getTripElements()) {
				if (e instanceof Activity a) {
					scoringFunction.handleActivity(a);
				} else if (e instanceof Leg l) {
					scoringFunction.handleLeg(l);
				}
			}
			scoringFunction.handleTrip(trip);
		}
		// pass the last activity of the day to the scoring function, as we have excluded it above
		if (!experiencedPlan.getPlanElements().isEmpty() && experiencedPlan.getPlanElements().getLast() instanceof Activity a) {
			scoringFunction.handleActivity(a);
		}

		scoringFunction.finish();

		// directly assign score to person, as this person will not be moved anymore.
		// we can do it this way, without synchronization, as an agent should only be
		// present on one partition at a time. This makes the person data structures
		// contained in the population independent of each other.
		newScoreAssigner.assignNewScore(iterationInformation.iteration(), scoringFunction, person);
	}
}
