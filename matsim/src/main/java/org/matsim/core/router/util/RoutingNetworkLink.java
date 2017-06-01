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
	// yy In most of matsim we have the convention that the wrapped object is provided, but the methods are not delegated.  I.e.
	//    routingNetworkLink.getLink().getFreeSpeed() ;
	// but NOT
	//    routingNetworkLink.getFreeSpeed() ;
	// The current approach seems to be doing both.  Why did it add the second way of doing things?  
	// (I can imagine that this came from retrofitting, but a design choice explanation would still be helpful.)  kai, may'17

	public Link getLink();
	
	@Override
	public Id<Link> getId();
	
	@Override
	public RoutingNetworkNode getFromNode();
	
	@Override
	public RoutingNetworkNode getToNode();
}