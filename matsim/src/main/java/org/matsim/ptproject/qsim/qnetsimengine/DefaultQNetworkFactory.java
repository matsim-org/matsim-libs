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

package org.matsim.ptproject.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.NetsimNetworkFactory;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;
import org.matsim.ptproject.qsim.interfaces.NetsimEngine;


/**
 * @author dgrether
 */
public final class DefaultQNetworkFactory implements NetsimNetworkFactory<QNode, QLinkInternalI> {

	public QLinkInternalI createNetsimLink(final Link link, final NetsimEngine simEngine, final QNode toQueueNode) {
		return new QLinkImpl(link, simEngine, toQueueNode);
	}

	/**
	 * @see org.matsim.core.mobsim.queuesim.QueueNetworkFactory#createNetsimNode(org.matsim.core.network.NodeImpl, org.matsim.core.mobsim.queuesim.QueueNetwork)
	 */
	public QNode createNetsimNode(final Node node, NetsimEngine simEngine) {
		return new QNode(node, simEngine);
	}

	public static NetsimNetwork createQNetwork(QSim qs, NetsimNetworkFactory<QNode, QLinkInternalI> factory) {
		return new QNetwork(qs, factory);
	}

	public static NetsimNetwork createQNetwork(QSim qs) {
		return new QNetwork(qs);
	}

}
