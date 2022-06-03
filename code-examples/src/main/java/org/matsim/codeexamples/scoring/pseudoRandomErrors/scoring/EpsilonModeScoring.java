package org.matsim.codeexamples.scoring.pseudoRandomErrors.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scoring.SumScoringFunction.TripScoring;

public class EpsilonModeScoring implements TripScoring {
	private final EpsilonProvider epsilonProvider;
	private final Id<Person> personId;

	private double score = 0.0;
	private int tripIndex = 0;

	public EpsilonModeScoring(Id<Person> personId, EpsilonProvider epsilonProvider) {
		this.personId = personId;
		this.epsilonProvider = epsilonProvider;
	}

	public void handleTrip(Trip trip) {
		String mode = TripStructureUtils.identifyMainMode(trip.getTripElements());

		double sample = epsilonProvider.getEpsilon(personId, tripIndex++, mode);

		score += sample;
	}

	public void finish() {
	}

	public double getScore() {
		return score;
	}
}
