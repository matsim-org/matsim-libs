/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.api.core.v01.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimPopulationObject;

/**
 * @author nagel
 *
 */
public interface Route extends MatsimPopulationObject {

	public double getDistance();

	public void setDistance(final double distance);

	public double getTravelTime();

	public void setTravelTime(final double travelTime);

	public Id<Link> getStartLinkId();

	public Id<Link> getEndLinkId();

	public void setStartLinkId(final Id<Link> linkId);

	public void setEndLinkId(final Id<Link> linkId);

	/**
	 * @return a serialization of this routes state as a String. Used to write the route to files.
	 */
	public String getRouteDescription();
	
	/**
	 * Sets the state of the route based on it's description
	 * 
	 * @param routeDescription
	 */
	public void setRouteDescription(final String routeDescription);

	/**
	 * @return an identifier describing the type of this route uniquely. Used when writing the route to files.
	 */
	public String getRouteType();
	
	/** make the clone method public, but do NOT implement Cloneable
	 * so that implementations can decide on their own if they support
	 * clone() or not.
	 * <p/>
	 * Design comments:<ul>
	 * <li>Do we really want this?  Martin ("Clean code") argues for the difference between data objects and behavioral objects.  Data objects should
	 * only be accessed via the interface methods.  I think that "route" is a data object.  In consequence, "copy" and/or "deepCopy" should, in 
	 * my view, be static methods. (The argument against this is, I guess, that one might want to add Route implementations that are not
	 * part of the standard.  Yet given that we want to be able to read/write them in xml, I am not sure how far this carries.)  kai, jan'13
	 * </ul>
	 */
	public Route clone();

}
