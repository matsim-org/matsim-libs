/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.pt.router;

import java.util.HashMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/**
 * A helper class to store custom data for {@link TransitTravelDisutility} which can be used
 * to e.g. keep track of paid fares during the routing process.
 * The stored data will be invalidated for each new routing request.
 * 
 * @author mrieser / senozon
 */
public class CustomDataManager {

	private final HashMap<Node, Object> data = new HashMap<Node, Object>();
	private Node fromNode = null;
	private Node toNode = null;

	private Object tmpToNodeData = null;
	
	public void setToNodeCustomData(final Object data) {
		this.tmpToNodeData = data;
	}

	/**
	 * @param node
	 * @return the stored data for the given node, or <code>null</code> if there is no data stored yet.
	 */
	public Object getFromNodeCustomData() {
		return this.data.get(this.fromNode);
	}

	public void initForLink(final Link link) {
		this.fromNode = link.getFromNode();
		this.toNode = link.getToNode();
		this.tmpToNodeData = null;
	}
	
	public void storeTmpData() {
		if (this.tmpToNodeData != null) {
			this.data.put(this.toNode, this.tmpToNodeData);
		}
	}
	
	public void reset() {
		this.data.clear();
		this.fromNode = null;
		this.toNode = null;
		this.tmpToNodeData = null;
	}

}
