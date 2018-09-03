/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.core.router;

import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

/**
 * {@link PlanAlgorithm} responsible for routing all trips of a plan.
 * Activity times are not updated, even if the previous trip arrival time
 * is after the activity end time.
 *
 * @author thibautd
 */
public class PlanRouter implements PlanAlgorithm, PersonAlgorithm {
	private final TripRouter tripRouter;
	private final ActivityFacilities facilities;

	/**
	 * Initialises an instance.
	 * @param tripRouter the {@link TripRouter} to use to route individual trips
	 * @param facilities the {@link ActivityFacilities} to which activities are refering.
	 * May be <tt>null</tt>: in this case, the router will be given facilities wrapping the
	 * origin and destination activity.
	 */
	public PlanRouter(
			final TripRouter tripRouter,
			final ActivityFacilities facilities) {
		this.tripRouter = tripRouter;
		this.facilities = facilities;
	}

	/**
	 * Short for initialising without facilities.
	 */
	public PlanRouter(
			final TripRouter routingHandler) {
		this( routingHandler , null );
	}

	/**
	 * Gives access to the {@link TripRouter} used
	 * to compute routes.
	 *
	 * @return the internal TripRouter instance.
	 */
	@Deprecated // get TripRouter out of injection instead. kai, feb'16
	public TripRouter getTripRouter() {
		return tripRouter;
	}

	@Override
	public void run(final Plan plan) {
		final List<Trip> trips = TripStructureUtils.getTrips( plan , tripRouter.getStageActivityTypes() );

		for (Trip oldTrip : trips) {
			final List<? extends PlanElement> newTrip =
					tripRouter.calcRoute(
							tripRouter.getMainModeIdentifier().identifyMainMode( oldTrip.getTripElements() ),
							toFacility( oldTrip.getOriginActivity() ),
							toFacility( oldTrip.getDestinationActivity() ),
							calcEndOfActivity( oldTrip.getOriginActivity() , plan, tripRouter.getConfig() ),
							plan.getPerson() );
			putVehicleFromOldTripIntoNewTripIfMeaningful(oldTrip, newTrip);
			putModeFromOldTripIntoNewTripIfMeaningful(oldTrip, newTrip);
			TripRouter.insertTrip(
					plan, 
					oldTrip.getOriginActivity(),
					newTrip,
					oldTrip.getDestinationActivity());
		}
	}

	/**
	 * If the old trip had vehicles set in its network routes, and it used a single vehicle,
	 * and if the new trip does not come with vehicles set in its network routes,
	 * then put the vehicle of the old trip into the network routes of the new trip.
	 * @param oldTrip The old trip
	 * @param newTrip The new trip
	 */
	private static void putVehicleFromOldTripIntoNewTripIfMeaningful(Trip oldTrip, List<? extends PlanElement> newTrip) {
		Id<Vehicle> oldVehicleId = getUniqueVehicleId(oldTrip);
		if (oldVehicleId != null) {
			for (Leg leg : TripStructureUtils.getLegs(newTrip)) {
				if (leg.getRoute() instanceof NetworkRoute) {
					if (((NetworkRoute) leg.getRoute()).getVehicleId() == null) {
						((NetworkRoute) leg.getRoute()).setVehicleId(oldVehicleId);
					}
				}
			}
		}
	}
	
	/**
	 * If the old trip was a public transit trip and the trip mode was set different from "pt" in {@link TransportMode},
	 * then set the mode of the new trip based on the (main-)mode of the old trip.
	 * @param oldTrip The old trip
	 * @param newTrip The new trip
	 */
	private static void putModeFromOldTripIntoNewTripIfMeaningful(Trip oldTrip, List<? extends PlanElement> newTrip) {
		for (Leg leg : TripStructureUtils.getLegs(newTrip)) {
			if (leg.getMode().equals(TransportMode.pt)) {
				if (oldTrip.getLegsOnly().get(1) == null) {
					throw new RuntimeException("The old trip consists of only one leg. Old trip: " + oldTrip.toString() + " /// New trip: " + newTrip.toString());
				}
				leg.setMode(oldTrip.getLegsOnly().get(1).getMode());
				// FIXME: Replace hard-coded 1 by something better. kai+ihab Sep'18
			}
		}
	}

