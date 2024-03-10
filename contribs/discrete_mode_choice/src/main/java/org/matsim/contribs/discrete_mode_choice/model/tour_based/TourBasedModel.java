package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGenerator;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGeneratorFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

/**
 * A choice model that makes decision on a tour basis. The major difference over
 * the trip-based model is, that it additionally relies on a TourFinder to
 * determine the tours in an agent's plan.
 * 
 * @author sebhoerl
 */
public class TourBasedModel implements DiscreteModeChoiceModel {
	final private static Logger logger = LogManager.getLogger(TourBasedModel.class);

	final private TourFinder tourFinder;
	final private TourFilter tourFilter;
	final private TourEstimator estimator;
	final private ModeAvailability modeAvailability;
	final private TourConstraintFactory constraintFactory;
	final private UtilitySelectorFactory selectorFactory;
	final private ModeChainGeneratorFactory modeChainGeneratorFactory;
	final private FallbackBehaviour fallbackBehaviour;
	final private TimeInterpretation timeInterpretation;

	public TourBasedModel(TourEstimator estimator, ModeAvailability modeAvailability,
			TourConstraintFactory constraintFactory, TourFinder tourFinder, TourFilter tourFilter,
			UtilitySelectorFactory selectorFactory, ModeChainGeneratorFactory modeChainGeneratorFactory,
			FallbackBehaviour fallbackBehaviour, TimeInterpretation timeInterpretation) {
		this.estimator = estimator;
		this.modeAvailability = modeAvailability;
		this.constraintFactory = constraintFactory;
		this.tourFinder = tourFinder;
		this.tourFilter = tourFilter;
		this.selectorFactory = selectorFactory;
		this.modeChainGeneratorFactory = modeChainGeneratorFactory;
		this.fallbackBehaviour = fallbackBehaviour;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public List<TripCandidate> chooseModes(Person person, List<DiscreteModeChoiceTrip> trips, Random random)
			throws NoFeasibleChoiceException {
		List<String> modes = new ArrayList<>(modeAvailability.getAvailableModes(person, trips));
		TourConstraint constraint = constraintFactory.createConstraint(person, trips, modes);

		List<TourCandidate> tourCandidates = new LinkedList<>();
		List<List<String>> tourCandidateModes = new LinkedList<>();

		int tripIndex = 1;
		TimeTracker timeTracker = new TimeTracker(timeInterpretation);

		for (List<DiscreteModeChoiceTrip> tourTrips : tourFinder.findTours(trips)) {
			timeTracker.addActivity(tourTrips.get(0).getOriginActivity());

			// We pass the departure time through the first origin activity
			tourTrips.get(0).setDepartureTime(timeTracker.getTime().seconds());

			TourCandidate finalTourCandidate = null;

			if (tourFilter.filter(person, tourTrips)) {
				ModeChainGenerator generator = modeChainGeneratorFactory.createModeChainGenerator(modes, person,
						tourTrips);
				UtilitySelector selector = selectorFactory.createUtilitySelector();

				while (generator.hasNext()) {
					List<String> tourModes = generator.next();

					if (!constraint.validateBeforeEstimation(tourTrips, tourModes, tourCandidateModes)) {
						continue;
					}

					TourCandidate candidate = estimator.estimateTour(person, tourModes, tourTrips, tourCandidates);

					if (!Double.isFinite(candidate.getUtility())) {
						logger.warn(buildIllegalUtilityMessage(tripIndex, person, candidate));
						continue;
					}

					if (!constraint.validateAfterEstimation(tourTrips, candidate, tourCandidates)) {
						continue;
					}

					selector.addCandidate(candidate);
				}

				Optional<UtilityCandidate> selectedCandidate = selector.select(random);

				if (!selectedCandidate.isPresent()) {
					switch (fallbackBehaviour) {
					case INITIAL_CHOICE:
						logger.warn(
								buildFallbackMessage(tripIndex, person, "Setting tour modes back to initial choice."));
						selectedCandidate = Optional.of(createFallbackCandidate(person, tourTrips, tourCandidates));
						break;
					case IGNORE_AGENT:
						return handleIgnoreAgent(tripIndex, person, tourTrips);
					case EXCEPTION:
						throw new NoFeasibleChoiceException(buildFallbackMessage(tripIndex, person, ""));
					}
				}

				finalTourCandidate = (TourCandidate) selectedCandidate.get();
			} else {
				finalTourCandidate = createFallbackCandidate(person, tourTrips, tourCandidates);
			}

			tourCandidates.add(finalTourCandidate);
			tourCandidateModes.add(
					finalTourCandidate.getTripCandidates().stream().map(c -> c.getMode()).collect(Collectors.toList()));

			tripIndex += tourTrips.size();

			for (int i = 0; i < tourTrips.size(); i++) {
				if (i > 0) { // Our time object is already at the end of the first activity
					timeTracker.addActivity(tourTrips.get(i).getOriginActivity());
				}

				timeTracker.addDuration(finalTourCandidate.getTripCandidates().get(i).getDuration());
			}
		}

		return createTripCandidates(tourCandidates);
	}

	private TourCandidate createFallbackCandidate(Person person, List<DiscreteModeChoiceTrip> tourTrips,
			List<TourCandidate> tourCandidates) {
		List<String> initialModes = tourTrips.stream().map(DiscreteModeChoiceTrip::getInitialMode)
				.collect(Collectors.toList());
		return estimator.estimateTour(person, initialModes, tourTrips, tourCandidates);
	}

	private List<TripCandidate> createTripCandidates(List<TourCandidate> tourCandidates) {
		return tourCandidates.stream().map(TourCandidate::getTripCandidates).flatMap(List::stream)
				.collect(Collectors.toList());
	}

	private List<TripCandidate> handleIgnoreAgent(int tripIndex, Person person, List<DiscreteModeChoiceTrip> trips) {
		List<TourCandidate> tourCandidates = new LinkedList<>();

		for (List<DiscreteModeChoiceTrip> tourTrips : tourFinder.findTours(trips)) {
			List<String> tourModes = tourTrips.stream().map(DiscreteModeChoiceTrip::getInitialMode)
					.collect(Collectors.toList());
			tourCandidates.add(estimator.estimateTour(person, tourModes, tourTrips, tourCandidates));
		}

		logger.warn(buildFallbackMessage(tripIndex, person, "Setting whole plan back to initial modes."));
		return createTripCandidates(tourCandidates);
	}

	private String buildFallbackMessage(int tripIndex, Person person, String appendix) {
		return String.format("No feasible mode choice candidate for tour starting at trip %d of agent %s. %s",
				tripIndex, person.getId().toString(), appendix);
	}

	private String buildIllegalUtilityMessage(int tripIndex, Person person, TourCandidate candidate) {
		TripCandidate trip = candidate.getTripCandidates().get(tripIndex);
		
		return String.format(
				"Received illegal utility for for tour starting at trip %d (%s) of agent %s. Continuing with next candidate.",
				tripIndex, trip.getMode(), person.getId().toString());
	}
}
