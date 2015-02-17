/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import playground.gregor.external.ExternalEngine;

public class HybridQSimExternalNetworkFactory implements
		NetsimNetworkFactory<QNode, QLinkInternalI> {

	private ExternalEngine e;

	public HybridQSimExternalNetworkFactory(ExternalEngine e) {
		this.e = e;
	}

	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network);
	}

	@Override
	public QLinkInternalI createNetsimLink(Link link, QNetwork network,
			QNode queueNode) {
		if (link.getAllowedModes().contains("qext")) {
			return new QSimExternalTransitionLink(link, network, e);
		}
		QLinkImpl ret = new QLinkImpl(link, network, queueNode);
		if (link.getAllowedModes().contains("extq")) {
			e.registerAdapter(new External2QAdapterLink(ret));
		}
		return ret;
	}

}
