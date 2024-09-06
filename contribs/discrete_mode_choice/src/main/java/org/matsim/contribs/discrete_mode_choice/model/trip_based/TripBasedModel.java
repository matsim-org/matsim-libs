package org.matsim.contribs.discrete_mode_choice.model.trip_based;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TripFilter;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

/**
 * This class defines a trip-based discrete choice model.
 * 
 * @author sebhoerl
 *
 */
public class TripBasedModel implements DiscreteModeChoiceModel {
	private final static Logger logger = LogManager.getLogger(TripBasedModel.class);

	private final TripEstimator estimator;
	private final TripFilter tripFilter;
	private final ModeAvailability modeAvailability;
	private final TripConstraintFactory constraintFactory;
	private final UtilitySelectorFactory selectorFactory;
	private final FallbackBehaviour fallbackBehaviour;
	private final TimeInterpretation timeInterpretation;

	public TripBasedModel(TripEstimator estimator, TripFilter tripFilter, ModeAvailability modeAvailability,
			TripConstraintFactory constraintFactory, UtilitySelectorFactory selectorFactory,
			FallbackBehaviour fallbackBehaviour, TimeInterpretation timeInterpretation) {
		this.estimator = estimator;
		this.tripFilter = tripFilter;
		this.modeAvailability = modeAvailability;
		this.constraintFactory = constraintFactory;
		this.selectorFactory = selectorFactory;
		this.fallbackBehaviour = fallbackBehaviour;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public List<TripCandidate> chooseModes(Person person, List<DiscreteModeChoiceTrip> trips, Random random)
			throws NoFeasibleChoiceException {
		List<String> modes = new ArrayList<>(modeAvailability.getAvailableModes(person, trips));
		TripConstraint constraint = constraintFactory.createConstraint(person, trips, modes);

		List<TripCandidate> tripCandidates = new ArrayList<>(trips.size());
		List<String> tripCandidateModes = new ArrayList<>(trips.size());

		int tripIndex = 0;
		TimeTracker timeTracker = new TimeTracker(timeInterpretation);

		for (DiscreteModeChoiceTrip trip : trips) {
			timeTracker.addActivity(trip.getOriginActivity());
			trip.setDepartureTime(timeTracker.getTime().seconds());

			TripCandidate finalTripCandidate = null;

			if (tripFilter.filter(person, trip)) {
				UtilitySelector selector = selectorFactory.createUtilitySelector();
				tripIndex++;

				for (String mode : modes) {
					if (!constraint.validateBeforeEstimation(trip, mode, tripCandidateModes)) {
						continue;
					}

					TripCandidate candidate = estimator.estimateTrip(person, mode, trip, tripCandidates);

					if (!Double.isFinite(candidate.getUtility())) {
						logger.warn(buildIllegalUtilityMessage(tripIndex, person, candidate));
						continue;
					}

					if (!constraint.validateAfterEstimation(trip, candidate, tripCandidates)) {
						continue;
					}

					selector.addCandidate(candidate);
				}

				Optional<UtilityCandidate> selectedCandidate = selector.select(random);

				if (!selectedCandidate.isPresent()) {
					switch (fallbackBehaviour) {
					case INITIAL_CHOICE:
						logger.info(buildFallbackMessage(tripIndex, person, "Setting trip back to initial mode."));
						selectedCandidate = Optional.of(createFallbackCandidate(person, trip, tripCandidates));
						break;
					case IGNORE_AGENT:
						return handleIgnoreAgent(tripIndex, person, trips);
					case EXCEPTION:
						throw new NoFeasibleChoiceException(buildFallbackMessage(tripIndex, person, ""));
					}
				}

				finalTripCandidate = (TripCandidate) selectedCandidate.get();
			} else {
				finalTripCandidate = createFallbackCandidate(person, trip, tripCandidates);
			}

			tripCandidates.add(finalTripCandidate);
			tripCandidateModes.add(finalTripCandidate.getMode());

			timeTracker.addDuration(finalTripCandidate.getDuration());
		}

		return tripCandidates;
	}

	private TripCandidate createFallbackCandidate(Person person, DiscreteModeChoiceTrip trip,
			List<TripCandidate> tripCandidates) {
		return estimator.estimateTrip(person, trip.getInitialMode(), trip, tripCandidates);
	}

	private List<TripCandidate> handleIgnoreAgent(int tripIndex, Person person, List<DiscreteModeChoiceTrip> trips) {
		List<TripCandidate> candidates = new ArrayList<>(trips.size());

		for (DiscreteModeChoiceTrip trip : trips) {
			candidates.add(estimator.estimateTrip(person, trip.getInitialMode(), trip, candidates));
		}

		logger.warn(buildFallbackMessage(tripIndex, person, "Setting whole plan back to initial modes."));
		return candidates;
	}

	private String buildFallbackMessage(int tripIndex, Person person, String appendix) {
		return String.format("No feasible mode choice candidate for trip %d of agent %s. %s", tripIndex,
				person.getId().toString(), appendix);
	}

	private String buildIllegalUtilityMessage(int tripIndex, Person person, TripCandidate candidate) {
		return String.format("Received illegal utility for trip %d (%s) of agent %s. Continuing with next candidate.",
				tripIndex, candidate.getMode(), person.getId().toString());
	}
}
