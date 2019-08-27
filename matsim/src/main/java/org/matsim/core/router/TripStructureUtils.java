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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;

/**
 * Helps to work on plans with complex trips.
 * It provides convenience methods to get only the non-dummy activities
 * or the information about trips, in a way hopefully clean and useful.
 * <br>
 * The collections returned by this class are immutable.
 * <br>
 * Two versions of the methods are provided, working on {@link Plan}s
 * or lists of {@link PlanElement}s.
 * <br>
 * The methods require an instance of {@link StageActivityTypes} as a parameter,
 * which is used to identify the dummy activities pertaining to trips.
 * In almost all use-cases, it should come from {@link TripRouter#getStageActivityTypes()}.
 *
 * @author thibautd
 */
public class TripStructureUtils {
	
	public enum StageActivityHandling { IncludeStageActivities, ExcludeStageActivities };

	private TripStructureUtils() {}

	// also need this for plain old fashioned legs.  kai
	public static List<Leg> getLegs(final Plan plan) {
		return getLegs( plan.getPlanElements() );
	}

	public static List<Leg> getLegs( final List<? extends PlanElement> planElements ) {
		final List<Leg> legs = new ArrayList<>();

		for (PlanElement pe : planElements) {
			if ( !(pe instanceof Leg) ) continue;
			legs.add( (Leg) pe );
		}

		// it is not backed to the plan: fail if try to modify
		return Collections.unmodifiableList( legs );
	}

	public static List<Activity> getActivities(
			final Plan plan,
			final StageActivityHandling stageActivityHandling) {
		return getActivities(
				plan.getPlanElements(),
				stageActivityHandling);
	}

	public static List<Activity> getActivities(
			final List<? extends PlanElement> planElements,
			final StageActivityHandling stageActivityHandling) {
		final List<Activity> activities = new ArrayList<>();

		for (PlanElement pe : planElements) {
			if ( !(pe instanceof Activity) ) continue;
			final Activity act = (Activity) pe;

			switch (stageActivityHandling) {
			case IncludeStageActivities:
				activities.add(act);
				break;
			case ExcludeStageActivities:
				if (!(new StageActivityTypesImpl().isStageActivity(act.getType()))) {
					activities.add(act);
				}
				break;
			default:
				throw new RuntimeException(Gbl.NOT_IMPLEMENTED);
			}
		}

		// it is not backed to the plan: fail if try to modify
		return Collections.unmodifiableList( activities );
	}

	public static List<Trip> getTrips(
			final Plan plan) {
		return getTrips(
				plan.getPlanElements());
	}

	// TODO: delete
	@Deprecated
	public static List<Trip> getTrips(
			final Plan plan,
			final StageActivityTypes stageActivityTypes) {
		return getTrips(
				plan.getPlanElements());
	}
	
	@Deprecated
	public static List<Trip> getTrips(
			final Plan plan,
			final Set<String> stageActivityTypes) {
		return getTrips(
				plan.getPlanElements(),
				stageActivityTypes);
	}

	public static List<Trip> getTrips(
			final List<? extends PlanElement> planElements) {
		final List<Trip> trips = new ArrayList<>();

		int originActivityIndex = -1;
		int currentIndex = -1;
		for (PlanElement pe : planElements) {
			currentIndex++;

			if ( !(pe instanceof Activity) ) continue;
			final Activity act = (Activity) pe;

			if (StageActivityTypeIdentifier.isStageActivity( act.getType() )) continue;
			if ( currentIndex - originActivityIndex > 1 ) {
				// which means, if I am understanding this right, that two activities without a leg in between will not be considered
				// a trip.  

				trips.add( new Trip(
						(Activity) planElements.get( originActivityIndex ),
						// do not back the list by the list in the plan:
						// according to the documentation, this would result
						// in an undefined behavior if the full sequence was modified
						// (for instance by modifying another trip)
						Collections.unmodifiableList(
								new ArrayList<>(
										planElements.subList(
												originActivityIndex + 1,
												currentIndex))),
						act ) );
			}

			originActivityIndex = currentIndex;
		}

		return Collections.unmodifiableList( trips );
	}
	
