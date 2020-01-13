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
package org.matsim.contrib.minibus.performance;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.List;

/**
 * {@link PlanAlgorithm} responsible for routing all trips of a plan.
 * Activity times are not updated, even if the previous trip arrival time
 * is after the activity end time.
 * <br>
 * <b>Duplicated code: Just one change that affects the run method. See comment there.</b>
 *
 * @author aneumann, thibautd
 */
final class PPlanRouter implements PlanAlgorithm, PersonAlgorithm {
	private final TripRouter routingHandler;
	private final ActivityFacilities facilities;

	/**
	 * Initialises an instance.
	 * @param routingHandler the {@link TripRouter} to use to route individual trips
	 * @param facilities the {@link ActivityFacilities} to which activities are refering.
	 * May be <tt>null</tt>: in this case, the router will be given facilities wrapping the
	 * origin and destination activity.
	 */
	public PPlanRouter(
			final TripRouter routingHandler,
			final ActivityFacilities facilities) {
		this.routingHandler = routingHandler;
		this.facilities = facilities;
	}

	/**
	 * Short for initialising without facilities.
	 * @param routingHandler
	 */
	public PPlanRouter(
			final TripRouter routingHandler) {
		this( routingHandler , null );
	}

	/**
	 * Gives access to the {@link TripRouter} used
	 * to compute routes.
	 *
	 * @return the internal TripRouter instance.
	 */
	public TripRouter getTripRouter() {
		return routingHandler;
	}

	@Override
	public void run(final Plan plan) {
		final List<Trip> trips = TripStructureUtils.getTrips( plan , routingHandler.getStageActivityTypes() );

		for (Trip trip : trips) {
			
			
			/** That's the only check that got added.... **/
			if (routingHandler.getMainModeIdentifier().identifyMainMode(trip.getTripElements()).equals(TransportMode.pt)) {
				final List<? extends PlanElement> newTrip =
						routingHandler.calcRoute(
								routingHandler.getMainModeIdentifier().identifyMainMode( trip.getTripElements() ),
								toFacility( trip.getOriginActivity() ),
								toFacility( trip.getDestinationActivity() ),
								calcEndOfActivity( trip.getOriginActivity() , plan ),
								plan.getPerson() );

					TripRouter.insertTrip(
							plan, 
							trip.getOriginActivity(),
							newTrip,
							trip.getDestinationActivity());
			}
			
		}
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
		if ((act.getLinkId() == null || act.getCoord() == null)
				&& facilities != null
				&& !facilities.getFacilities().isEmpty()) {
			// use facilities only if the activity does not provides the required fields.
			return facilities.getFacilities().get( act.getFacilityId() );
		}
		return FacilitiesUtils.toFacility( act, facilities );
	}

	private static double calcEndOfActivity(
			final Activity activity,
			final Plan plan) {
		if (activity.getEndTime() != Time.UNDEFINED_TIME) return activity.getEndTime();

		// no sufficient information in the activity...
		// do it the long way.
		// XXX This is inefficient! Using a cache for each plan may be an option
		// (knowing that plan elements are iterated in proper sequence,
		// no need to re-examine the parts of the plan already known)
		double now = 0;

		for (PlanElement pe : plan.getPlanElements()) {
			now = updateNow( now , pe );
			if (pe == activity) return now;
		}

		throw new RuntimeException( "activity "+activity+" not found in "+plan.getPlanElements() );
	}

	private static double updateNow(
			final double now,
			final PlanElement pe) {
		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			double endTime = act.getEndTime();
			double startTime = act.getStartTime();
			double dur = (act instanceof Activity ? act.getMaximumDuration() : Time.UNDEFINED_TIME);
			if (endTime != Time.UNDEFINED_TIME) {
				// use fromAct.endTime as time for routing
				return endTime;
			}
			else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
				// use fromAct.startTime + fromAct.duration as time for routing
				return startTime + dur;
			}
			else if (dur != Time.UNDEFINED_TIME) {
				// use last used time + fromAct.duration as time for routing
				return now + dur;
			}
			else {
				throw new RuntimeException("activity has neither end-time nor duration." + act);
			}
		}
		double tt = ((Leg) pe).getTravelTime();
		return now + (tt != Time.UNDEFINED_TIME ? tt : 0);
	}	
}

