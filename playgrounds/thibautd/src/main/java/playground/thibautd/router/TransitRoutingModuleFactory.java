/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRoutingModuleFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.router;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.pt.router.TransitRouterFactory;

/**
 * Sets the handler for PT so has to use detailed transit simulation.
 * No other mode is set here!
 *
 * @author thibautd
 */
public class TransitRoutingModuleFactory implements RoutingModuleFactory {
	private final TransitRouterFactory transitRouterFactory;

	/**
	 * Initialises a builder.
	 * @param transitRouterFactory the factory to use to initialise hanlders
	 */
	public TransitRoutingModuleFactory(
			final TransitRouterFactory transitRouterFactory) {
		this.transitRouterFactory = transitRouterFactory;
	}

	@Override
	public RoutingModule createModule(
			final String mainMode,
			final TripRouterFactory factory) {
		return new TransitRouterWrapper(
				transitRouterFactory.createTransitRouter(),
				// use a walk router in case no PT path is found
				factory.getRoutingModuleFactories().get(
					TransportMode.transit_walk ).createModule(
						TransportMode.transit_walk,
						factory ));
	}
}

