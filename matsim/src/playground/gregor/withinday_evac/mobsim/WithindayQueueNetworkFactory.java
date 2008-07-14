/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayQueueNetworkFactory.java
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

package playground.gregor.withinday_evac.mobsim;

import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkFactory;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.network.Link;
import org.matsim.network.Node;

public class WithindayQueueNetworkFactory implements QueueNetworkFactory<QueueNode, QueueLink>{

	public QueueLink newQueueLink(final Link link, final QueueNetworkLayer queueNetwork,
			final QueueNode toQueueNode) {
		return new QueueLink(link, queueNetwork, toQueueNode);
	}

	public QueueNode newQueueNode(final Node node, final QueueNetworkLayer queueNetwork) {
		// TODO Auto-generated method stub
		return  new WithindayQueueNode(node, queueNetwork);
	}

}
