package org.matsim.contrib.discrete_mode_choice.replanning;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.discrete_mode_choice.test_utils.PlanBuilder;
import org.matsim.contribs.discrete_mode_choice.components.estimators.CumulativeTourEstimator;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.FallbackBehaviour;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.NoFeasibleChoiceException;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.CompositeTourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.constraints.CompositeTripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.filters.CompositeTourFilter;
import org.matsim.contribs.discrete_mode_choice.model.filters.CompositeTripFilter;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.DefaultModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.DefaultModeChainGenerator;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGeneratorFactory;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourBasedModel;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourEstimator;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourFilter;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TripFilter;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripBasedModel;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.RandomSelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.PlansConfigGroup.TripDurationHandling;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.timing.TimeInterpretation;

public class TestDepartureTimes {
	@Test
	void testTripBasedModelDepartureTimes() throws NoFeasibleChoiceException {
		TripBasedModel model = createTripBasedModel();

		// Case 1: Only activity end times, and trips fall well between the end times

		Plan plan1 = new PlanBuilder() //
				.addActivityWithEndTime("home", 1000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("work", 2000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("shop", 3000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("home", 4000.0) //
				.buildPlan();

		List<Double> departureTimes1 = Arrays.asList(1000.0, 2000.0, 3000.0);
		assertDepartureTimes(model, plan1, departureTimes1);

		// Case 2: One maximum duration, which should add up to the trip travel time

		Plan plan2 = new PlanBuilder() //
				.addActivityWithEndTime("home", 1000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("work", 2000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithDuration("shop", 200.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("home", 4000.0) //
				.buildPlan();

		List<Double> departureTimes2 = Arrays.asList(1000.0, 2000.0, 2700.0);
		assertDepartureTimes(model, plan2, departureTimes2);

		// Case 3: Travel times is exceeding the first end time, so it should be pushed

		Plan plan3 = new PlanBuilder() //
				.addActivityWithEndTime("home", 1000.0) //
				.addLeg("generic", 1200.0) //
				.addActivityWithEndTime("work", 2000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("shop", 3000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("home", 4000.0) //
				.buildPlan();

		List<Double> departureTimes3 = Arrays.asList(1000.0, 2200.0, 3000.0);
		assertDepartureTimes(model, plan3, departureTimes3);

		// Case 4: Travel time is exceeding all end times, so they should be pushed

		Plan plan4 = new PlanBuilder() //
				.addActivityWithEndTime("home", 1000.0) //
				.addLeg("generic", 3200.0) //
				.addActivityWithEndTime("work", 2000.0) //
				.addLeg("generic", 3200.0) //
				.addActivityWithEndTime("shop", 3000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("home", 4000.0) //
				.buildPlan();

		List<Double> departureTimes4 = Arrays.asList(1000.0, 4200.0, 7400.0);
		assertDepartureTimes(model, plan4, departureTimes4);

		// Case 5: Sum up excessive travel time for first trip plus maximum duration
		// afterwards

		Plan plan5 = new PlanBuilder() //
				.addActivityWithEndTime("home", 1000.0) //
				.addLeg("generic", 1500.0) //
				.addActivityWithDuration("work", 50.0) //
				.addLeg("generic", 200.0) //
				.addActivityWithEndTime("shop", 3000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("home", 4000.0) //
				.buildPlan();

		List<Double> departureTimes5 = Arrays.asList(1000.0, 2550.0, 3000.0);
		assertDepartureTimes(model, plan5, departureTimes5);
	}

	@Test
	void testPushDepartureTimeToNextTour() throws NoFeasibleChoiceException {
		TourBasedModel model = createTourBasedModel();

		Plan plan = new PlanBuilder() //
				.addActivityWithEndTime("home", 1000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("work", 2000.0) //
				.addLeg("generic", 1200.0) // This trip is too long, so the time should be pushed to next tour
				.addActivityWithEndTime("home", 3000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("shop", 4000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("home", 5000.0) //
				.buildPlan();

		List<Double> departureTimes = Arrays.asList(1000.0, 2000.0, 3200.0, 4000.0);
		assertDepartureTimes(model, plan, departureTimes);
	}

	@Test
	void testAccumulateAndPushDepartureTimeToNextTour() throws NoFeasibleChoiceException {
		TourBasedModel model = createTourBasedModel();

		Plan plan = new PlanBuilder() //
				.addActivityWithEndTime("home", 1000.0) //
				.addLeg("generic", 1200.0) //
				.addActivityWithEndTime("work", 2000.0) //
				.addLeg("generic", 3200.0) // This trip is too long, so the time should be pushed to next tour
				.addActivityWithEndTime("home", 3000.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithDuration("shop", 50.0) //
				.addLeg("generic", 500.0) //
				.addActivityWithEndTime("home", 5000.0) //
				.buildPlan();

		List<Double> departureTimes = Arrays.asList(1000.0, 2200.0, 5400.0, 5950.0);
		assertDepartureTimes(model, plan, departureTimes);
	}

	static private void assertDepartureTimes(DiscreteModeChoiceModel model, Plan plan, List<Double> referenceTimes)
			throws NoFeasibleChoiceException {
		List<DiscreteModeChoiceTrip> trips = new TripListConverter().convert(plan);
		List<TripCandidate> candidates = model.chooseModes(plan.getPerson(), trips, new Random(0));

		List<Double> departureTimes = new LinkedList<>();

		for (TripCandidate candidate : candidates) {
			Leg leg = (Leg) ((RoutedTripCandidate) candidate).getRoutedPlanElements().get(0);
			departureTimes.add(leg.getDepartureTime().seconds());
		}

		assertEquals(referenceTimes, departureTimes);
	}

	static private TripBasedModel createTripBasedModel() {
		TripEstimator estimator = new TestTripEstimator();
		TripFilter tripFilter = new CompositeTripFilter(Collections.emptySet());
		ModeAvailability modeAvailability = new DefaultModeAvailability(Arrays.asList("car"));
		TripConstraintFactory constraintFactory = new CompositeTripConstraintFactory();
		UtilitySelectorFactory selectorFactory = new RandomSelector.Factory();
		FallbackBehaviour fallbackBehaviour = FallbackBehaviour.EXCEPTION;

		return new TripBasedModel(estimator, tripFilter, modeAvailability, constraintFactory, selectorFactory,
				fallbackBehaviour, TimeInterpretation.create(ActivityDurationInterpretation.tryEndTimeThenDuration,
						TripDurationHandling.shiftActivityEndTimes));
	}

	static private TourBasedModel createTourBasedModel() {
		TimeInterpretation timeInterpretation = TimeInterpretation.create(
				ActivityDurationInterpretation.tryEndTimeThenDuration, TripDurationHandling.shiftActivityEndTimes);

		TourEstimator estimator = new CumulativeTourEstimator(new TestTripEstimator(), timeInterpretation);
		TourFilter tourFilter = new CompositeTourFilter(Collections.emptySet());
		ModeAvailability modeAvailability = new DefaultModeAvailability(Arrays.asList("car"));
		TourConstraintFactory constraintFactory = new CompositeTourConstraintFactory();
		TourFinder tourFinder = new ActivityTourFinder(Arrays.asList("home"));
		UtilitySelectorFactory selectorFactory = new RandomSelector.Factory();
		FallbackBehaviour fallbackBehaviour = FallbackBehaviour.EXCEPTION;
		ModeChainGeneratorFactory modeChainGeneratorFactory = new DefaultModeChainGenerator.Factory();

		return new TourBasedModel(estimator, modeAvailability, constraintFactory, tourFinder, tourFilter,
				selectorFactory, modeChainGeneratorFactory, fallbackBehaviour, timeInterpretation);
	}

	static private class TestTripEstimator implements TripEstimator {
		@Override
		public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
				List<TripCandidate> previousTrips) {
			double travelTime = ((Leg) trip.getInitialElements().get(0)).getTravelTime().seconds();

			Leg leg = PopulationUtils.createLeg(mode);
			leg.setDepartureTime(trip.getDepartureTime());
			leg.setTravelTime(travelTime);

			return new DefaultRoutedTripCandidate(0.0, mode, Arrays.asList(leg), travelTime);
		}
	}
}
