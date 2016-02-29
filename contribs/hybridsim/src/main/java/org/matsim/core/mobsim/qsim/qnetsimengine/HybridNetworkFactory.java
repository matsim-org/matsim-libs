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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class HybridNetworkFactory implements
		NetsimNetworkFactory {

	private final Map<String, NetsimNetworkFactory> facs = new LinkedHashMap<>();
	
	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network);
	}

	@Override
	public QLinkInternalI createNetsimLink(Link link, QNetwork network,
			QNode queueNode) {
		
		for (Entry<String, NetsimNetworkFactory> e : this.facs.entrySet()) {
			if (link.getAllowedModes().contains(e.getKey())) {
				return e.getValue().createNetsimLink(link, network, queueNode);
			}
		}
		//default QLink
		return new QLinkImpl(link, network, queueNode);
	}
	
	public void putNetsimNetworkFactory(String key, NetsimNetworkFactory fac) {
		this.facs.put(key, fac);
	}

}
