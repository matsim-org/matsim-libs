package org.matsim.contribs.discrete_mode_choice.replanning;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.NoFeasibleChoiceException;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripRouter;

/**
 * This replanning algorithm uses a predefined discrete mode choice model to
 * perform mode decisions for a given plan.
 *
 * @author sebhoerl
 */
public class DiscreteModeChoiceAlgorithm implements PlanAlgorithm {
	private final Random random;
	private final DiscreteModeChoiceModel modeChoiceModel;
	private final TripListConverter tripListConverter;

	private final PopulationFactory populationFactory;

	public DiscreteModeChoiceAlgorithm(Random random, DiscreteModeChoiceModel modeChoiceModel,
			PopulationFactory populationFactory, TripListConverter tripListConverter) {
		this.random = random;
		this.modeChoiceModel = modeChoiceModel;
		this.populationFactory = populationFactory;
		this.tripListConverter = tripListConverter;
	}

	@Override
	/**
	 * Performs mode choice on a plan. We assume that TripsToLegs has been called
	 * before, hence the code is working diretly on legs.
	 */
	public void run(Plan plan) {
		// I) First build a list of DiscreteModeChoiceTrips
		List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(plan);

		// II) Run mode choice

		try {
			// Perform mode choice and retrieve candidates
			List<TripCandidate> chosenCandidates = modeChoiceModel.chooseModes(plan.getPerson(), trips, random);

			for (int i = 0; i < trips.size(); i++) {
				DiscreteModeChoiceTrip trip = trips.get(i);
				TripCandidate candidate = chosenCandidates.get(i);

				List<? extends PlanElement> insertElements;

				if (candidate instanceof RoutedTripCandidate) {
					RoutedTripCandidate routedCandidate = (RoutedTripCandidate) candidate;
					insertElements = routedCandidate.getRoutedPlanElements();
				} else {
					Leg insertLeg = populationFactory.createLeg(candidate.getMode());
					insertElements = Collections.singletonList(insertLeg);
				}

				TripRouter.insertTrip(plan, trip.getOriginActivity(), insertElements, trip.getDestinationActivity());
			}
			plan.getAttributes().putAttribute("utility", chosenCandidates.stream().mapToDouble(UtilityCandidate::getUtility).sum());
		} catch (NoFeasibleChoiceException e) {
			throw new IllegalStateException(e);
		}
	}
}
