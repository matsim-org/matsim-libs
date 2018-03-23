/* *********************************************************************** *
 * project: org.matsim.*
 * QueueNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNode;
import org.matsim.vis.snapshotwriters.VisLink;

/**
 *
 * @author david
 * @author mrieser
 * @author dgrether
 */

public class QNetwork implements NetsimNetwork {

	private final Map<Id<Link>, QLinkI> links;

	private final Map<Id<Node>, QNodeI> nodes;

	private final Network network;

	QNetwork(final Network network, final QNetworkFactory netsimNetworkFactory, InternalInterface internalInterface ) {
		this.network = network;
		this.links = new LinkedHashMap<>((int)(network.getLinks().size()*1.1), 0.95f);
		this.nodes = new LinkedHashMap<>((int)(network.getLinks().size()*1.1), 0.95f);

		//netsimNetworkFactory.initializeFactory();
		for (Node n : network.getNodes().values()) {
			this.nodes.put(n.getId(), netsimNetworkFactory.createNetsimNode(n, internalInterface));
		}
		for (Link l : network.getLinks().values()) {
			final QLinkI qlink = netsimNetworkFactory.createNetsimLink(l, this.nodes.get(l.getToNode().getId()), internalInterface);
			this.links.put(l.getId(), qlink);
		}
		for (QNodeI n : this.nodes.values()) {
			n.init(this);
		}
	}
	
	@Override
	public Network getNetwork() {
		return this.network;
	}

	@Override
	public Map<Id<Link>, QLinkI> getNetsimLinks() {
		return Collections.unmodifiableMap(this.links);
	}

	@Override
	public Map<Id<Link>, ? extends VisLink> getVisLinks() {
		return Collections.unmodifiableMap(this.links);
	}

	@Override
	public Map<Id<Node>, QNodeI> getNetsimNodes() {
		return Collections.unmodifiableMap(this.nodes);
	}

	@Override
	public QLinkI getNetsimLink(final Id<Link> id) {
		return this.links.get(id);
	}

	@Override
	public NetsimNode getNetsimNode(final Id<Node> id) {
		return this.nodes.get(id);
	}


}
