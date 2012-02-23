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

	/**
	 * Extracts the plan structure using {@link TripRouter#tripsToLegs(Plan)},
	 * routes it using {@link #run(Person, List)}, and updates the plan so that
	 * it references the routed sequence.
	 */
	@Override
	public void run(final Plan plan) {
		List<PlanElement> newSequence = routingHandler.tripsToLegs( plan );
		newSequence = run( plan.getPerson() , newSequence );
		updatePlanElements( plan , newSequence );
	}

	/**
	 * The actual processing method of the {@link #run(Plan)} method.
	 *
	 * It routes the trips defined by the legs in the planStructure sequence,
	 * and returns the full routed structure.
	 * @param person the {@link Person} to route
	 * @param planStructure the sequence of plan elements, where the trips are replaced
	 * by legs. This can be obtained by the {@link TripRouter#tripsToLegs(Plan)}
	 * and {@link TripRouter#tripsToLegs(List)} methods.
	 *
	 * @return the routed sequence of plan elements
	 */
	public List<PlanElement> run(
			final Person person,
			final List<PlanElement> planStructure) {
		List<PlanElement> newPlanElements = new ArrayList<PlanElement>();

		Facility destination = null;
		Iterator<PlanElement> pes = planStructure.iterator();
		Activity currentAct =  (Activity) pes.next();
		Facility origin = toFacility( currentAct );
		Leg legTrip = null;
		List<? extends PlanElement> trip = null;

		double now = 0;
		newPlanElements.add( currentAct );
		while (pes.hasNext()) {
			// normally, strict act/leg alternance here (trips replaced by legs).
			// If not, throw an exception with an informative debugging message.
			try {
				legTrip = (Leg) pes.next();
			}
			catch (ClassCastException e) {
				throw new RuntimeException( "agent "+person.getId()+": unexpected leg/act alternance in structure "+planStructure );
			}

			now = updateNow( now , currentAct );
			
			try {
				currentAct = (Activity) pes.next();
			}
			catch (ClassCastException e) {
				throw new RuntimeException( "agent "+person.getId()+": unexpected leg/act alternance in structure "+planStructure );
			}

			destination = toFacility( currentAct );

			try {
				trip = routingHandler.calcRoute(
							legTrip.getMode(),
							origin,
							destination,
							now,
							person);
			}
			catch (TripRouter.UnknownModeException e) {
				throw new RuntimeException( "agent "+person.getId()+": unexpected mode in structure "+planStructure , e );
			}

			newPlanElements.addAll( trip );

			for (PlanElement pe : trip) {
				now = updateNow( now , pe );
			}

			newPlanElements.add( currentAct );
			origin = destination;
		}

		return newPlanElements;
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
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

	private static double updateNow(
			final double now,
			final PlanElement pe) {
		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			double endTime = act.getEndTime();
			double startTime = act.getStartTime();
			double dur = (act instanceof ActivityImpl ? ((ActivityImpl) act).getMaximumDuration() : Time.UNDEFINED_TIME);
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
		else {
			return now + ((Leg) pe).getTravelTime();
		}
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

