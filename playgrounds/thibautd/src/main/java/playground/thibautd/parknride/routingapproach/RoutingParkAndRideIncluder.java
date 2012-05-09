/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingParkAndRideIncluder.java
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
package playground.thibautd.parknride.routingapproach;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.parknride.ParkAndRideConstants;
import playground.thibautd.parknride.ParkAndRideFacilities;
import playground.thibautd.parknride.ParkAndRideFacility;
import playground.thibautd.parknride.ParkAndRideIncluder;
import playground.thibautd.router.ActivityWrapperFacility;
import playground.thibautd.router.TripRouter;

/**
 * Convenience class which routes access and egress park and ride trips,
 * taking care of the uniqueness of the park and ride facility.
 *
 * @author thibautd
 */
public class RoutingParkAndRideIncluder implements ParkAndRideIncluder {
	private final ParkAndRideFacilities facilities;
	private final ParkAndRideRoutingModule pnrRoutingModule;
	private final TripRouter tripRouter;

	public RoutingParkAndRideIncluder(
			final ParkAndRideFacilities facilities,
			final ParkAndRideRoutingModule pnrRoutingModule,
			final TripRouter tripRouter) {
		this.facilities = facilities;
		this.pnrRoutingModule = pnrRoutingModule;
		this.tripRouter = tripRouter;
	}

	@Override
	public boolean routeAndIncludePnrTrips(
			final Activity accessOriginActivity,
			final Activity accessDestinationActivity,
			final Activity egressOriginActivity,
			final Activity egressDestinationActivity,
			final Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();

		List<? extends PlanElement> pnrAccess =
			pnrRoutingModule.calcRoute(
					new ActivityWrapperFacility( accessOriginActivity ),
					new ActivityWrapperFacility( accessDestinationActivity ),
					getEndTime( accessOriginActivity , planElements ),
					plan.getPerson());

		if (pnrAccess == null) {
			return false;
		}

		ParkAndRideFacility choosenFacility = identifyPnrFacility( pnrAccess , facilities );

		TripRouter.insertTrip(
				planElements,
				accessOriginActivity,
				pnrAccess,
				accessDestinationActivity);

		List<? extends PlanElement> returnTrip =
			routeReturnTrip(
					plan.getPerson(),
					planElements,
					egressOriginActivity,
					egressDestinationActivity,
					choosenFacility);

		TripRouter.insertTrip(
				planElements,
				egressOriginActivity,
				returnTrip,
				egressDestinationActivity);

		return true;
	}

	private List<? extends PlanElement> routeReturnTrip(
			final Person person,
			final List<PlanElement> plan,
			final Activity egressOriginActivity,
			final Activity egressDestinationActivity,
			final ParkAndRideFacility facility) {
		double carDeparture = getEndTime( egressOriginActivity , plan );

		if (facility != null) {
			List<PlanElement> trip = new ArrayList<PlanElement>();

			trip.addAll(
					tripRouter.calcRoute(
						TransportMode.pt,
						new ActivityWrapperFacility( egressOriginActivity ),
						facility,
						carDeparture,
						person) );

			ActivityImpl act = new ActivityImpl(
						ParkAndRideConstants.PARKING_ACT,
						facility.getCoord(),
						facility.getLinkId());
			act.setMaximumDuration( 0d );

			// XXX: dangerous! Not a facility from ActivityFacilities!
			act.setFacilityId( facility.getId() );

			trip.add( act );

			double ptDeparture = carDeparture + ((Leg) trip.get( 0 )).getTravelTime();
			trip.addAll(
					tripRouter.calcRoute(
						TransportMode.car,
						facility,
						new ActivityWrapperFacility( egressDestinationActivity ),
						ptDeparture,
						person) );

			return trip;
		}
		else {
			return tripRouter.calcRoute(
						TransportMode.pt,
						new ActivityWrapperFacility( egressOriginActivity ),
						new ActivityWrapperFacility( egressDestinationActivity ),
						carDeparture,
						person);
		}
	}

	public static ParkAndRideFacility identifyPnrFacility(
			final List<? extends PlanElement> pnrAccess,
			final ParkAndRideFacilities facilities) {
		if (pnrAccess.size() < 2) {
			return null;
		}

		Activity pnrAct =  (Activity) pnrAccess.get( 1 );

		if (!pnrAct.getType().equals( ParkAndRideConstants.PARKING_ACT )) {
			// throw new RuntimeException( "unexpected activity type "+pnrAct.getType()+" in "+pnrAccess );
			// this must be a "pure" PT trip
			return null;
		}

		Id facility = pnrAct.getFacilityId();

		return facilities.getFacilities().get( facility );
	}

	private static double getEndTime(
			final Activity act,
			final List<PlanElement> plan) {
		double endTime = act.getEndTime();

		if (endTime == Time.UNDEFINED_TIME) {
			double startTime = act.getStartTime();

			if (startTime == Time.UNDEFINED_TIME) {
				for (PlanElement pe : plan) {
					if (pe instanceof Activity) {
						Activity currentAct = (Activity) pe;
						double currentEnd = currentAct.getEndTime();
						double currentStart = currentAct.getStartTime();
						double dur = (currentAct instanceof ActivityImpl ? ((ActivityImpl) currentAct).getMaximumDuration() : Time.UNDEFINED_TIME);
						if (currentEnd != Time.UNDEFINED_TIME) {
							// use fromcurrentAct.currentEnd as time for routing
							startTime = currentEnd;
						}
						else if ((currentStart != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
							// use fromcurrentAct.currentStart + fromcurrentAct.duration as time for routing
							startTime = currentStart + dur;
						}
						else if (dur != Time.UNDEFINED_TIME) {
							// use last used time + fromcurrentAct.duration as time for routing
							startTime += dur;
						}
						else {
							throw new RuntimeException("currentActivity has neither end-time nor duration:"+currentAct+" in "+plan);
						}
					}
					else {
						startTime += ((Leg) pe).getTravelTime();
					}

					if (pe == act) break;
				}
			}

			endTime = startTime + act.getMaximumDuration();
		}

		return endTime;
	}

	private static class OriginDestinations {
		public final Activity originFirstTrip;
		public final Activity destinationFirstTrip;
		public final Activity originReturnTrip;
		public final Activity destinationReturnTrip;

		public OriginDestinations(
				final Activity originFirstTrip,
				final Activity destinationFirstTrip,
				final Activity originReturnTrip,
				final Activity destinationReturnTrip) {
			this.originFirstTrip = originFirstTrip;
			this.destinationFirstTrip = destinationFirstTrip;
			this.originReturnTrip = originReturnTrip;
			this.destinationReturnTrip = destinationReturnTrip;
		}
	}
}

