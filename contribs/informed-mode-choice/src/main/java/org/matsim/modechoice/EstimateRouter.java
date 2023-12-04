package org.matsim.modechoice;

import com.google.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Router responsible for routing all possible modes between trips.
 */
public final class EstimateRouter {

	private final TripRouter tripRouter;
	private final ActivityFacilities facilities;
	private final TimeInterpretation timeInterpretation;

	@Inject
	public EstimateRouter(TripRouter tripRouter, ActivityFacilities facilities,
	                      TimeInterpretation timeInterpretation) {
		this.tripRouter = tripRouter;
		this.facilities = facilities;
		this.timeInterpretation = timeInterpretation;
	}

	/**
	 * Route all modes that are relevant with all possible options.
	 */
	public void routeModes(PlanModel model, Collection<String> modes) {

		double[] startTimes = model.getStartTimes();

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		calcStartTimes(model, timeTracker, startTimes, model.trips());

		for (String mode : modes) {

			List<Leg>[] legs = new List[model.trips()];

			int i = 0;

			for (TripStructureUtils.Trip oldTrip : model) {

				final String routingMode = TripStructureUtils.identifyMainMode(oldTrip.getTripElements());

				timeTracker.setTime(startTimes[i]);

				// Ignored mode
				if (!modes.contains(routingMode)) {
					legs[i++] = null;
					continue;
				}

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

				// Use the end-time of an activity or the time tracker if not available
				final List<? extends PlanElement> newTrip = tripRouter.calcRoute(
						mode, from, to,
						oldTrip.getOriginActivity().getEndTime().orElse(timeTracker.getTime().seconds()),
						model.getPerson(),
						oldTrip.getTripAttributes()
				);

				timeTracker.addElements(newTrip);

				// store and increment
				List<Leg> ll = newTrip.stream()
						.filter(el -> el instanceof Leg)
						.map(el -> (Leg) el)
						.collect(Collectors.toList());

				// The PT router can return walk only trips that don't actually use pt
				// this one special case is handled here, it is unclear if similar behaviour might be present in other modes
				if (mode.equals(TransportMode.pt) && ll.stream().noneMatch(l -> l.getMode().equals(TransportMode.pt))) {
					legs[i++] = null;
					continue;
				}

				// TODO: might consider access agress walk modes

				// Filters all kind of modes that did return only walk legs when they could not be used (e.g. drt)
				if (!mode.equals(TransportMode.walk) && ll.stream().allMatch(l -> l.getMode().equals(TransportMode.walk))) {
					legs[i++] = null;
					continue;
				}

				legs[i++] = ll;

			}

			model.setLegs(mode, legs);
		}

		model.setFullyRouted(true);
	}

	/**
	 * Route a single trip with certain index.
	 */
	public void routeSingleTrip(PlanModel model, Collection<String> modes, int idx) {

		double[] startTimes = model.getStartTimes();
		TimeTracker timeTracker = new TimeTracker(timeInterpretation);

		// Plus one so that start time idx is calculated
		calcStartTimes(model, timeTracker, startTimes, idx + 1);

		TripStructureUtils.Trip oldTrip = model.getTrip(idx);

		Facility from = FacilitiesUtils.toFacility(oldTrip.getOriginActivity(), facilities);
		Facility to = FacilitiesUtils.toFacility(oldTrip.getDestinationActivity(), facilities);

		for (String mode : modes) {

			timeTracker.setTime(startTimes[idx]);

			final List<? extends PlanElement> newTrip = tripRouter.calcRoute(
					mode, from, to,
					oldTrip.getOriginActivity().getEndTime().orElse(timeTracker.getTime().seconds()),
					model.getPerson(),
					oldTrip.getTripAttributes()
			);

			List<Leg> ll = newTrip.stream()
					.filter(el -> el instanceof Leg)
					.map(el -> (Leg) el)
					.collect(Collectors.toList());

			// not a real pt trip, see reasoning above
			if (mode.equals(TransportMode.pt) && ll.stream().noneMatch(l -> l.getMode().equals(TransportMode.pt))) {
				model.setLegs(mode, new List[model.trips()]);
				continue;
			}

			List<Leg>[] legs = new List[model.trips()];
			legs[idx] = ll;

			model.setLegs(mode, legs);
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

		// If no time is known the previous trips need to be routed
		if (undefined) {
			String routingMode = TripStructureUtils.getRoutingMode(oldLegs.get(0));
			List<? extends PlanElement> legs = routeTrip(oldTrip, plan, routingMode != null ? routingMode : oldLegs.get(0).getMode(), timeTracker);
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
