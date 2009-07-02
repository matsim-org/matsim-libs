/* *********************************************************************** *
 * project: org.matsim.*
 * MyQueueNetworkFactory.java
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

package playground.christoph.mobsim;

import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueNetworkFactory;
import org.matsim.core.mobsim.queuesim.QueueNode;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

public class MyQueueNetworkFactory implements QueueNetworkFactory<QueueNode, QueueLink> {

	/**
	 * @see org.matsim.core.mobsim.queuesim.QueueNetworkFactory#newQueueLink(org.matsim.core.network.LinkImpl, org.matsim.core.mobsim.queuesim.QueueNetwork)
	 */
	public QueueLink newQueueLink(LinkImpl link, QueueNetwork queueNetwork, QueueNode toQueueNode) {
		return new QueueLink(link, queueNetwork, toQueueNode);
		//return new MyQueueLink(link, queueNetwork, toQueueNode);
	}

	/**
	 * @see org.matsim.core.mobsim.queuesim.QueueNetworkFactory#newQueueNode(org.matsim.core.network.NodeImpl, org.matsim.core.mobsim.queuesim.QueueNetwork)
	 */
	public MyQueueNode newQueueNode(NodeImpl node, QueueNetwork queueNetwork) {
		return new MyQueueNode(node, queueNetwork);
	}

}