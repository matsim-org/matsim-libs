/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.mrieser.core.mobsim.network.api.VisLink;
import playground.mrieser.core.mobsim.network.api.VisNetwork;
import playground.mrieser.core.mobsim.network.api.VisNode;

public class VisNetworkImpl implements VisNetwork {

	private final Map<Id, VisLinkImpl> links;
	private final Map<Id, VisNodeImpl> nodes;

	public VisNetworkImpl(final QueueNetwork qnet) {
		this.links = new HashMap<Id, VisLinkImpl>((int) (qnet.getLinks().size() * 1.5));
		this.nodes = new HashMap<Id, VisNodeImpl>((int) (qnet.getNodes().size() * 1.5));

		for (QueueLink link : qnet.getLinks().values()) {
			this.links.put(link.getId(), new VisLinkImpl(link));
		}
		for (QueueNode node : qnet.getNodes().values()) {
			this.nodes.put(node.getId(), new VisNodeImpl());
		}
	}

	@Override
	public Map<Id, ? extends VisLink> getLinks() {
		return this.links;
	}

	@Override
	public Map<Id, ? extends VisNode> getNodes() {
		return this.nodes;
	}

}
