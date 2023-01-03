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
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.framework.MobsimTimer;
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

	private final IdMap<Link, QLinkI> links;

	private final IdMap<Node, QNodeI> nodes;

	private final Network network;

	private final QNetworkFactory queueNetworkFactory;

	QNetsimEngineI simEngine; // only for tests...

	QNetwork(final Network network, final QNetworkFactory netsimNetworkFactory ) {
		this.network = network;
		this.queueNetworkFactory = netsimNetworkFactory;
		this.links = new IdMap<>(Link.class);
		this.nodes = new IdMap<>(Node.class);
	}

	public void initialize(QNetsimEngineI simEngine, AgentCounter agentCounter, MobsimTimer simTimer) {
		this.simEngine = simEngine;
		this.queueNetworkFactory.initializeFactory( agentCounter, simTimer, simEngine.getNetsimInternalInterface());
		for (Node n : this.network.getNodes().values()) {
			this.nodes.put(n.getId(), this.queueNetworkFactory.createNetsimNode(n));
		}
		for (Link l : this.network.getLinks().values()) {
			final QLinkI qlink = this.queueNetworkFactory.createNetsimLink(l, this.nodes.get(l.getToNode().getId()));
			this.links.put(l.getId(), qlink);
		}
		for (QNodeI n : this.nodes.values()) {
			n.init();
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
