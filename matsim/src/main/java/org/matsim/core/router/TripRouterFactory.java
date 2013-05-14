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
package org.matsim.core.router;

import org.matsim.core.api.internal.MatsimFactory;

/**
 * Creates configured {@link TripRouter} instances.
 * This interface must be implemented to implement a custom routing behaviour.
 * @author thibautd
 */
public interface TripRouterFactory extends MatsimFactory {
	/**
	 * Creates a new {@link TripRouter} instance.
	 * <p/>
	 * This method is not the usual createXxx(...) method to draw attention to the fact that it does not return an interface but a class.  The syntax is roughly
	 * <pre>
	 *   public TripRouter instantiateAndConfigureTripRouter() {
	 *      TripRouter tr = new TripRouter(...) ;
	 *      tr.setRoutingModule( modeString, routingModule ) ;
	 *      tr....(...) ;
	 *      return tr ;
	 *   }
	 * </pre>
	 * The actual router is set by routingModule of type {@link RoutingModule}; it is responsible for the leg mode described by modeString.
	 * <p/>
	 * Also see <code> tutorial.programming.example12PluggableTripRouter </code> 
	 * and <code> tutorial.programming.example13MultiStateTripRouting </code>.
	 * 
	 * @return a fully initialised {@link TripRouter}.
	 */
	public TripRouter instantiateAndConfigureTripRouter();
}

