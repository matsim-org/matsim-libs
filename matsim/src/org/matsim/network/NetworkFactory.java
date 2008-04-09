/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.network;

import org.matsim.utils.identifiers.IdI;


/**
 * @author dgrether
 *
 */
public class NetworkFactory {

	private final NetworkLayer networkLayer;


	public NetworkFactory(NetworkLayer networkLayer) {
		this.networkLayer = networkLayer;
	}

	public Node newNode(final String id, final String x, final String y, final String type) {
		return new Node(id, x, y, type);
	}


	protected Link newLink(final IdI id, Node from, Node to, double length, double freespeed, double capacity, int permlanes) {
		Link ret = new LinkImpl(this.networkLayer, id, from, to, length, freespeed, capacity, permlanes);
		this.networkLayer.addLink(ret);
		return ret;
	}

	public Link newLink(String id, Node from_node, Node to_node, String length,
			String freespeed, String capacity, String permlanes,  final String origid, final String type) {
		Link ret = new LinkImpl(this.networkLayer, id, from_node, to_node, length, freespeed, capacity, permlanes, origid, type);
		this.networkLayer.addLink(ret);
		return ret;
	}



}
