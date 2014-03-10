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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;

/**
 * @author nagel
 *
 */
class MyTripRouterFactory implements TripRouterFactory {

	private MySimulationObserver observer;

	MyTripRouterFactory(MySimulationObserver observer) {
		this.observer = observer ;
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext iterationContext) {
		// My observer is an EventHandler. I can ask it what it thinks the world currently looks like,
		// based on the last observed iteration, and pass that into my routing module to make decisions.
		//
		// Do not plug a routing module itself into the EventsManager! Trip routers are short lived,
		// they are recreated as needed (per iteration, per thread...), and factory methods
		// such as this should normally not have visible side effects (such as plugging something
		// into the EventsManager).
		final MyRoutingModule module = new MyRoutingModule(observer.getIterationData());
		
		TripRouter tr = new TripRouter() ;
		tr.setRoutingModule(TransportMode.car, module) ;
		return tr ;
	}

}
