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
import org.matsim.contrib.hybridsim.simulation.ExternalEngine;


public class HybridQSimExternalNetworkFactory implements
		NetsimNetworkFactory {

	private final ExternalEngine e;

	public HybridQSimExternalNetworkFactory(ExternalEngine e) {
		this.e = e;
	}

	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network);
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNetwork network,
			QNode queueNode) {
		if (link.getAllowedModes().contains("2ext")) {
			return new QSimExternalTransitionLink(link, network, this.e);
		}
		QLinkImpl ret = new QLinkImpl(link, network, queueNode);
		if (link.getAllowedModes().contains("ext2")) {
			this.e.registerAdapter(new QLinkInternalIAdapter(ret));
		}
		return ret;
	}

}
