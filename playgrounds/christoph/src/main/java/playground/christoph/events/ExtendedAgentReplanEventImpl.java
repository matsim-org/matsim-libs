/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.christoph.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEventImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;

/**
 * @author cdobler
 */
public class ExtendedAgentReplanEventImpl extends PersonEventImpl {

	public static final String EVENT_TYPE = "extendedreplan";

	private final NetworkRouteWRefs replannedRoute;
	private final NetworkRouteWRefs originalRoute;
	
	public ExtendedAgentReplanEventImpl(final double time, final Id agentId, final NetworkRouteWRefs alternativeRoute, final NetworkRouteWRefs originalRoute)
	{
		super(time, agentId);
		this.replannedRoute = alternativeRoute;
		this.originalRoute = originalRoute;
	}

	@Override
	public String getEventType()
	{
		return EVENT_TYPE;
	}

	public NetworkRouteWRefs getReplannedRoute()
	{
		return this.replannedRoute;
	}

	public NetworkRouteWRefs getOriginalRoute()
	{
		return this.originalRoute;
	}
}
