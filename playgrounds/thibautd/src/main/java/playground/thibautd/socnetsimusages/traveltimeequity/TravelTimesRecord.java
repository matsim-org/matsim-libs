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

import com.google.inject.Singleton;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.misc.Time;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author thibautd
 */
@Singleton
public class TravelTimesRecord implements PersonDepartureEventHandler,
		ActivityStartEventHandler {
	private static final Logger log = Logger.getLogger(TravelTimesRecord.class);
	private final Map<Id<Person>, TravelTimesForPerson> times = new HashMap<>();
	private final Set<Id<Person>> ignoreDeparture = new HashSet<>();

	private final StageActivityTypes stageActivityTypes;

	public TravelTimesRecord(final StageActivityTypes stageActivityTypes) {
		this.stageActivityTypes = stageActivityTypes;
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		if ( log.isTraceEnabled() ) {
			log.trace( "Handling activity start "+event );
		}

		if ( stageActivityTypes.isStageActivity( event.getActType() ) ) {
			ignoreDeparture.add( event.getPersonId() );
		}
		else {
			MapUtils.getArbitraryObject(
					event.getPersonId(),
					times,
					TravelTimesForPerson::new ).addArrival(event.getTime());
		}
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		if ( log.isTraceEnabled() ) {
			log.trace( "Handling person departure "+event );
		}

		if ( !ignoreDeparture.remove( event.getPersonId() ) ) {
			MapUtils.getArbitraryObject(
					event.getPersonId(),
					times,
					TravelTimesForPerson::new ).addDeparture(event.getTime());
		}
	}

	public double getTravelTimeBefore( final Id<Person> person , final double time ) {
		return MapUtils.getArbitraryObject(
				person,
				times,
				TravelTimesForPerson::new ).getTravelTimeBefore( time );
	}

	public boolean alreadyKnowsTravelTimeAfter( final Id<Person> person , final double time ) {
		return MapUtils.getArbitraryObject(
				person,
				times,
				TravelTimesForPerson::new ).alreadyKnowsTravelTimeAfter(time);
	}

	public double getTravelTimeAfter( final Id<Person> person , final double time ) {
		return MapUtils.getArbitraryObject(
				person,
				times,
				TravelTimesForPerson::new ).getTravelTimeAfter(time);
	}

	@Override
	public void reset(final int iteration) {
		times.clear();
		ignoreDeparture.clear();
	}


	private static class TravelTimesForPerson {
		// work in full seconds to avoid numerical problems
		private final TIntList departures = new TIntArrayList();
		private final TIntList arrivals = new TIntArrayList();

		public void addDeparture( final double time ) {
			this.departures.add( (int) time );
		}

		public void addArrival( final double time ) {
			this.arrivals.add( (int) time );
		}

		public double getTravelTimeBefore( final double time ) {
			final int bs = arrivals.binarySearch( (int) time );
			final int index = bs < 0 ? -bs - 2 : bs;

			if ( index < 0 ) {
				throw new RuntimeException(
						"error search travel time before "+
								Time.writeTime( time )+" with "+arrivals.size()+" arrivals "+
								IntStream.of( arrivals.toArray() ).mapToObj( Time::writeTime ).collect( Collectors.toList() ) );
			}

			final double tt = arrivals.get( index ) - departures.get( index );
			assert tt >= 0;

			return tt;
		}

		public boolean alreadyKnowsTravelTimeAfter( final double time ) {
			assert arrivals.size() == departures.size() || arrivals.size() == departures.size() - 1;
			return !departures.isEmpty() &&
					arrivals.size() == departures.size() &&
					departures.get( departures.size() - 1 ) >= time;

		}

		public double getTravelTimeAfter( final double time ) {
			assert arrivals.size() == departures.size();
			final int bs = departures.binarySearch( (int) time );
			final int index = bs < 0 ? -bs -1 : bs;

			final double tt = arrivals.get( index ) - departures.get( index );
			assert tt >= 0;

			return tt;
		}
	}
}
