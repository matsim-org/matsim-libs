package org.matsim.modechoice;

import com.google.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
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
import java.util.Set;
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
	public void routeModes(Plan plan, PlanModel model, Collection<String> modes) {


		for (String mode : modes) {

			TimeTracker timeTracker = new TimeTracker(timeInterpretation);

			List<Leg>[] legs = new List[model.trips()];

			int i = 0;

			for (TripStructureUtils.Trip oldTrip : model) {

				final String routingMode = TripStructureUtils.identifyMainMode(oldTrip.getTripElements());
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
						plan.getPerson(),
						oldTrip.getTripAttributes()
				);

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

}
