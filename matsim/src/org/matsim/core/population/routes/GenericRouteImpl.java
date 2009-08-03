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

import org.matsim.api.core.v01.network.Link;


public class GenericRouteImpl extends AbstractRoute implements GenericRoute {

	private String routeDescription = null;
	
	public GenericRouteImpl(final Link startLink, final Link endLink	) {
		super(startLink, endLink);
	}

	public String getRouteDescription() {
		return this.routeDescription;
	}

	public void setRouteDescription(Link startLink, String routeDescription, Link endLink) {
		setStartLink(startLink);
		this.routeDescription = routeDescription;
		setEndLink(endLink);
	}

}
