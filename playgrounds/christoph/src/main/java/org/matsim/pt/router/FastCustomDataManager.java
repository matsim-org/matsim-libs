/* *********************************************************************** *
 * project: org.matsim.*
 * FastCustomDataManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.pt.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.priorityqueue.HasIndex;
import org.matsim.core.router.util.ArrayRoutingNetworkNode;

/**
 * A helper class to store custom data for {@link TransitTravelDisutility} which can be used
 * to e.g. keep track of paid fares during the routing process.
 * The stored data will be invalidated for each new routing request.
 * 
 * This class extends CustomDataManager since there is no interface that it could implement.
 * 
 * @author cdobler / mrieser / senozon
 */
public class FastCustomDataManager extends CustomDataManager {

	private Object[] data;
	private final int numNodes;
	private Node fromNode = null;
	private Node toNode = null;

	private Object tmpToNodeData = null;

	public FastCustomDataManager(Network network) {
		this.numNodes = network.getNodes().size();
	}
	
	public void setToNodeCustomData(final Object data) {
		this.tmpToNodeData = data;
	}
	
	/**
	 * @param node
	 * @return the stored data for the given node, or <code>null</code> if there is no data stored yet.
	 */
	public Object getFromNodeCustomData() {
		int index = ((HasIndex) this.fromNode).getArrayIndex();
		return this.data[index];
	}

	/*package*/ void initForLink(final Link link) {
		this.fromNode = link.getFromNode();
		this.toNode = link.getToNode();
		this.tmpToNodeData = null;
	}
	
	/*package*/ void storeTmpData() {
		if (this.tmpToNodeData != null) {
			int index = ((ArrayRoutingNetworkNode) this.toNode).getArrayIndex();
			this.data[index] = this.tmpToNodeData;
		}
	}
	
	/*package*/ void reset() {
		this.data = new Object[numNodes];
		this.fromNode = null;
		this.toNode = null;
		this.tmpToNodeData = null;
	}

}
