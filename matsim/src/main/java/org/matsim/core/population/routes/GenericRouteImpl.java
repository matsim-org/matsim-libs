/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouteImpl.java
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

package org.matsim.core.population.routes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


public final class GenericRouteImpl extends AbstractRoute {

	/*package*/ final static String ROUTE_TYPE = "generic";
	
	private String routeDescription = null;

	public GenericRouteImpl(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}

	@Override
	public GenericRouteImpl clone() {
		return (GenericRouteImpl) super.clone();
	}

	@Override
	public String getRouteDescription() {
		return this.routeDescription;
	}

	@Override
	public void setRouteDescription(final String routeDescription) {
		this.routeDescription = routeDescription;
	}

	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}

}
