/* *********************************************************************** *
 * project: org.matsim.*
 * TransitLine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule.api;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

/**
 * Description of a single transit line. Can have multiple routes (e.g. from A to B and from B to A).
 * 
 * @author mrieser
 */
public interface TransitLine extends Identifiable<TransitLine> {

	/**
	 * Adds the specified transit route to this line.
	 * 
	 * @param transitRoute
	 * @throws IllegalArgumentException if there is already a route with the same Id
	 */
	public abstract void addRoute(final TransitRoute transitRoute);

	
	/**
	 * @return immutable Map containing all transit routes assigned to this transit line
	 */
	public abstract Map<Id<TransitRoute>, TransitRoute> getRoutes();

	/**
	 * Removes the specified transit route from this transit line.
	 * @param route
	 * @return <code>true</code> if this line contained the specified route.
	 */
	public abstract boolean removeRoute(final TransitRoute route);

	public void setName(final String name);
	
	public String getName();
	
}