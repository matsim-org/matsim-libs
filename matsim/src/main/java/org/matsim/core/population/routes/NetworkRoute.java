/* *********************************************************************** *
 * project: org.matsim.*
 * CarRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.population.routes;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.vehicles.Vehicle;


/**
 * A route that describes a path in a (road) network. Thus, the route is described usually
 * as a series of links or nodes.
 *
 * @author mrieser
 */
public interface NetworkRoute extends Route {

	public void setLinkIds(final Id<Link> startLinkId, final List<Id<Link>> linkIds, final Id<Link> endLinkId);

	/**Design thoughts:<ul>
	 * <li> yyyy It this general cost or monetary cost?  kai/benjamin, jun'11
	 * </ul>
	 */
	public void setTravelCost(final double travelCost);

	/**Design thoughts:<ul>
	 * <li> yyyy It this general cost or monetary cost?  kai/benjamin, jun'11
	 * </ul>
	 */
	public double getTravelCost();

	/**
	 * Returns the list of link ids that build the route. The links where the route
	 * starts and ends (the links where the activities are on) are <b>not</b>
	 * included in the list (note that they are in getStartLinkId() and getEndLinkId() of the Route super-interface).
	 * @return a list containing the link ids the agents plans to travel along
	 */
	public List<Id<Link>> getLinkIds();

	/**
	 * This method returns a new Route object with the subroute of this, using fromLinkId as the
	 * subroute's startLink, toLinkId as the subroute's endLink, and the links in between fromLinkId
	 * and toLinkIds set as the route's link
	 * @param fromLinkId
	 * @param toLinkId
	 * @return subroute of this route starting at fromLinkId and ending at toLinkId
	 */
	public NetworkRoute getSubRoute(final Id<Link> fromLinkId, final Id<Link> toLinkId);
	// yyyy my intuition is that this should be removed from the API (and replaced by a static method in RouteUtils). kai, oct'17

	/**
	 * Sets the id of the vehicle that should be used to drive along this route.
	 *
	 * @param vehicleId
	 */
	public abstract void setVehicleId(final Id<Vehicle> vehicleId);

	/**
	 * @return the id of the vehicle that should be used to drive along this route.
	 */
	public abstract Id<Vehicle> getVehicleId();
	// Does it really make sense to couple the vehicle to the route?  I would have coupled it to the leg.  kai, aug'10
	// Well, I guess now it belongs to the route. :-)  kai, aug'10

	@Override
	public NetworkRoute clone();
	// to get the correct interface type.  kai, sep'17

}
