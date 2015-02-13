/* *********************************************************************** *
 * project: org.matsim.*
 * BeingTogetherScoring.java
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
package playground.thibautd.socnetsim.scoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTime;

import playground.ivt.utils.MapUtils;
import playground.ivt.utils.MapUtils.Factory;

/**
 * @author thibautd
 */
public class BeingTogetherScoring {
	private final Id ego;
	private final Set<Id<Person>> alters;

	private final ActivityFacilities facilities;

	private final Filter actTypeFilter;
	private final Filter modeFilter;

	private final PersonOverlapScorer overlapScorer;

	private final Interval activeTimeWindow;

	private final Factory<IntervalsAtLocation> locatedIntervalsFactory =
		new Factory<IntervalsAtLocation>() {
			@Override
			public IntervalsAtLocation create() {
				return new IntervalsAtLocation();
			}
		};
	private final IntervalsAtLocation intervalsForEgo = new IntervalsAtLocation();
	private final Map<Id, IntervalsAtLocation> intervalsPerAlter = new HashMap<Id, IntervalsAtLocation>();

	private final Map<Id, String> currentModeOfRelevantAgents = new HashMap<Id, String>();

	public BeingTogetherScoring(
			final ActivityFacilities facilities,
			final double marginalUtilityOfTime,
			final Id ego,
			final Collection<Id<Person>> alters) {
		this( facilities,
				Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY,
				marginalUtilityOfTime,
				ego,
				alters );
	}

	public BeingTogetherScoring(
			final ActivityFacilities facilities,
			final double startActiveWindow,
			final double endActiveWindow,
			final double marginalUtilityOfTime,
			final Id ego,
			final Collection<Id<Person>> alters) {
		this( facilities,
				startActiveWindow,
				endActiveWindow,
				new AcceptAllFilter(),
				new AcceptAllFilter(),
				marginalUtilityOfTime,
				ego,
				alters );
	}

	public BeingTogetherScoring(
			final ActivityFacilities facilities,
			final Filter actTypeFilter,
			final Filter modeFilter,
			final double marginalUtilityOfTime,
			final Id ego,
			final Collection<Id<Person>> alters) {
		this( facilities,
				Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY,
				actTypeFilter,
				modeFilter,
				marginalUtilityOfTime,
				ego,
				alters );
	}

	public BeingTogetherScoring(
			final ActivityFacilities facilities,
			final Filter actTypeFilter,
			final Filter modeFilter,
			final PersonOverlapScorer scorer,
			final Id ego,
			final Collection<Id<Person>> alters) {
		this( facilities,
				Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY,
				actTypeFilter,
				modeFilter,
				scorer,
				ego,
				alters );
	}


	public BeingTogetherScoring(
			final ActivityFacilities facilities,
			final double startActiveWindow,
			final double endActiveWindow,
			final Filter actTypeFilter,
			final Filter modeFilter,
			final double marginalUtilityOfTime,
			final Id ego,
			final Collection<Id<Person>> alters) {
		this(
			facilities,
			startActiveWindow,
			endActiveWindow,
			actTypeFilter,
			modeFilter,
			new LinearOverlapScorer( marginalUtilityOfTime ),
			ego,
			alters);
	}