	@Deprecated
	public static List<Trip> getTrips(
			final List<? extends PlanElement> planElements,
			final Set<String> stageActivityTypes) {
		final List<Trip> trips = new ArrayList<>();

		int originActivityIndex = -1;
		int currentIndex = -1;
		for (PlanElement pe : planElements) {
			currentIndex++;

			if ( !(pe instanceof Activity) ) continue;
			final Activity act = (Activity) pe;

			if (StageActivityTypeIdentifier.isStageActivity( act.getType() ) || 
					stageActivityTypes.contains( act.getType() )) continue;
			if ( currentIndex - originActivityIndex > 1 ) {
				// which means, if I am understanding this right, that two activities without a leg in between will not be considered
				// a trip.  

				trips.add( new Trip(
						(Activity) planElements.get( originActivityIndex ),
						// do not back the list by the list in the plan:
						// according to the documentation, this would result
						// in an undefined behavior if the full sequence was modified
						// (for instance by modifying another trip)
						Collections.unmodifiableList(
								new ArrayList<>(
										planElements.subList(
												originActivityIndex + 1,
												currentIndex))),
						act ) );
			}

			originActivityIndex = currentIndex;
		}

		return Collections.unmodifiableList( trips );
	}

	public static Collection<Subtour> getSubtours(
            final Plan plan,
            final StageActivityTypes stageActivityTypes) {
		return getSubtours(
				plan.getPlanElements(),
				stageActivityTypes
        );
	}

	/**
	 * Gives access to a list of the subtours in the plan.
	 * A subtour is undestood as the smallest sequence of trips starting
	 * and ending at the same location (known as anchor point).
	 * <br>
	 * The subtour structure is a tree: a subtour may have a father subtour,
	 * understood as the smallest subtour containing it, and children subtours,
	 * which are the "biggest" subtours it contains (ie the subtours it is the father
	 * of).
	 * Methods are provided to get or all the trips between the two anchor activities,
	 * or just the trips not being part of any child subtour.
	 * <br>
	 * In case of "open" plans (which do not end by a subtour), one of the
	 * {@link Subtour} objects contains a non-circular sequence of Trips
	 * (ie it is not a subtour according to the definition above).
	 * In this case, the {@link Subtour#isClosed()} method returns false.
	 * <br>
	 * The order of iteration of the returned collection should not be considered
	 * as significant, nor even stable facing refactorings.
	 * <br>
	 * It is able to handle non-strict act/leg sequence and complex trips;
	 * in case of successive activities not being located at the same location
	 * (that is, if the origin of a trip is not the destination of the preceding
	 * trip), an exception will be thrown.
	 *
	 * @throws RuntimeException if the Trip sequence has inconsistent location
	 * sequence
	 */
	public static Collection<Subtour> getSubtours(
            final List<? extends PlanElement> planElements,
            final StageActivityTypes stageActivityTypes) {
		final List<Subtour> subtours = new ArrayList<>();

		Id<?> destinationId = null;
		final List<Id<?>> originIds = new ArrayList<>();
		final List<Trip> trips = getTrips( planElements );
		final List<Trip> nonAllocatedTrips = new ArrayList<>( trips );
		for (Trip trip : trips) {
            final Id<?> originId;
            //use facilities if available
		    if (trip.getOriginActivity().getFacilityId()!=null ) {
		        originId = trip.getOriginActivity().getFacilityId();
            } else {
		        originId = trip.getOriginActivity().getLinkId();
            }

					if ( originId == null ) {
						throw new NullPointerException( "Both facility id and link id for origin activity "+trip.getOriginActivity()+
								" are null!" );
					}

					if (destinationId != null && !originId.equals( destinationId )) {
						throw new RuntimeException( "unconsistent trip location sequence: "+destinationId+" != "+originId );
					}

            if (trip.getDestinationActivity().getFacilityId()!=null ) {
                destinationId = trip.getDestinationActivity().getFacilityId();
            } else {
                destinationId = trip.getDestinationActivity().getLinkId();
            }

							if ( destinationId == null ) {
								throw new NullPointerException( "Both facility id and link id for destination activity "+trip.getDestinationActivity()+
										" are null!" );
							}

							originIds.add( originId );

							if (originIds.contains( destinationId )) {
								// end of a subtour
								final int subtourStartIndex = originIds.lastIndexOf( destinationId );
								final int subtourEndIndex = originIds.size();

								final List<Trip> subtour = new ArrayList<>( trips.subList( subtourStartIndex , subtourEndIndex ) );
								nonAllocatedTrips.removeAll( subtour );

								// do not consider the locations visited in finished subtours
								// as possible anchor points
								for (int i=subtourStartIndex; i < subtourEndIndex; i++) {
									originIds.set( i , null );
								}

								addSubtourAndUpdateParents(
										subtours,
										new Subtour(
												subtourStartIndex,
												subtourEndIndex,
												subtour,
												true) );
							}
		}

		if (nonAllocatedTrips.size() != 0) {
			// "open" plan: the root is the sequence of all trips,
			// even if it is not closed
			addSubtourAndUpdateParents(
					subtours,
					new Subtour(
							0,
							trips.size(),
							new ArrayList<>( trips ),
							false));
		}

		return Collections.unmodifiableList( subtours );
	}

