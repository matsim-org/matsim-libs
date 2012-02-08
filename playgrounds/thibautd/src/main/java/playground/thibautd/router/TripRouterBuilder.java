/* *********************************************************************** *
 * project: org.matsim.*
 * TripRouterBuilder.java
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

/**
 * Defines object meant to "build" a trip router before it is used.
 * See javadoc of individual methods for details.
 *
 * @author thibautd
 */
public interface TripRouterBuilder {

	/**
	 * Sets the mode handlers, using the method {@link TripRouter#setModeHandler(String, RoutingModeHandler)}.
	 * The elements with which to initialize the {@link RoutingModeHandler}s should
	 * be obtained from the factories provided by the argument {@link TripRouterFactory}.
	 *
	 * @param tripRouter the instance for which mode handlers are to set.
	 */
	public void setModeHandlers(final TripRouterFactory factory, final TripRouter tripRouter);
}