	public BeingTogetherScoring(
			final ActivityFacilities facilities,
			final double startActiveWindow,
			final double endActiveWindow,
			final Filter actTypeFilter,
			final Filter modeFilter,
			final PersonOverlapScorer overlapScorer,
			final Id ego,
			final Collection<Id<Person>> alters) {
		this.facilities = facilities;
		this.actTypeFilter = actTypeFilter;
		this.modeFilter = modeFilter;
		this.activeTimeWindow = new Interval( startActiveWindow , endActiveWindow );
		this.overlapScorer = overlapScorer;
		this.ego = ego;
		this.alters = Collections.unmodifiableSet( new HashSet<Id<Person>>( alters ) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// basic scoring
	// /////////////////////////////////////////////////////////////////////////
	public double getScore() {
		final Map<Id, Double> timePerSocialContact = new HashMap<Id, Double>();

		for ( Map.Entry<Location, WrappedAroundIntervalSequence> e : intervalsForEgo.map.entrySet() ) {
			final Location location = e.getKey();
			final List<Interval> egoIntervals = e.getValue().getWrappedAroundSequence();

			for (Map.Entry<Id, IntervalsAtLocation> e2 : intervalsPerAlter.entrySet() ) {
				final Id alter = e2.getKey();
				final IntervalsAtLocation locatedAlterIntervals = e2.getValue();

				final WrappedAroundIntervalSequence seq = locatedAlterIntervals.map.get( location );
				if ( seq == null ) continue;
				final List<Interval> alterIntervals = seq.getWrappedAroundSequence();

				final List<Interval> openingIntervals = getOpeningIntervals( location );

				MapUtils.addToDouble(
						alter,
						timePerSocialContact,
						0,
						calcOverlap(
							activeTimeWindow,
							openingIntervals,
							egoIntervals,
							alterIntervals ) );
			}
		}

		double accumulatedUtility = 0;
		for ( Map.Entry<Id, Double> idAndTime : timePerSocialContact.entrySet() ) {
			accumulatedUtility += overlapScorer.getScore( idAndTime.getKey() , idAndTime.getValue() );
		}

		return accumulatedUtility;
	}

	private List<Interval> getOpeningIntervals(
			final Location location) {
		// TODO: cache instead of recomputing each time?
		if ( location.facilityId == null || facilities == null ) {
			return Collections.singletonList(
					new Interval(
						Double.NEGATIVE_INFINITY,
						Double.POSITIVE_INFINITY ) );
		}

		final ActivityFacility facility = facilities.getFacilities().get( location.facilityId );
		final ActivityOption option = facility.getActivityOptions().get( location.activityType );

		if ( option.getOpeningTimes().isEmpty() ) {
			return Collections.singletonList(
					new Interval(
						Double.NEGATIVE_INFINITY,
						Double.POSITIVE_INFINITY ) );
		}

		final ArrayList<Interval> intervals = new ArrayList<Interval>();
		for ( OpeningTime openingTime : option.getOpeningTimes() ) {
			intervals.add( new Interval( openingTime.getStartTime() , openingTime.getEndTime() ) );
		}

		return intervals;
	}

	private static double calcOverlap(
			final Interval activeTimeWindow,
			final List<Interval> openingIntervals,
			final List<Interval> egoIntervals,
			final List<Interval> alterIntervals) {
		double sum = 0;
		for ( Interval ego : egoIntervals ) {
			final Interval activeEgo = intersect( ego , activeTimeWindow );

			for ( Interval open : openingIntervals ) {
				final Interval openActiveEgo = intersect( activeEgo , open );
				for ( Interval alter : alterIntervals ) {
					sum += measureOverlap( openActiveEgo , alter );
				}
			}
		}
		return sum;
	}

	private static Interval intersect(
			final Interval i1,
			final Interval i2) {
		final double startOverlap = Math.max( i1.start , i2.start );
		final double endOverlap = Math.min( i1.end , i2.end );
		// XXX end can be before start!
		return new Interval( startOverlap , endOverlap );
	}

	private static double measureOverlap(
			final Interval i1,
			final Interval i2) {
		final double startOverlap = Math.max( i1.start , i2.start );
		final double endOverlap = Math.min( i1.end , i2.end );
		return Math.max( endOverlap - startOverlap , 0 );
	}

	// /////////////////////////////////////////////////////////////////////////
	// event handling
	// /////////////////////////////////////////////////////////////////////////
	public void handleEvent(final Event event) {
		if (event instanceof PersonDepartureEvent) startMode( (PersonDepartureEvent) event );
		if (event instanceof PersonArrivalEvent) endMode( (PersonArrivalEvent) event );
		if (event instanceof ActivityStartEvent) startAct( (ActivityStartEvent) event );
		if (event instanceof ActivityEndEvent) endAct( (ActivityEndEvent) event );
		if (event instanceof PersonEntersVehicleEvent) enterVehicle( (PersonEntersVehicleEvent) event );
		if (event instanceof PersonLeavesVehicleEvent) leaveVehicle( (PersonLeavesVehicleEvent) event );
	}

	private void startMode(final PersonDepartureEvent event) {
		if ( !isRelevant( event.getPersonId() ) ) return;
		currentModeOfRelevantAgents.put( event.getPersonId() , event.getLegMode() );
	}

	private void endMode(final PersonArrivalEvent event) {
		// no need to check if "relevant agent" here
		currentModeOfRelevantAgents.remove( event.getPersonId() );
	}

	private void enterVehicle(final PersonEntersVehicleEvent event) {
		if ( !isRelevant( event.getPersonId() ) ) return;
		if ( !modeFilter.consider( currentModeOfRelevantAgents.get( event.getPersonId() ) ) ) return;
		final IntervalsAtLocation intervals =
			event.getPersonId().equals( ego ) ?
			intervalsForEgo :
			MapUtils.getArbitraryObject(
					event.getPersonId(),
					intervalsPerAlter,
					locatedIntervalsFactory);
		intervals.startInterval(
				new Location( event.getVehicleId() ),
				event.getTime() );
	}

	private void leaveVehicle(final PersonLeavesVehicleEvent event) {
		if ( !isRelevant( event.getPersonId() ) ) return;
		if ( !modeFilter.consider( currentModeOfRelevantAgents.get( event.getPersonId() ) ) ) return;
		final IntervalsAtLocation intervals =
			event.getPersonId().equals( ego ) ?
			intervalsForEgo :
			MapUtils.getArbitraryObject(
					event.getPersonId(),
					intervalsPerAlter,
					locatedIntervalsFactory);
		intervals.endInterval(
				new Location( event.getVehicleId() ),
				event.getTime() );	
	}

	private void startAct(final ActivityStartEvent event) {
		if ( !isRelevant( event.getPersonId() ) ) return;
		if ( !actTypeFilter.consider( event.getActType() ) ) return;
		final IntervalsAtLocation intervals =
			event.getPersonId().equals( ego ) ?
			intervalsForEgo :
			MapUtils.getArbitraryObject(
					event.getPersonId(),
					intervalsPerAlter,
					locatedIntervalsFactory);
		intervals.startInterval(
				new Location( event.getLinkId() , event.getFacilityId() , event.getActType() ),
				event.getTime() );		
	}

	private void endAct(final ActivityEndEvent event) {
		if ( !isRelevant( event.getPersonId() ) ) return;
		if ( !actTypeFilter.consider( event.getActType() ) ) return;
		final IntervalsAtLocation intervals =
			event.getPersonId().equals( ego ) ?
			intervalsForEgo :
			MapUtils.getArbitraryObject(
					event.getPersonId(),
					intervalsPerAlter,
					locatedIntervalsFactory);
		intervals.endInterval(
				new Location( event.getLinkId() , event.getFacilityId() , event.getActType() ),
				event.getTime() );	
	}

	private boolean isRelevant(final Id personId) {
		return ego.equals( personId ) || alters.contains( personId );
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private static class IntervalsAtLocation {
		private final Factory<WrappedAroundIntervalSequence> seqFactory =
			new Factory<WrappedAroundIntervalSequence>() {
				@Override
				public WrappedAroundIntervalSequence create() {
					return new WrappedAroundIntervalSequence();
				}
			};
		private final Map<Location, WrappedAroundIntervalSequence> map =
			new HashMap<Location,WrappedAroundIntervalSequence>();

		public void startInterval(
				final Location location,
				final double time) {
			final WrappedAroundIntervalSequence seq =
				MapUtils.getArbitraryObject(
						location,
						map,
						seqFactory);
			seq.startInterval( time );
		}

		public void endInterval(
				final Location location,
				final double time) {
			final WrappedAroundIntervalSequence seq =
				MapUtils.getArbitraryObject(
						location,
						map,
						seqFactory);
			seq.endInterval( time );
		}
	}

	private static class Location {
		private final Id vehId;
		private final Id linkId;
		private final Id facilityId;
		private final String activityType;

		public Location(final Id vehicleId) {
			this.vehId = vehicleId;
			this.linkId = null;
			this.facilityId = null;
			this.activityType = null;
		}

		public Location(
				final Id linkId,
				final Id facilityId,
				final String actType) {
			this.vehId = null;
			this.linkId = linkId;
			this.facilityId = facilityId;
			this.activityType = actType;
		}

		@Override
		public boolean equals( final Object o ) {
			return o instanceof Location &&
				areEquals( ((Location) o).vehId , vehId ) &&
				areEquals( ((Location) o).linkId , linkId ) &&
				areEquals( ((Location) o).facilityId , facilityId ) &&
				areEquals( ((Location) o).activityType , activityType );
		}
		
		private final boolean areEquals(
				final Object o1,
				final Object o2 ) {
			if ( o1 == null ) return o2 == null;
			return o1.equals( o2 );
		}

		@Override
		public int hashCode() {
			return (vehId == null ? 0 : vehId.hashCode()) +
				(linkId == null ? 0 : linkId.hashCode()) +
				(facilityId == null ? 0 : facilityId.hashCode()) +
				(activityType == null ? 0 : activityType.hashCode());
		}
	}

	private static class WrappedAroundIntervalSequence {
		private Interval first = null;
		private final List<Interval> between = new ArrayList<Interval>();
		private Interval last = null;

		public void startInterval(double time) {
			if (last != null) throw new IllegalStateException( "must close interval before starting new one" );
			last = new Interval();
			last.start = time;
		}

		public void endInterval(double time) {
			if ( last == null ) {
				assert between.isEmpty();
				assert first == null;
				first = new Interval();
				first.end = time;
			}
			else {
				last.end = time;
				between.add( last );
				last = null;
			}
		}

		public List<Interval> getWrappedAroundSequence() {
			final List<Interval> seq = new ArrayList<Interval>( between );
			if ( first != null && last != null ) {
				assert Double.isNaN( first.start );
				assert Double.isNaN( last.end );
				final Interval wrap = new Interval();
				wrap.start = last.start;
				wrap.end = first.end + 24 * 3600;
				if ( wrap.start <= wrap.end ) {
					// if time inconsistent, just do not add an interval
					// (the agent is "less than not here")
					seq.add( wrap );
				}
			}
			// XXX probably a better way
			return fitIn24Hours( seq );
		}

		private static List<Interval> fitIn24Hours(final List<Interval> seq) {
			final List<Interval> newList = new ArrayList<Interval>();

			for ( Interval old : seq ) {
				newList.addAll( splitIn24Hours( old ) );
			}

			return newList;
		}

		private static Collection<Interval> splitIn24Hours(final Interval old) {
			if ( old.start < 0 ) throw new IllegalArgumentException( ""+old.start );
			if ( old.start > old.end ) throw new IllegalArgumentException( old.start+" > "+old.end );

			final Interval newInterval = new Interval( old.start , old.end );
			if ( newInterval.start > 24 * 3600 ) {
				int c = 0;
				// shift start in day
				while ( newInterval.start > 24 * 3600 ) {
					newInterval.start -= 24 * 3600;
					c++;
				}
				// shift end by same amount
				newInterval.end = old.end - c * 24d * 3600;
			}

			if ( newInterval.end < 24 * 3600 ) return Collections.singleton( newInterval );

			final List<Interval> split = new ArrayList<Interval>();
			split.add( new Interval( old.start , 24 * 3600 ) );
			split.add( new Interval( 0 , old.end - 24 * 3600 ) );

			return split;
		}
	}

	private static class Interval {
		private double start = Double.NaN;
		private double end = Double.NaN;

		public Interval() {}
		public Interval(final double start, final double end) {
			this.start = start;
			this.end = end;
		}
	}

	public static interface Filter {
		public boolean consider(final String typeOrMode);
	}

	public static class AcceptAllFilter implements Filter {
		@Override
		public boolean consider(final String typeOrMode) {
			return true;
		}
	}

	public static class AcceptAllInListFilter implements Filter {
		private final Collection<String> toAccept = new ArrayList<String>();

		public AcceptAllInListFilter( final Iterable<String> types ) {
			for ( String s : types ) toAccept.add( s );
		}
		
		public AcceptAllInListFilter( final String... types ) {
			for ( String s : types ) toAccept.add( s );
		}

		@Override
		public boolean consider(final String typeOrMode) {
			return toAccept.contains( typeOrMode );
		}
	}

	public static class RejectAllFilter implements Filter {
		@Override
		public boolean consider(final String typeOrMode) {
			return false;
		}
	}

	public static interface PersonOverlapScorer {
		public double getScore(
				Id alter,
				double totalTimePassedTogether);
	}

	public static class LinearOverlapScorer implements PersonOverlapScorer {
		private final double marginalUtility;

		public LinearOverlapScorer(final double marginalUtility) {
			this.marginalUtility = marginalUtility;
		}

		@Override
		public double getScore(final Id alter, final double totalTimePassedTogether) {
			return marginalUtility * totalTimePassedTogether;
		}
	}

	public static class LogOverlapScorer implements PersonOverlapScorer {
		private final double marginalUtility;
		private final double typicalDuration;
		private final double zeroDuration;

		public LogOverlapScorer(
				final double marginalUtility,
				final double typicalDuration,
				final double zeroDuration) {
			this.marginalUtility = marginalUtility;
			this.typicalDuration = typicalDuration;
			this.zeroDuration = zeroDuration;
		}

		@Override
		public double getScore(final Id alter, final double totalTimePassedTogether) {
			final double log = marginalUtility * typicalDuration
						* Math.log( totalTimePassedTogether / zeroDuration );
			// penalizing being a short time with social contacts would make no sense,
			// as it would be null again when no contact at all.
			return log > 0 ? log : 0;
		}
	}
}