	@Deprecated
	public static Collection<Subtour> getSubtours(
            final Plan plan,
            final Set<String> stageActivityTypes) {
		return getSubtours(
				plan.getPlanElements(),
				stageActivityTypes
        );
	}
	
	@Deprecated
	public static Collection<Subtour> getSubtours(
            final List<? extends PlanElement> planElements,
            final Set<String> stageActivityTypes) {
		final List<Subtour> subtours = new ArrayList<>();

		Id<?> destinationId = null;
		final List<Id<?>> originIds = new ArrayList<>();
		final List<Trip> trips = getTrips( planElements, stageActivityTypes );
		final List<Trip> nonAllocatedTrips = new ArrayList<>( trips );
		for (Trip trip : trips) {
            final Id<?> originId;
            //use facilities if available
		    if (trip.getOriginActivity().getFacilityId()!=null ) {
		        originId = trip.getOriginActivity().getFacilityId();
            } else {
		        originId = trip.getOriginActivity().getLinkId();
            }

					if ( originId == null ) {
						throw new NullPointerException( "Both facility id and link id for origin activity "+trip.getOriginActivity()+
								" are null!" );
					}

					if (destinationId != null && !originId.equals( destinationId )) {
						throw new RuntimeException( "unconsistent trip location sequence: "+destinationId+" != "+originId );
					}

            if (trip.getDestinationActivity().getFacilityId()!=null ) {
                destinationId = trip.getDestinationActivity().getFacilityId();
            } else {
                destinationId = trip.getDestinationActivity().getLinkId();
            }

							if ( destinationId == null ) {
								throw new NullPointerException( "Both facility id and link id for destination activity "+trip.getDestinationActivity()+
										" are null!" );
							}

							originIds.add( originId );

							if (originIds.contains( destinationId )) {
								// end of a subtour
								final int subtourStartIndex = originIds.lastIndexOf( destinationId );
								final int subtourEndIndex = originIds.size();

								final List<Trip> subtour = new ArrayList<>( trips.subList( subtourStartIndex , subtourEndIndex ) );
								nonAllocatedTrips.removeAll( subtour );

								// do not consider the locations visited in finished subtours
								// as possible anchor points
								for (int i=subtourStartIndex; i < subtourEndIndex; i++) {
									originIds.set( i , null );
								}

								addSubtourAndUpdateParents(
										subtours,
										new Subtour(
												subtourStartIndex,
												subtourEndIndex,
												subtour,
												true) );
							}
		}

		if (nonAllocatedTrips.size() != 0) {
			// "open" plan: the root is the sequence of all trips,
			// even if it is not closed
			addSubtourAndUpdateParents(
					subtours,
					new Subtour(
							0,
							trips.size(),
							new ArrayList<>( trips ),
							false));
		}

		return Collections.unmodifiableList( subtours );
	}

	private static void addSubtourAndUpdateParents(
			final List<Subtour> subtours,
			final Subtour newSubtour) {
		// the parent of a subtour is the first found enclosing subtour
		for (Subtour existingSubtour : subtours) {
			if ( existingSubtour.parent != null ) continue;
			if ( existingSubtour.startIndex < newSubtour.startIndex ) continue;
			if ( existingSubtour.endIndex < newSubtour.startIndex ) continue;

			// the trips are parsed in sequence, so it is not possible
			// that a existing subtour contains elements later than the
			// end of the new subtour.
			assert existingSubtour.startIndex < newSubtour.endIndex;
			assert existingSubtour.endIndex <= newSubtour.endIndex;

			existingSubtour.parent = newSubtour;
			newSubtour.children.add( existingSubtour );
		}
		subtours.add( newSubtour );
	}
	
