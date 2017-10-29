/**
 * 
 */
package org.matsim.withinday.utils;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.Facility;

/**
 * @author kainagel
 *
 */
public class EditTrips {

	private final TripRouter tripRouter;

	public EditTrips( TripRouter tripRouter ) {
		this.tripRouter = tripRouter;
	}

	public final boolean replanFutureTrip(Trip trip, Plan plan, String mainMode, double departureTime) {
		Person person = plan.getPerson();

		Facility<?> fromFacility = new ActivityWrapperFacility( trip.getOriginActivity() ) ;
		Facility<?> toFacility = new ActivityWrapperFacility( trip.getDestinationActivity() ) ;

		final List<? extends PlanElement> newTrip = tripRouter.calcRoute(mainMode, fromFacility, toFacility, departureTime, person);

		TripRouter.insertTrip(plan, trip.getOriginActivity(), newTrip, trip.getDestinationActivity());

		return true;
	}

	/** Convenience method, to be consistent with earlier syntax.  kai, may'16
	 * @param trip
	 * @param plan
	 * @param mainMode
	 * @param departureTime
	 * @param network
	 * @param tripRouter
	 */
	public boolean relocateFutureTrip(Trip trip, Plan plan, String mainMode, double departureTime ) {
		return replanFutureTrip(trip, plan, mainMode, departureTime);
	}

}
