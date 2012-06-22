/* *********************************************************************** *
 * project: org.matsim.*
 * PointerRoutingNetworkNode.java
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

import org.matsim.api.core.v01.network.Node;

public class ArrayRoutingNetworkNode extends AbstractRoutingNetworkNode {

	final int arrayIndex;
	
	/*package*/ ArrayRoutingNetworkNode(Node node, int numOutLinks, int arrayIndex) {
		super(node, numOutLinks);
		this.arrayIndex = arrayIndex;
	}
	
	public int getArrayIndex() {
		return this.arrayIndex;
	}
}