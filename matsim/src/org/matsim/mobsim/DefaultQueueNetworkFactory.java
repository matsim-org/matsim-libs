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
package org.matsim.mobsim;

import org.matsim.network.Link;
import org.matsim.network.Node;


/**
 * @author dgrether
 *
 */
public final class DefaultQueueNetworkFactory implements QueueNetworkFactory<QueueNode, QueueLink> {

	/**
	 * @see org.matsim.mobsim.QueueNetworkFactory#newQueueLink(org.matsim.network.Link, org.matsim.mobsim.QueueNetworkLayer)
	 */
	public QueueLink newQueueLink(Link link, QueueNetworkLayer queueNetwork, QueueNode toQueueNode) {
		return new QueueLink(link, queueNetwork, toQueueNode);
	}

	/**
	 * @see org.matsim.mobsim.QueueNetworkFactory#newQueueNode(org.matsim.network.Node, org.matsim.mobsim.QueueNetworkLayer)
	 */
	public QueueNode newQueueNode(Node node, QueueNetworkLayer queueNetwork) {
		return new QueueNode(node, queueNetwork);
	}

}
