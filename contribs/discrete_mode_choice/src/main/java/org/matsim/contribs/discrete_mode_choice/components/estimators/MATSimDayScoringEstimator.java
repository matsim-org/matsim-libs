package org.matsim.contribs.discrete_mode_choice.components.estimators;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.DefaultTourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.timing.TimeInterpretation;

/**
 * This tour estimator tries to resemble the MATSim scoring functions as closely
 * as possible. The utility parameters are taken directly from the config file.
 * 
 * Right now, we do not score activities, but everything for that is prepared.
 * We know, for instance, the travel times of all the trips so we could easily
 * shift activities around and check how much of activity time is lost. The
 * calculation of utility for activities is not trivial though, so it is
 * ommitted for now. Feel free to add this in another DayEstimator.
 * 
 * @author sebhoerl
 */
public class MATSimDayScoringEstimator implements TourEstimator {
	private final TourEstimator delegate;
	private final ScoringParametersForPerson scoringParametersForPerson;

	public MATSimDayScoringEstimator(TripEstimator tripEstimator, ScoringParametersForPerson scoringParametersForPerson,
			TimeInterpretation timeInterpretation) {
		this.delegate = new CumulativeTourEstimator(tripEstimator, timeInterpretation);
		this.scoringParametersForPerson = scoringParametersForPerson;
	}

	@Override
	public TourCandidate estimateTour(Person person, List<String> modes, List<DiscreteModeChoiceTrip> trips,
			List<TourCandidate> previousTours) {
		ScoringParameters parameters = scoringParametersForPerson.getScoringParameters(person);

		// First, calculate utility from trips. They're simply summed up.
		TourCandidate candidate = delegate.estimateTour(person, modes, trips, previousTours);
		double utility = candidate.getUtility();

		// Add daily constants for trips
		Set<String> uniqueModes = new HashSet<>(modes);

		for (String uniqueMode : uniqueModes) {
			ModeUtilityParameters modeParams = parameters.modeParams.get(uniqueMode);
			utility += modeParams.dailyUtilityConstant;
			utility += parameters.marginalUtilityOfMoney * modeParams.dailyMoneyConstant;
		}

		return new DefaultTourCandidate(utility, candidate.getTripCandidates());
	}
}