	private static Id<Vehicle> getUniqueVehicleId(Trip trip) {
		Id<Vehicle> vehicleId = null;
		for (Leg leg : trip.getLegsOnly()) {
			if (leg.getRoute() instanceof NetworkRoute) {
				if (vehicleId != null && (!vehicleId.equals(((NetworkRoute) leg.getRoute()).getVehicleId()))) {
					return null; // The trip uses several vehicles.
				}
				vehicleId = ((NetworkRoute) leg.getRoute()).getVehicleId();
			}
		}
		return vehicleId;
	}

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			run( plan );
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private Facility toFacility(final Activity act) {
//		if (  (act.getLinkId() == null && act.getCoord() == null)  // yyyy this used to be || instead of && --???  kai, jun'16
//				&& facilities != null
//				&& !facilities.getFacilities().isEmpty()) {
//			// use facilities only if the activity does not provide the required fields.
//			// yyyyyy Seems to me that the Access/EgressRoutingModule only needs either link or coord to start from.  So we only go
//			// to facilities if neither is provided.  --  This may, however, be at odds of how it is done in the AccessEgressRoutingModule, so we
//			// need to conceptually sort this out!!  kai, jun'16
//			return facilities.getFacilities().get( act.getFacilityId() );
//		}

		// use facility first if available i.e. reversing the logic above Amit July'18
		if (	facilities != null &&
				! facilities.getFacilities().isEmpty() &&
				act.getFacilityId() != null ) {
			return facilities.getFacilities().get( act.getFacilityId() );
		}

		return new ActivityWrapperFacility( act );
	}

	public static double calcEndOfActivity(
			final Activity activity,
			final Plan plan,
			final Config config ) {
		
		if (activity.getEndTime() != Time.UNDEFINED_TIME) return activity.getEndTime();

		// no sufficient information in the activity...
		// do it the long way.
		// XXX This is inefficient! Using a cache for each plan may be an option
		// (knowing that plan elements are iterated in proper sequence,
		// no need to re-examine the parts of the plan already known)
		double now = 0;

		for (PlanElement pe : plan.getPlanElements()) {
			now = updateNow( now , pe, config );
			if (pe == activity) return now;
		}

		throw new RuntimeException( "activity "+activity+" not found in "+plan.getPlanElements() );
	}

	private static double updateNow(
			final double now,
			final PlanElement pe,
			final Config config ) {
		// yyyy see similar method in TripRouter. kai, oct'17
		if (pe instanceof Activity) {
			// yyyyyy this should use PopulationUtils.getActivityEndTime(...) to be consistent with other code.  kai, oct'17
			Activity act = (Activity) pe;
			return PopulationUtils.getActivityEndTime(act, now, config ) ;
			
//			double endTime = act.getEndTime();
//			double startTime = act.getStartTime();
//			double dur = (act instanceof Activity ? act.getMaximumDuration() : Time.UNDEFINED_TIME);
//			if (endTime != Time.UNDEFINED_TIME) {
//				// use fromAct.endTime as time for routing
//				return endTime;
//			}
//			else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
//				// use fromAct.startTime + fromAct.duration as time for routing
//				return startTime + dur;
//			}
//			else if (dur != Time.UNDEFINED_TIME) {
//				// use last used time + fromAct.duration as time for routing
//				return now + dur;
//			}
//			else {
//				throw new RuntimeException("activity has neither end-time nor duration." + act);
//			}
		}
		double tt = ((Leg) pe).getTravelTime();
		return now + (tt != Time.UNDEFINED_TIME ? tt : 0);
	}	
}

