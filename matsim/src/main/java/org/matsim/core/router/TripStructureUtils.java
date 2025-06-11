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

import java.util.*;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.Attributes;

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
 * The methods require {@link StageActivityHandling} as a parameter,
 * which is used to decide whether dummy activities such as pt interaction should be
 * handled like normal activities or ignored.
 *
 * @author thibautd
 */
public final class TripStructureUtils {

	private static final Logger log = LogManager.getLogger(TripStructureUtils.class);

	public static final String routingMode = "routingMode";

	public enum StageActivityHandling {StagesAsNormalActivities, ExcludeStageActivities}

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
				case StagesAsNormalActivities:
					activities.add(act);
					break;
				case ExcludeStageActivities:
					if (!(StageActivityTypeIdentifier.isStageActivity(act.getType()))) {
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

	public static List<Trip> getTrips( final Plan plan) {
		return getTrips( plan.getPlanElements());
	}


	public static List<Trip> getTrips( final Plan plan, final Predicate<String> isStageActivity) {
		return getTrips( plan.getPlanElements(), isStageActivity);
	}

	public static List<Trip> getTrips( final List<? extends PlanElement> planElements) {
		return getTrips(planElements, TripStructureUtils::isStageActivityType ) ;
	}


	public static List<Trip> getTrips(
			final List<? extends PlanElement> planElements,
			final Predicate<String> isStageActivity ) {
		final List<Trip> trips = new ArrayList<>();

		int originActivityIndex = -1;
		int currentIndex = -1;
		for (PlanElement pe : planElements) {
			currentIndex++;

			if ( !(pe instanceof Activity) ) continue;
			final Activity act = (Activity) pe;

//			if (StageActivityTypeIdentifier.isStageActivity( act.getType() ) || stageActivityTypes.contains( act.getType() )) continue;
//			if (StageActivityTypeIdentifier.isStageActivity( act.getType() ) || isStageActivity.test( act.getType() )) continue;
			// I I don't like the || (= "or").  If I want to identify subtrips, then I want to put in a reduced number of stage
			// activities!!!!  kai, jan'20
			if ( isStageActivity.test( act.getType() ) ) {
				continue;
			}

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

	public static Collection<Subtour> getSubtours( final Plan plan) {
		return getSubtours( plan.getPlanElements(), 0 );
	}

	public static Collection<Subtour> getSubtours( final Plan plan, double coordDistance) {
		return getSubtours( plan.getPlanElements(), coordDistance );
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
	 * <br>
	 * Note: We (VSP) are not sure what this code does exactly. The correct definition
	 * of what a subtour is in MATSim needs to be found!
	 * Theresa, VSP mode choice seminar in jul'22
	 *
	 * @param coordDistance if larger 0, also consider coordinates to be at same location if smaller than distance
	 *
	 * @throws RuntimeException if the Trip sequence has inconsistent location
	 * sequence
	 */
	public static Collection<Subtour> getSubtours( final List<? extends PlanElement> planElements, double coordDistance) {
		return getSubtours(planElements, TripStructureUtils::isStageActivityType, coordDistance );
	}

	/**
	 * Returns the top-level tour as {@link Subtour} object even if it is unclosed. This subtour will always
	 * contain all trips of a plan. Child tours will not be set.
	 * @see Subtour
	 */
	public static Subtour getUnclosedRootSubtour(final Plan plan) {
		return new Subtour(TripStructureUtils.getTrips(plan), false);
	}

	// for contrib socnetsim only
	// I think now that we should actually keep this.  kai, jan'20
	@Deprecated
	public static Collection<Subtour> getSubtours( final Plan plan, final Predicate<String> isStageActivity) {
		return getSubtours( plan.getPlanElements(), isStageActivity, 0);
	}

	public static Collection<Subtour> getSubtours(
		final List<? extends PlanElement> planElements,
		final Predicate<String> isStageActivity, double coordDistance) {
		return getSubtoursFromTrips(getTrips(planElements, isStageActivity), coordDistance);
	}

	public static Collection<Subtour> getSubtoursFromTrips(List<Trip> trips, double coordDistance) {

		final List<Subtour> subtours = new ArrayList<>();
		Object destinationId = null;

		// can be either id or coordinate
		final List<Object> originIds = new ArrayList<>();
		final List<Trip> nonAllocatedTrips = new ArrayList<>( trips );

		for (Trip trip : trips) {
			final Object originId;
			//use facilities if available
			if (trip.getOriginActivity().getFacilityId() != null) {
				originId = trip.getOriginActivity().getFacilityId();
			} else if (coordDistance > 0 && trip.getOriginActivity().getCoord() != null) {
				originId = trip.getOriginActivity().getCoord();
			} else {
				originId = trip.getOriginActivity().getLinkId();
			}

			if ( originId == null ) {
				throw new NullPointerException( "Facility id, link id and coordinates for origin activity "+trip.getOriginActivity()+
										" are null!" );
			}

			if (destinationId != null && !originId.equals( destinationId )) {
				throw new RuntimeException( "unconsistent trip location sequence: "+destinationId+" != "+originId );
			}

			if (trip.getDestinationActivity().getFacilityId() != null) {
				destinationId = trip.getDestinationActivity().getFacilityId();
			} else if (coordDistance > 0 && trip.getDestinationActivity().getCoord() != null) {
				destinationId = trip.getDestinationActivity().getCoord();
			} else {
				destinationId = trip.getDestinationActivity().getLinkId();
			}

			if ( destinationId == null ) {
				throw new NullPointerException( "Facility id, and link id and coordinates for destination activity "+trip.getDestinationActivity()+
										" are null!" );
			}

			originIds.add( originId );

			int lastIdx = originIds.lastIndexOf(destinationId);

			// fuzzy lookup for last idx based on coordinates
			if (coordDistance > 0 && destinationId instanceof Coord destinationCoord) {
				for (int i = originIds.size() - 1; i >= 0; i--) {

					Object cmp = originIds.get(i);
					if (cmp instanceof Coord cmpCoord) {
						if (CoordUtils.calcEuclideanDistance(destinationCoord, cmpCoord) <= coordDistance) {
							lastIdx = i;
							break;
						}
					}
				}
			}

			if (lastIdx > -1) {
				// end of a subtour
				final int subtourStartIndex = lastIdx;
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
	public static OptionalTime getDepartureTime(Trip trip) {
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

		/**
		 * Attributes of preceding activity are passed as trip attributes until more explicit encoding is found.
		 */
		public Attributes getTripAttributes() {
			return originActivity.getAttributes();
		}

		@Override
		public String toString() {
			return "{Trip: origin="+originActivity+"; "+
					       "trip="+trip+"; "+
					       "destination="+destinationActivity + "; " +
					       getTripAttributes().toString() + "}";
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
			return children2.size() == children3.size();// should check more, but risk of infinite recursion...

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
	public static Trip findCurrentTrip( PlanElement pe, Plan plan ) {
		return findTripAtPlanElement( pe, plan ) ;
	}

	public static Trip findTripAtPlanElement( PlanElement currentPlanElement, Plan plan ){
		return findTripAtPlanElement( currentPlanElement, plan, TripStructureUtils::isStageActivityType ) ;
	}
	public static Trip findTripAtPlanElement( PlanElement currentPlanElement, Plan plan, Predicate<String> isStageActivity ) {
		if ( currentPlanElement instanceof Activity ) {
//			Gbl.assertIf( StageActivityTypeIdentifier.isStageActivity( ((Activity)currentPlanElement).getType() ) ) ;
			Gbl.assertIf( isStageActivity.test( ((Activity)currentPlanElement).getType() ) ) ;
		}
		List<Trip> trips = getTrips(plan.getPlanElements(), isStageActivity) ;
		for ( Trip trip : trips ) {
			int index = trip.getTripElements().indexOf( currentPlanElement ) ;
			if ( index != -1 ) {
				return trip ;
			}
		}
		return null ;
	}

	public static Trip findTripEndingAtActivity(Activity activity, Plan plan) {
		Gbl.assertIf( ! StageActivityTypeIdentifier.isStageActivity( activity.getType()) ) ;
		List<Trip> trips = getTrips(plan.getPlanElements()) ;
		for ( Trip trip : trips ) {
			if ( activity.equals( trip.getDestinationActivity() ) ) {
				return trip;
			}
		}
		return null ;
	}

	public static Trip findTripStartingAtActivity( final Activity activity, final Plan plan ) {
		Gbl.assertIf( ! StageActivityTypeIdentifier.isStageActivity( activity.getType()) ) ;
		List<Trip> trips = getTrips( plan ) ;
		for ( Trip trip : trips ) {
			if ( trip.getOriginActivity().equals( activity ) ) {
				return trip ;
			}
		}
		return null ;
	}

	public static String getRoutingMode(Leg leg) {
		return leg.getRoutingMode();
	}

	public static void setRoutingMode(Leg leg, String mode) {
		leg.setRoutingMode(mode);
	}

	// if we make the routing mode identifier replaceable via Guice/Inject, we should return that one here or get rid of the method
	public static MainModeIdentifier getRoutingModeIdentifier() {
		return new RoutingModeMainModeIdentifier();
	}

	public static String identifyMainMode( final List<? extends PlanElement> tripElements) {
		// first try the routing mode:
		String mode = TripStructureUtils.getRoutingMode(((Leg) tripElements.get( 0 )));
		// else see if trip has only one leg, if so, use that mode (situation after initial demand generation)
		if ( mode == null && tripElements.size()==1 ) {
			mode = ((Leg) tripElements.get(0)).getMode() ;
		}
		if (mode == null) {
			log.error("Could not find routing mode for trip " + tripElements);
		}
		return mode;
	}

	public static boolean isStageActivityType( String activityType ) {
		return StageActivityTypeIdentifier.isStageActivity( activityType ) ;
	}
	public static String createStageActivityType( String mode ) {
		return ScoringConfigGroup.createStageActivityType( mode ) ;
	}

}

