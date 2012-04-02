/* *********************************************************************** *
 * project: org.matsim.*
 * QLanesNetworkFactory
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.lanes.data.v20.LaneDefinitions20;


public class QLanesNetworkFactory implements NetsimNetworkFactory<QNode, QLinkInternalI> {

	private NetsimNetworkFactory<QNode, QLinkInternalI> delegate;
	private LaneDefinitions20 laneDefinitions;

	public QLanesNetworkFactory(NetsimNetworkFactory<QNode, QLinkInternalI> delegate, LaneDefinitions20 laneDefintions){
		this.delegate = delegate;
		this.laneDefinitions = laneDefintions;
	}

	@Override
	public QLinkInternalI createNetsimLink(Link link, QNetwork network, QNode queueNode) {
		QLinkInternalI ql = null;
		if (this.laneDefinitions.getLanesToLinkAssignments().containsKey(link.getId())){
			ql = new QLinkLanesImpl(link, network, queueNode, this.laneDefinitions.getLanesToLinkAssignments().get(link.getId()));
		}
		else {
			ql = this.delegate.createNetsimLink(link, network, queueNode);
		}
		return ql;
	}

	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return this.delegate.createNetsimNode(node, network);
	}

}
