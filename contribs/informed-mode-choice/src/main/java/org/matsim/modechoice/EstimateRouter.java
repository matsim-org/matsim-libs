package org.matsim.modechoice;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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
	public PlanModel routeModes(Plan plan, Set<String> modes) {

		List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);

		PlanModel model = new PlanModel(trips);

		for (String mode : modes) {

			TimeTracker timeTracker = new TimeTracker(timeInterpretation);

			for (TripStructureUtils.Trip oldTrip : trips) {

				// TODO: routing mode for new routes

				final String routingMode = TripStructureUtils.identifyMainMode(oldTrip.getTripElements());

				// Ignored
				if (!modes.contains(routingMode))
					continue;

				timeTracker.addActivity(oldTrip.getOriginActivity());

				final List<? extends PlanElement> newTrip = tripRouter.calcRoute(
						routingMode,
						FacilitiesUtils.toFacility(oldTrip.getOriginActivity(), facilities),
						FacilitiesUtils.toFacility(oldTrip.getDestinationActivity(), facilities),
						timeTracker.getTime().seconds(),
						plan.getPerson(),
						oldTrip.getTripAttributes()
				);

				// TODO: store legs
			}
		}

		// TODO: input: context, collected mode options, plan
		// only mode options that allow to use the mode at all


		// TODO: output
		// for each mode: (options is not relevant)
		// array of the travel times

		// everything may go into the plan model ?
		// -> alternatives

		// also collect the trips here for re-use


		return model;
	}

}
