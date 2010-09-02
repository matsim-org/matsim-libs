/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalQNetworkFactory.java
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

package playground.christoph.multimodal.mobsim.netsimengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.ptproject.qsim.interfaces.QNetworkFactory;
import org.matsim.ptproject.qsim.netsimengine.QLinkInternalI;
import org.matsim.ptproject.qsim.netsimengine.QNode;
import org.matsim.ptproject.qsim.netsimengine.QSimEngine;

import playground.christoph.multimodal.router.costcalculator.MultiModalTravelTime;

public class MultiModalQNetworkFactory implements QNetworkFactory<QNode, QLinkInternalI> {

	private MultiModalTravelTime travelTime;
	
	public MultiModalQNetworkFactory(MultiModalTravelTime travelTime) {
		this.travelTime = travelTime;
	}
	
	public QLinkInternalI createQueueLink(final Link link, final QSimEngine simEngine, final QNode toQueueNode) {
		return new MultiModalQLinkImpl(link, simEngine, toQueueNode, travelTime);
	}

	public QNode createQueueNode(final Node node, QSimEngine simEngine) {
		return new MultiModalQNode(node, simEngine);
	}
}