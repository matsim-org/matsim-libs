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

/**
 * 
 */
package tutorial.programming.example12PluggableTripRouter;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;

/**
 * @author nagel
 *
 */
class MyTripRouterFactory implements TripRouterFactory {

	private final EventsManager events;

	MyTripRouterFactory(EventsManager events) {
		this.events = events ;
	}

	@Override
	public TripRouter createTripRouter() {
		final MyRoutingModule module = new MyRoutingModule();
		
		// my own router could listen to events:
		events.addHandler(module) ;
		// (this is a very simple design; one may want to separate the tasks of the observer from the tasks of the router)

		TripRouter tr = new TripRouter() ;
		tr.setRoutingModule(TransportMode.car, module ) ;
		return tr ;
	}

}
