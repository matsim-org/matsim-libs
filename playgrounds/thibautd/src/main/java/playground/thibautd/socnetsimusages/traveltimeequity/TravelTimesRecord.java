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

import gnu.trove.list.array.TDoubleArrayList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.utils.collections.MapUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
public class TravelTimesRecord implements PersonDepartureEventHandler,
		ActivityStartEventHandler {
	private static final Logger log = Logger.getLogger(TravelTimesRecord.class);
	private final Map<Id<Person>, TravelTimesForPerson> times = new HashMap<>();
	private final Set<Id<Person>> ignoreDeparture = new HashSet<>();

	private final StageActivityTypes stageActivityTypes;

	private final MapUtils.Factory<TravelTimesForPerson> factory =
			new MapUtils.Factory<TravelTimesForPerson>() {
				@Override
				public TravelTimesForPerson create() {
					return new TravelTimesForPerson();
				}
			};

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
					factory ).addArrival(event.getTime());
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
					factory).addDeparture(event.getTime());
		}
	}

	public double getTravelTimeBefore( final Id<Person> person , final double time ) {
		return MapUtils.getArbitraryObject(
				person,
				times,
				factory).getTravelTimeBefore( time );
	}

	public boolean alreadyKnowsTravelTimeAfter( final Id<Person> person , final double time ) {
		return MapUtils.getArbitraryObject(
				person,
				times,
				factory).alreadyKnowsTravelTimeAfter(time);
	}

	public double getTravelTimeAfter( final Id<Person> person , final double time ) {
		return MapUtils.getArbitraryObject(
				person,
				times,
				factory).getTravelTimeAfter(time);
	}

	@Override
	public void reset(final int iteration) {
		times.clear();
		ignoreDeparture.clear();
	}


	private static class TravelTimesForPerson {
		private final TDoubleArrayList departures = new TDoubleArrayList();
		private final TDoubleArrayList arrivals = new TDoubleArrayList();

		public void addDeparture( final double time ) {
			this.departures.add( time );
		}

		public void addArrival( final double time ) {
			this.arrivals.add( time );
		}

		public double getTravelTimeBefore( final double time ) {
			final int bs = arrivals.binarySearch( time );
			final int index = bs < 0 ? -bs - 2 : bs;

			final double tt = arrivals.get( index ) - departures.get( index );
			assert tt >= 0;

			return tt;
		}

		public boolean alreadyKnowsTravelTimeAfter( final double time ) {
			return departures.get( departures.size() - 1 ) >= time;

		}
		public double getTravelTimeAfter( final double time ) {
			final int bs = departures.binarySearch( time );
			final int index = bs < 0 ? -bs -1 : bs;

			final double tt = arrivals.get( index ) - departures.get( index );
			assert tt >= 0;

			return tt;
		}
	}
}
