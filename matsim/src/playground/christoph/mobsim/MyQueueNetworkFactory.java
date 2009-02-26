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

import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNetworkFactory;
import org.matsim.mobsim.queuesim.QueueNode;

public class MyQueueNetworkFactory implements QueueNetworkFactory<QueueNode, QueueLink> {

	/**
	 * @see org.matsim.mobsim.queuesim.QueueNetworkFactory#newQueueLink(org.matsim.interfaces.core.v01.Link, org.matsim.mobsim.queuesim.QueueNetwork)
	 */
	public QueueLink newQueueLink(Link link, QueueNetwork queueNetwork, QueueNode toQueueNode) {
		return new QueueLink(link, queueNetwork, toQueueNode);
		//return new MyQueueLink(link, queueNetwork, toQueueNode);
	}

	/**
	 * @see org.matsim.mobsim.queuesim.QueueNetworkFactory#newQueueNode(org.matsim.interfaces.core.v01.Node, org.matsim.mobsim.queuesim.QueueNetwork)
	 */
	public MyQueueNode newQueueNode(Node node, QueueNetwork queueNetwork) {
		return new MyQueueNode(node, queueNetwork);
	}

}