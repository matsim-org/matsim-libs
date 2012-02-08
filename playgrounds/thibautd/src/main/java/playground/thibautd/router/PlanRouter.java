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
package playground.thibautd.router;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * {@link PlanAlgorithm} responsible for routing all trips of a plan.
 * Activity times are not updated, even if the previous trip arrival time
 * is after the activity end time.
 *
 * @author thibautd
 */
public class PlanRouter implements PlanAlgorithm {
	private final TripRouter routingHandler;
	private final ActivityFacilities facilities;

	/**
	 * Initialises an instance.
	 * @param routingHandler the {@link TripRouter} to use to route individual trips
	 * @param facilities the {@link ActivityFacilities} to which activities are refering.
	 * May be <tt>null</tt>: in this case, the router will be given facilities wrapping the
	 * origin and destination activity.
	 */
	public PlanRouter(
			final TripRouter routingHandler,
			final ActivityFacilities facilities ) {
		this.routingHandler = routingHandler;
		this.facilities = facilities;
	}

	/**
	 * Short for initialising without facilities.
	 * @param routingHandler
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
	public TripRouter getTripRouter() {
		return routingHandler;
	}

	@Override
	public void run(final Plan plan) {
		List<PlanElement> planStructure = routingHandler.tripsToLegs( plan );
		List<PlanElement> newPlanElements = new ArrayList<PlanElement>();
		Person person = plan.getPerson();

		Facility destination = null;
		Iterator<PlanElement> pes = planStructure.iterator();
		Activity currentAct =  (Activity) pes.next();
		Facility origin = toFacility( currentAct );
		Leg trip = null;

		double now = 0;
		newPlanElements.add( currentAct );
		while (pes.hasNext()) {
			// strict act/leg alternance here
			trip = (Leg) pes.next();
			double endTime = currentAct.getEndTime();
			double startTime = currentAct.getStartTime();
			double dur = (currentAct instanceof ActivityImpl ? ((ActivityImpl) currentAct).getMaximumDuration() : Time.UNDEFINED_TIME);
			if (endTime != Time.UNDEFINED_TIME) {
				// use fromAct.endTime as time for routing
				now = endTime;
			}
			else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
				// use fromAct.startTime + fromAct.duration as time for routing
				now = startTime + dur;
			}
			else if (dur != Time.UNDEFINED_TIME) {
				// use last used time + fromAct.duration as time for routing
				now += dur;
			}
			else {
				throw new RuntimeException("activity of plan of person " + plan.getPerson().getId() + " has neither end-time nor duration." + currentAct);
			}

			currentAct = (Activity) pes.next();
			destination = toFacility( currentAct );

			newPlanElements.addAll(
					routingHandler.calcRoute(
						trip.getMode(),
						origin,
						destination,
						now,
						person));

			newPlanElements.add( currentAct );
			origin = destination;
		}

		updatePlanElements( plan , newPlanElements );
	}

	private Facility toFacility(final Activity act) {
		if (facilities != null) {
			return facilities.getFacilities().get( act.getFacilityId() );
		}
		else {
			return new ActivityWrapperFacility( act );
		}
	}

	private static void updatePlanElements(
			final Plan plan,
			final List<PlanElement> newPlanElements) {
		List<PlanElement> pes = plan.getPlanElements();
		pes.clear();
		pes.addAll( newPlanElements );
	}

	private static class ActivityWrapperFacility implements Facility {
		private final Activity act;

		public ActivityWrapperFacility(final Activity act) {
			this.act = act;
		}

		@Override
		public Coord getCoord() {
			return act.getCoord();
		}

		@Override
		public Id getId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return null;
		}

		@Override
		public Id getLinkId() {
			return act.getLinkId();
		}
	}
}

