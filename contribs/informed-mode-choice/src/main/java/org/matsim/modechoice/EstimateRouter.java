package org.matsim.modechoice;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Router responsible for routing all possible modes between trips.
 */
public final class EstimateRouter {

	/**
	 * Initial value for unrouted trips.
	 */
	static final List<Leg> UN_ROUTED = List.of();

	private final TripRouter tripRouter;
	private final ActivityFacilities facilities;
	private final AnalysisMainModeIdentifier mmi;
	private final TimeInterpretation timeInterpretation;

	@Inject
	public EstimateRouter(TripRouter tripRouter, ActivityFacilities facilities,
						  AnalysisMainModeIdentifier mmi) {
		this.tripRouter = tripRouter;
		this.facilities = facilities;
		this.mmi = mmi;
		// ignore the travel times of individual legs
		this.timeInterpretation = TimeInterpretation.create(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration,
			PlansConfigGroup.TripDurationHandling.ignoreDelays);
	}

	/**
	 * Route all modes that are relevant with all possible options.
	 */
	public void routeModes(PlanModel model, Collection<String> modes, TripModeFilter filter) {

		double[] startTimes = model.getStartTimes();

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		calcStartTimes(model, timeTracker, startTimes, model.trips());

		String[] currentMode = model.getCurrentModesMutable();

		List<String> considerModes = Stream.concat(Arrays.stream(currentMode), modes.stream())
			.filter(Objects::nonNull)
			.distinct()
			.toList();


		for (String mode : considerModes) {

			// Null may be put into the collection, which will just be ignored here
			if (mode == null)
				continue;

			// Legs will be updated in-place
			List<Leg>[] legs = model.getLegs(mode, UN_ROUTED);

			int i = 0;
			for (TripStructureUtils.Trip oldTrip : model) {

				// The current modes are always routed, regardless of filter.
				// they are always needed to score whole plan
				if (!filter.accept(mode, i) && !mode.equals(currentMode[i])) {
					i++;
					continue;
				}

				// Skip entries that are already routed
				if (legs[i] != UN_ROUTED) {
					i++;
					continue;
				}

				timeTracker.setTime(startTimes[i]);

				Facility from = FacilitiesUtils.toFacility(oldTrip.getOriginActivity(), facilities);
				Facility to = FacilitiesUtils.toFacility(oldTrip.getDestinationActivity(), facilities);

				// don't route if same location
				/*
				if ((from.getLinkId() != null && from.getLinkId() == to.getLinkId()) ||
						(from.getCoord() != null && from.getCoord().equals(to.getCoord()))) {
					legs[i++] = null;
					continue;
				}
				 */

				final List<? extends PlanElement> newTrip = tripRouter.calcRoute(
						mode, from, to,
						oldTrip.getOriginActivity().getEndTime().orElse(timeTracker.getTime().seconds()),
						model.getPerson(),
						oldTrip.getTripAttributes()
				);

				// update time tracker, however it will be updated before each iteration
				timeTracker.addElements(newTrip);

				// store and increment
				List<Leg> ll = newTrip.stream()
						.filter(el -> el instanceof Leg)
						.map(el -> (Leg) el)
						.collect(Collectors.toList());

				legs[i++] = ll;

			}
		}
	}

	/**
	 * Calculate the starting times for each trip.
	 */
	private void calcStartTimes(PlanModel model, TimeTracker timeTracker, double[] startTimes, int until) {

		if (model.trips() == 0)
			return;

		timeTracker.addActivity(model.getTrip(0).getOriginActivity());
		startTimes[0] = timeTracker.getTime().seconds();

		for (int i = 0; i < until - 1; i++) {
			TripStructureUtils.Trip oldTrip = model.getTrip(i);
			startTimes[i + 1] = advanceTimetracker(timeTracker, oldTrip, model);
		}
	}

	/**
	 * Use time information of existing routes or compute routes if necessary.
	 */
	private double advanceTimetracker(TimeTracker timeTracker, TripStructureUtils.Trip oldTrip, PlanModel plan) {
		List<Leg> oldLegs = oldTrip.getLegsOnly();
		boolean undefined = oldLegs.stream().anyMatch(l -> timeInterpretation.decideOnLegTravelTime(l).isUndefined());

		// If no time is known the previous trips needs to be routed
		if (undefined) {
			String routingMode = TripStructureUtils.getRoutingMode(oldLegs.getFirst());
			if (routingMode == null)
				routingMode = mmi.identifyMainMode(oldLegs);

			List<? extends PlanElement> legs = routeTrip(oldTrip, plan, routingMode, timeTracker);
			timeTracker.addElements(legs);

		} else
			timeTracker.addElements(oldLegs);

		timeTracker.addActivity(oldTrip.getDestinationActivity());

		return timeTracker.getTime().seconds();
	}

	private List<? extends PlanElement> routeTrip(TripStructureUtils.Trip trip, PlanModel plan, String routingMode, TimeTracker timeTracker) {
		return tripRouter.calcRoute( //
				routingMode, //
				FacilitiesUtils.toFacility(trip.getOriginActivity(), facilities), //
				FacilitiesUtils.toFacility(trip.getDestinationActivity(), facilities), //
				timeTracker.getTime().seconds(), //
				plan.getPerson(), //
				trip.getTripAttributes() //
		);
	}

}
