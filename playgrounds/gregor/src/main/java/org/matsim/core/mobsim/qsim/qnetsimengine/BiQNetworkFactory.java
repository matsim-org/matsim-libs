/* *********************************************************************** *
 * project: org.matsim.*
 * BiQNetworkFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class BiQNetworkFactory implements NetsimNetworkFactory<QNode, QLinkInternalI>{
	
	private final Map<Id,BiPedQ> qs = new HashMap<Id,BiPedQ>();

	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network);
	}

	@Override
	public QLinkInternalI createNetsimLink(Link link, QNetwork network,
			QNode toQueueNode) {
		
		BiPedQ q = new BiPedQ(network.simEngine.getMobsim().getSimTimer(),0.1);
		Iterator<? extends Link> it = link.getToNode().getOutLinks().values().iterator();
		while (it.hasNext()) {
			Link rev = it.next();
			if (rev.getToNode().equals(link.getFromNode())) {
				BiPedQ revQ = this.qs.remove(rev.getId());
				if (revQ != null) {
					revQ.setRevQ(q);
					q.setRevQ(revQ);
				} else {
					this.qs.put(link.getId(), q);
				}
				break;
			}
		}
		BiPedQLinkImpl ret = new BiPedQLinkImpl(link, network, toQueueNode, q);
		return ret;
	}

}
