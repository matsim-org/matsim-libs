/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008, 2011 by the members listed in the COPYING,  *
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

package org.matsim.core.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

/**
 * @author dgrether
 * @author mrieser
 */
public class NetworkFactoryImpl implements NetworkFactory {

	private final static Logger log = Logger.getLogger(NetworkFactoryImpl.class);

	private LinkFactory linkFactory = null;
	

	private NetworkChangeEventFactory networkChangeEventFactory = new NetworkChangeEventFactoryImpl();

	private final Network network;

	public NetworkFactoryImpl(final Network network) {
		this.network = network;
		this.linkFactory = new LinkFactoryImpl();
	}

	@Override
	public NodeImpl createNode(final Id<Node> id, final Coord coord) {
		NodeImpl node = new NodeImpl(id);
		node.setCoord(coord) ;
		return node ;
	}

	@Override
	public Link createLink(Id<Link> id, Node fromNode, Node toNode) {
		return this.linkFactory.createLink(id, fromNode, toNode, this.network, 1.0, 1.0, 1.0, 1.0);
	}

	public Link createLink(final Id<Link> id, final Node from, final Node to,
			final NetworkImpl network, final double length, final double freespeedTT, final double capacity,
			final double lanes) {
		return this.linkFactory.createLink(id, from, to, network, length, freespeedTT, capacity, lanes);
	}


	/**
	 * @param time the time when the NetworkChangeEvent occurs
	 * @return a new NetworkChangeEvent
	 *
	 * @see #setNetworkChangeEventFactory(NetworkChangeEventFactory)
	 */
	public NetworkChangeEvent createNetworkChangeEvent(double time) {
		return this.networkChangeEventFactory.createNetworkChangeEvent(time);
	}
	
	public void setLinkFactory(final LinkFactory factory) {
		this.linkFactory = factory;
	}

	public void setNetworkChangeEventFactory(NetworkChangeEventFactory networkChangeEventFactory) {
		this.networkChangeEventFactory = networkChangeEventFactory;
	}
}
