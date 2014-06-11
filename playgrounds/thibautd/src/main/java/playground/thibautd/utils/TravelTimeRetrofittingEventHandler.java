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

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;

/**
 * "Retrofits" travel time from events in the plans.
 * This is considered pretty bad practice, but it is the best way to get an efficient
 * "learning" teleportation-based pSim. Otherwise, completely out-of-date travel times
 * are used in pSim, leading to undefined results...
 * @author thibautd
 */
public class TravelTimeRetrofittingEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {
	private final Population population;

	// all the stateful part goes there: makes resetting easy
	private LegHandler legHandler;

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
		legHandler.endLeg( event.getPersonId() , event.getTime() );
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		legHandler.startLeg( event.getPersonId() , event.getTime() );
	}

	private class LegHandler {
		private final Map<Id, Iterator<Leg>> legIterators = new HashMap<Id, Iterator<Leg>>( population.getPersons().size() );
		private final Map<Id, Double> departure = new HashMap<Id, Double>();

		public void startLeg( final Id person , final double time ) {
			departure.put( person , time );
		}

		public void endLeg( final Id person , final double time ) {
			final Iterator<Leg> it =
				MapUtils.getArbitraryObject(
						person,
						legIterators,
						new MapUtils.Factory<Iterator<Leg>>() {
							@Override
							public Iterator<Leg> create() {
								return TripStructureUtils.getLegs(
									population.getPersons().get( person ).getSelectedPlan() ).iterator();
							}
						});
			final Leg leg = it.next();
			final double tt = time - departure.remove( person );
			leg.setTravelTime( tt );
			leg.getRoute().setTravelTime( tt );
		}
	}
}
