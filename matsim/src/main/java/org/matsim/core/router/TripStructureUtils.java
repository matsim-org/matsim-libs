/* *********************************************************************** *
 * project: org.matsim.*
 * TripStructureUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * Helps to work on plans with complew trips.
 * It provides convenience methods to get only the non-dummy activities
 * or the information about trips, in a way hopefully clean and useful.
 * <br>
 * The collections returned by this class are immutable.
 * <br>
 * The methods require an instance of {@link StageActivityTypes} as a parameter,
 * which is used to identify the dummy activities pertaining to trips.
 * In almost all use-cases, it should come from {@link TripRouter#getStageActivityTypes()}.
 *
 * @author thibautd
 */
public class TripStructureUtils {
	private TripStructureUtils() {};

	public static List<Activity> getActivities(
			final Plan plan,
			final StageActivityTypes stageActivities) {
		final List<Activity> activities = new ArrayList<Activity>();
		
		for (PlanElement pe : plan.getPlanElements()) {
			if ( !(pe instanceof Activity) ) continue;
			final Activity act = (Activity) pe;

			if ( !stageActivities.isStageActivity( act.getType() ) ) {
				activities.add( act );
			}
		}

		// it is not backed to the plan: fail if try to modify
		return Collections.unmodifiableList( activities );
	}

	public static List<Trip> getTrips(
			final Plan plan,
			final StageActivityTypes stageActivities) {
		final List<Trip> trips = new ArrayList<Trip>();

		final List<PlanElement> planElements = plan.getPlanElements();
		int originActivityIndex = -1;
		int currentIndex = -1;
		for (PlanElement pe : planElements) {
			currentIndex++;

			if ( !(pe instanceof Activity) ) continue;
			final Activity act = (Activity) pe;

			if (stageActivities.isStageActivity( act.getType() )) continue;
			if ( currentIndex - originActivityIndex > 1 ) {
				trips.add( new Trip(
						(Activity) planElements.get( originActivityIndex ),
						// do not back the list by the list in the plan:
						// according to the documentation, this would result
						// in an undefined behavior if the full sequence was modified
						// (for instance by modifying another trip)
						Collections.unmodifiableList(
							new ArrayList<PlanElement>(
								planElements.subList(
									originActivityIndex + 1,
									currentIndex))),
						act ) );
			}

			originActivityIndex = currentIndex;
		}

		return Collections.unmodifiableList( trips );
	}

	/**
	 * Represents a trip, that is, the longest sequence of
	 * {@link PlanElement}s consisting only of legs and "dummy"
	 * activities.
	 * <br>
	 * It also provides the references of the {@link Activity}s
	 * just before and after the trip (origin and destination),
	 * for convenience.
	 */
	public final static class Trip {
		private final Activity originActivity;
		private final Activity destinationActivity;
		private final List<PlanElement> trip;
		private final List<Leg> legs;

		Trip( 	final Activity originActivity,
				final List<PlanElement> trip,
				final Activity destinationActivity) {
			this.originActivity = originActivity;
			this.trip = trip;
			this.legs = extractLegs( trip );
			this.destinationActivity = destinationActivity;
		}

		private static List<Leg> extractLegs( final List<PlanElement> trip ) {
			final List<Leg> legs = new ArrayList<Leg>();

			for (PlanElement pe : trip) {
				if ( pe instanceof Leg ) {
					legs.add( (Leg) pe );
				}
			}

			return Collections.unmodifiableList( legs );
		}

		public Activity getOriginActivity() {
			return originActivity;
		}

		public Activity getDestinationActivity() {
			return destinationActivity;
		}

		public List<PlanElement> getTripElements() {
			return trip;
		}

		public List<Leg> getLegsOnly() {
			return legs;
		}

		@Override
		public String toString() {
			return "{Trip: origin="+originActivity+"; "+
				"trip="+trip+"; "+
				"destination="+destinationActivity+"}";
		}

		@Override
		public boolean equals(final Object other) {
			if ( !(other instanceof Trip) ) return false;

			final Trip otherTrip = (Trip) other;
			return otherTrip.originActivity.equals( originActivity ) &&
				otherTrip.trip.equals( trip ) &&
				otherTrip.destinationActivity.equals( destinationActivity );
		}

		@Override
		public int hashCode() {
			return originActivity.hashCode() + trip.hashCode() + destinationActivity.hashCode();
		}
	}
}

