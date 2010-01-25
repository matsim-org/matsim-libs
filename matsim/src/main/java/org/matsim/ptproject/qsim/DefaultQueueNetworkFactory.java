/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;


/**
 * @author dgrether
 */
/*package*/ final class DefaultQueueNetworkFactory implements QNetworkFactory<QNode, QLink> {

	public QLink newQueueLink(final Link link, final QNetwork queueNetwork, final QNode toQueueNode) {
		return new QLinkImpl(link, queueNetwork, toQueueNode);
	}

	/**
	 * @see org.matsim.core.mobsim.queuesim.QueueNetworkFactory#newQueueNode(org.matsim.core.network.NodeImpl, org.matsim.core.mobsim.queuesim.QueueNetwork)
	 */
	public QNode newQueueNode(final Node node, final QNetwork queueNetwork) {
		return new QNode(node, queueNetwork);
	}

}
