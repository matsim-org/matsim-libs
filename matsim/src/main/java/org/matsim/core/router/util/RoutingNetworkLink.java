/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingNetworkLink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.router.util;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface RoutingNetworkLink extends Link {

	public Link getLink();
	
	@Override
	public Id<Link> getId();
	
	@Override
	public RoutingNetworkNode getFromNode();
	
	@Override
	public RoutingNetworkNode getToNode();
}