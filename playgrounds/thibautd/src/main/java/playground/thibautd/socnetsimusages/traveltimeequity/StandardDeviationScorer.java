/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.socnetsimusages.traveltimeequity;

import gnu.trove.TDoubleCollection;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.events.CourtesyEvent;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.misc.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * @author thibautd
 */
public class StandardDeviationScorer implements SumScoringFunction.ArbitraryEventScoring {
	private static final Logger log = Logger.getLogger(StandardDeviationScorer.class);
	private final TravelTimesRecord travelTimesRecord;
	private final Set<String> activityType;

	private final List<ActivityValues> activityValues = new ArrayList<>( 2 );
	private ActivityValues currentActivityValues = null;
	private final double betaStdDev;

	public StandardDeviationScorer(
			final TravelTimesRecord travelTimesRecord,
			final Set<String> activityType,
			final double betaStdDev) {
		this.travelTimesRecord = travelTimesRecord;
		this.activityType = activityType;
		this.betaStdDev = betaStdDev;
	}

	@Override
	public void handleEvent(final Event event) {
		if ( (event instanceof CourtesyEvent) ) handleCourtesyEvent( (CourtesyEvent) event );
		if ( (event instanceof ActivityStartEvent) ) handleActivityStartEvent((ActivityStartEvent) event);
		if ( (event instanceof ActivityEndEvent) ) handleActivityEndEvent((ActivityEndEvent) event);
	}

	private void handleActivityStartEvent(final ActivityStartEvent event) {
		if ( activityType.contains( event.getActType() ) ) {
			currentActivityValues = new ActivityValues();
			activityValues.add( currentActivityValues );

			currentActivityValues.addArrival( event.getPersonId() , event.getTime() );
		}
		else currentActivityValues = null;
	}

	private void handleActivityEndEvent(final ActivityEndEvent event) {
		if ( currentActivityValues != null ) {
			currentActivityValues.addDeparture( event.getPersonId() , event.getTime() );
		}
	}

	private void handleCourtesyEvent(final CourtesyEvent event) {
		if ( currentActivityValues == null ) return;

		switch ( event.getType() ) {
			case sayHelloEvent:
				currentActivityValues.addArrival( event.getAlterId() , event.getTime() );
				break;
			case sayGoodbyeEvent:
				currentActivityValues.addDeparture(event.getAlterId(), event.getTime());
				break;
			default:
				throw new RuntimeException( event.getType()+" not recognized!?" );
		}
	}

	@Override
	public void finish() {}

	@Override
	public double getScore() {
		double sumStdDevs = 0;
		for ( ActivityValues values : activityValues ) sumStdDevs += values.calcNormalizedStdDev();

		assert sumStdDevs >= 0;
		if ( log.isTraceEnabled() && sumStdDevs > 0 ) {
			log.trace( "got sum of standard deviations of "+sumStdDevs );
		}

		return betaStdDev * sumStdDevs;
	}

	private class ActivityValues {
		private final TDoubleArrayList arrivalTimes = new TDoubleArrayList();
		private final List<Id<Person>> arrivalPersons = new ArrayList<>();

		private final TDoubleArrayList departureTimes = new TDoubleArrayList();
		private final List<Id<Person>> departurePersons = new ArrayList<>();

		public void addArrival( final Id<Person> person , final double time ) {
			if ( log.isTraceEnabled() ) log.trace( "remember arrival for "+person+" at "+time );
			arrivalPersons.add( person );
			arrivalTimes.add( time );
		}

		public void addDeparture( final Id<Person> person , final double time ) {
			if ( log.isTraceEnabled() ) log.trace( "remember departure for "+person+" at "+time );
			departurePersons.add( person );
			departureTimes.add( time );
		}

		public double calcNormalizedStdDev() {
			// assert departureTimes.size() == arrivalTimes.size();
			// not the case before activity ends (which might never happen)
			// for each person, sum the travel times: we do not want to enforce that access and egress of an agent have
			// the same length, but similarity across agents.
			final TObjectDoubleHashMap<Id<Person>> travelTimes = new TObjectDoubleHashMap<>();

			assert arrivalTimes.size() == arrivalPersons.size();
			for ( int i=0; i < arrivalTimes.size(); i++ ) {
				final double tt =
						getTravelTimeBefore( i );

				travelTimes.adjustOrPutValue(
						arrivalPersons.get( i ),
						tt, tt);
			}

			assert departureTimes.size() == departurePersons.size();
			for ( int i=0; i < departureTimes.size(); i++ ) {
				// For several reasons, there might not (yet) be any subsequent trip:
				// - some parts of the code ask for "partial" score, possibly before the trip is completed
				// - an agent might fail to complete its plan
				// In those cases, estimate the total travel time using 2x the access travel time
				final double tt =
						travelTimesRecord.alreadyKnowsTravelTimeAfter(
								departurePersons.get( i ),
								departureTimes.get( i ) ) ?
							travelTimesRecord.getTravelTimeAfter(
									departurePersons.get( i ),
									departureTimes.get( i ) ) :
							travelTimesRecord.getTravelTimeBefore(
									departurePersons.get( i ),
									departureTimes.get( i ) );

				travelTimes.adjustOrPutValue(
						departurePersons.get(i),
						tt, tt);
			}

			return calcNormalizedStdDev(travelTimes.valueCollection());
		}

		private double getTravelTimeBefore( final int i ) {
			try {
				return travelTimesRecord.getTravelTimeBefore(
						arrivalPersons.get( i ),
						arrivalTimes.get( i ) );
			}
			catch (Exception e) {
				throw new RuntimeException(
						"problem searching travel time before "+ Time.writeTime( arrivalTimes.get( i ) )+
								" for person "+arrivalPersons.get( i )+" at index "+i+" in times "+
								DoubleStream.of( arrivalTimes.toArray() )
										.mapToObj( Time::writeTime )
										.collect(
												Collectors.toList() )+
								" for persons "+arrivalPersons ,
						e );
			}
		}

		private double calcNormalizedStdDev(final TDoubleCollection travelTimes) {
			double avg = 0;
			for ( double tt : travelTimes.toArray() ) avg += tt;
			avg /= travelTimes.size();

			double stdDev = 0;
			for ( double tt : travelTimes.toArray() ) stdDev += Math.abs( tt - avg );
			stdDev /= travelTimes.size();

			return stdDev;
		}
	}

}
