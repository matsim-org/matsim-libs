/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeRetrofittingEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;

import org.matsim.core.utils.collections.MapUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * "Retrofits" travel time from events in the plans.
 * This is considered pretty bad practice, but it is the best way to get an efficient
 * "learning" teleportation-based pSim. Otherwise, completely out-of-date travel times
 * are used in pSim, leading to undefined results...
 * @author thibautd
 */
@Singleton
public class TravelTimeRetrofittingEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {
	private final Population population;

	// all the stateful part goes there: makes resetting easy
	private LegHandler legHandler;

	@Inject
	public TravelTimeRetrofittingEventHandler(
			final Scenario sc) {
		this( sc.getPopulation() );
	}

	public TravelTimeRetrofittingEventHandler(
			final Population population) {
		this.population = population;
	}

	@Override
	public void reset(final int iteration) {
		legHandler = new LegHandler();
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		legHandler.endLeg( event );
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		legHandler.startLeg( event );
	}

	private class LegHandler {
		private final Map<Id, Iterator<Leg>> legIterators = new HashMap<Id, Iterator<Leg>>( population.getPersons().size() );
		private final Map<Id, Double> departure = new HashMap<Id, Double>();

		public void startLeg( final PersonDepartureEvent event ) {
			departure.put( event.getPersonId() , event.getTime() );
		}

		public void endLeg( final PersonArrivalEvent event ) {
			final Person person = population.getPersons().get( event.getPersonId() );
			if ( person == null ) return;
			final Iterator<Leg> it =
				MapUtils.getArbitraryObject(
						event.getPersonId(),
						legIterators,
						new MapUtils.Factory<Iterator<Leg>>() {
							@Override
							public Iterator<Leg> create() {
								return TripStructureUtils.getLegs(
										person.getSelectedPlan() ).iterator();
							}
						});
			final Leg leg = it.next();
			if ( !leg.getMode().equals( event.getLegMode() ) ) {
				throw new RuntimeException( "leg "+leg+" does not have mode "+event.getLegMode()+" of event "+event );
			}
			final double tt = event.getTime() - departure.remove( event.getPersonId() );
			leg.setTravelTime( tt );
			leg.getRoute().setTravelTime( tt );
		}
	}
}
