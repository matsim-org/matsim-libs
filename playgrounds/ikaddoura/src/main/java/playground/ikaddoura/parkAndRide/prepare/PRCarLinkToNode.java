/* *********************************************************************** *
 * project: org.matsim.*
 * PRCarLinkToNode.java
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

/**
 * 
 */
package playground.ikaddoura.parkAndRide.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

/**
 * @author Ihab
 *
 */
public class PRCarLinkToNode {
	
	private Id nodeId;
	private Node node;
	private String stopName;
	
	public Id getNodeId() {
		return nodeId;
	}
	public void setNodeId(Id nodeId) {
		this.nodeId = nodeId;
	}
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
	}
	public String getStopName() {
		return stopName;
	}
	public void setStopName(String stopName) {
		this.stopName = stopName;
	}

}
