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


/**
 * A route that describes a path in a (road) network. Thus, the route is described usually
 * as a series of links or nodes.
 *
 * @author mrieser
 */
public interface NetworkRoute extends RouteWRefs {

	public void setLinkIds(final Id startLinkId, final List<Id> linkIds, final Id endLinkId);

	public void setTravelCost(final double travelCost);

	public double getTravelCost();

	/**
	 * Returns the list of link ids that build the route. The links where the route
	 * starts and ends (the links where the activities are on) are <b>not</b>
	 * included in the list.
	 * @return a list containing the link ids the agents plans to travel along
	 */
	public List<Id> getLinkIds();

	/**
	 * This method returns a new Route object with the subroute of this, using fromLinkId as the
	 * subroute's startLink, toLinkId as the subroute's endLink, and the links in between fromLinkId
	 * and toLinkIds set as the route's link
	 * @param fromLinkId
	 * @param toLinkId
	 * @return subroute of this route starting at fromLinkId and ending at toLinkId
	 */
	public NetworkRoute getSubRoute(final Id fromLinkId, final Id toLinkId);

	/**
	 * Sets the id of the vehicle that should be used to drive along this route.
	 *
	 * @param vehicleId
	 */
	public abstract void setVehicleId(final Id vehicleId);

	/**
	 * @return the id of the vehicle that should be used to drive along this route.
	 */
	public abstract Id getVehicleId();
}
