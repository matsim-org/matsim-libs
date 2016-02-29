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
import org.matsim.lanes.ModelLane;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.lanes.LanesUtils;

import java.util.List;


class QLanesNetworkFactory implements NetsimNetworkFactory {

	private final NetsimNetworkFactory delegate;
	private final Lanes laneDefinitions;

	public QLanesNetworkFactory(NetsimNetworkFactory delegate, Lanes laneDefintions){
		this.delegate = delegate;
		this.laneDefinitions = laneDefintions;
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNetwork network, QNode queueNode) {
		QLinkI ql = null;
		LanesToLinkAssignment20 l2l = this.laneDefinitions.getLanesToLinkAssignments().get(link.getId());
		if (l2l != null){
			List<ModelLane> lanes = LanesUtils.createLanes(link, l2l);
			ql = new QLinkLanesImpl(link, network, queueNode, lanes);
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
