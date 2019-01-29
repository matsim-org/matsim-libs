/* *********************************************************************** *
 * project: org.matsim.*
 * PointerRoutingNetworkLink.java
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

package org.matsim.core.router.util;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.priorityqueue.HasIndex;

public class ArrayRoutingNetworkLink extends AbstractRoutingNetworkLink implements HasIndex {

	final int arrayIndex;
	
	public ArrayRoutingNetworkLink(Link link, RoutingNetworkNode fromNode, RoutingNetworkNode toNode, int arrayIndex) {
		super(link, fromNode, toNode);
		this.arrayIndex = arrayIndex;
	}
	
	@Override
	public int getArrayIndex() {
		return this.arrayIndex;
	}
}