/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingModuleFactory.java
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
 * Creates a routing module.
 * @author thibautd
 */
public interface RoutingModuleFactory {
	/**
	 * Creates a new instance.
	 * @param mainMode the main mode of the trips to handle
	 * @param factory the {@link TripRouterFactory} from which pertinent
	 * elements/estimators can be obtained
	 * @return a new instance
	 */
	public RoutingModule createModule(String mainMode, TripRouterFactory factory);
}

