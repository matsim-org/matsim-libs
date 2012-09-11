/* *********************************************************************** *
 * project: org.matsim.*
 * TripRouterFactory.java
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

import org.matsim.core.api.internal.MatsimFactory;

/**
 * Creates configured {@link TripRouter} instances.
 * This interface must be implemented to implement a custom routing behaviour.
 * @author thibautd
 */
public interface TripRouterFactory extends MatsimFactory {
	/**
	 * Creates a new {@link TripRouter} instance, using the registered
	 * {@link RoutingModuleFactory}es.
	 * @return a fully initialised {@link TripRouter}.
	 */
	public TripRouter createTripRouter();
}