	/**
	 * @param trip
	 * @return the departure time of the first leg of the trip
	 */
	public static double getDepartureTime(Trip trip) {
		// does this always make sense?
		Leg leg = (Leg) trip.getTripElements().get(0);
		return leg.getDepartureTime();
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
			final List<Leg> legs = new ArrayList<>();

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

	public static final class Subtour {
		// this is used at construction to find parents,
		// but I do not think this should be made accessible from outside.
		// I even think it should be stored somewhere else.
		// td, feb.13
		private final int startIndex;
		private final int endIndex;

		private final List<Trip> trips;
		private final boolean isClosed;
		Subtour parent = null;
		final List<Subtour> children = new ArrayList<>();

		// for tests
		Subtour(final List<Trip> trips, final boolean isClosed) {
			this( -1 , -1 , trips , isClosed );
		}

		private Subtour(final int startIndex,
				final int endIndex,
				final List<Trip> trips,
				final boolean isClosed) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.trips = Collections.unmodifiableList( trips );
			this.isClosed = isClosed;
		}

		public List<Trip> getTrips() {
			return trips;
		}

		public List<Trip> getTripsWithoutSubSubtours() {
			final List<Trip> list = new ArrayList<>();

			for (Trip t : trips) {
				boolean isInChildSt = false;
				for (Subtour child : children) {
					if ( child.contains( t ) ) {
						isInChildSt = true;
						break;
					}
				}

				if ( !isInChildSt ) {
					list.add( t );
				}
			}

			return list;
		}

		private boolean contains(final Trip t) {
			return trips.contains( t );
		}

		public Subtour getParent() {
			return this.parent;
		}

		public Collection<Subtour> getChildren() {
			return Collections.unmodifiableList( children );
		}

		public boolean isClosed() {
			return isClosed;
		}

		@Override
		public boolean equals(final Object other) {
			if (other == null) return false;
			if ( !other.getClass().equals( getClass() ) ) return false;
			final Subtour s = (Subtour) other;
			return s.trips.equals( trips ) &&
					areChildrenCompatible( children , s.children ) &&
					(s.parent == null ? parent == null : s.parent.equals( parent )) &&
					(s.isClosed == isClosed);
		}

		private static boolean areChildrenCompatible(
				final List<Subtour> children2,
				final List<Subtour> children3) {
			if ( children2.size() != children3.size() ) return false;

			// should check more, but risk of infinite recursion...

			return true;
		}

		@Override
		public int hashCode() {
			return trips.hashCode();
		}

		@Override
		public String toString() {
			return "Subtour: "+trips.toString();
		}
	}
	@Deprecated // use findTripAtPlanElement(...) instead.
	public static Trip findCurrentTrip( PlanElement pe, Plan plan, StageActivityTypes sat ) {
		return findTripAtPlanElement( pe, plan, sat ) ;
	}
	public static Trip findTripAtPlanElement( PlanElement currentPlanElement, Plan plan, StageActivityTypes stageActivities ) {
		if ( currentPlanElement instanceof Activity ) {
			Gbl.assertIf( stageActivities.isStageActivity( ((Activity)currentPlanElement).getType() ) ) ;
		}
		List<Trip> trips = getTrips(plan.getPlanElements()) ;
		for ( Trip trip : trips ) {
			int index = trip.getTripElements().indexOf( currentPlanElement ) ;
			if ( index != -1 ) {
				return trip ;
			}
		}
		return null ;
	}
	public static Trip findTripEndingAtActivity(Activity activity, Plan plan, StageActivityTypes stageActivities ) {
		Gbl.assertIf( ! stageActivities.isStageActivity( activity.getType()) ) ;
		List<Trip> trips = getTrips(plan.getPlanElements()) ;
		for ( Trip trip : trips ) {
			if ( activity.equals( trip.getDestinationActivity() ) ) {
				return trip;
			}
		}
		return null ;
	}
	public static Trip findTripStartingAtActivity( final Activity activity, final Plan plan, StageActivityTypes stageActivities ) {
		Gbl.assertIf( ! stageActivities.isStageActivity( activity.getType()) ) ;
		List<Trip> trips = getTrips( plan, stageActivities ) ;
		for ( Trip trip : trips ) {
			if ( trip.getOriginActivity().equals( activity ) ) {
				return trip ;
			}
		}
		return null ;
	}


}

