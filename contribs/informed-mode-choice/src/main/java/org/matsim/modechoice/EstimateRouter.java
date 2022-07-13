package org.matsim.modechoice;

import com.google.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;
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

		double[] startTimes = new double[model.trips()];

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);

		for (int i = 0; i < model.trips() - 1; i++) {
			TripStructureUtils.Trip oldTrip = model.getTrip(i);

			startTimes[i + 1] = advanceTimetracker(timeTracker, oldTrip, model);
		}

		for (String mode : modes) {

			List<Leg>[] legs = new List[model.trips()];

			int i = 0;

			for (TripStructureUtils.Trip oldTrip : model) {

				final String routingMode = TripStructureUtils.identifyMainMode(oldTrip.getTripElements());

				timeTracker.setTime(startTimes[i]);
				timeTracker.addActivity(oldTrip.getOriginActivity());

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

				legs[i++] = ll;

			}

			model.setLegs(mode, legs);
		}
	}

	/**
	 * Route a single trip with certain index.
	 */
	public void routeSingleTrip(PlanModel model, Collection<String> modes, int idx) {

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);

		for (int i = 0; i < idx; i++) {
			TripStructureUtils.Trip oldTrip = model.getTrip(i);
			advanceTimetracker(timeTracker, oldTrip, model);
		}

		TripStructureUtils.Trip oldTrip = model.getTrip(idx);

		Facility from = FacilitiesUtils.toFacility(oldTrip.getOriginActivity(), facilities);
		Facility to = FacilitiesUtils.toFacility(oldTrip.getDestinationActivity(), facilities);

		// store time before this trip starts
		OptionalTime t = timeTracker.getTime();

		for (String mode : modes) {

			timeTracker.setTime(t.seconds());
			timeTracker.addActivity(oldTrip.getOriginActivity());

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
	 * Use time information of existing routes or compute routes if necessary.
	 */
	private double advanceTimetracker(TimeTracker timeTracker, TripStructureUtils.Trip oldTrip, PlanModel plan) {
		timeTracker.addActivity(oldTrip.getOriginActivity());

		List<Leg> oldLegs = oldTrip.getLegsOnly();
		boolean undefined = oldLegs.stream().anyMatch(l -> timeInterpretation.decideOnLegTravelTime(l).isUndefined());

		// If no time is known the previous trips need to be routed
		if (undefined) {
			String routingMode = TripStructureUtils.getRoutingMode(oldLegs.get(0));
			List<? extends PlanElement> legs = routeTrip(oldTrip, plan, routingMode != null ? routingMode : oldLegs.get(0).getMode(), timeTracker);
			timeTracker.addElements(legs);

		} else
			timeTracker.addElements(oldLegs);

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
