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

package org.matsim.core.mobsim.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.queuesim.interfaces.QueueNetworkFactory;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

/**
 * QueueNetwork is responsible for creating the QueueLinks and QueueNodes.
 *
 * @author david
 * @author mrieser
 * @author dgrether
 */
public class QueueNetwork {

	private final Map<Id, QueueLink> links;

	private final Map<Id, QueueNode> nodes;

	private final Network networkLayer;

	private final QueueNetworkFactory<QueueNode, QueueLink> queueNetworkFactory;

	public QueueNetwork(final Network networkLayer2) { // accessed from elsewhere
		this(networkLayer2, new DefaultQueueNetworkFactory());
	}

	/*package*/ QueueNetwork(final Network networkLayer, final QueueNetworkFactory<QueueNode, QueueLink> factory) {
		this.networkLayer = networkLayer;
		this.queueNetworkFactory = factory;
		this.links = new LinkedHashMap<Id, QueueLink>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		this.nodes = new LinkedHashMap<Id, QueueNode>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		for (Node n : networkLayer.getNodes().values()) {
			this.nodes.put(n.getId(), this.queueNetworkFactory.createQueueNode(n, this));
		}
		for (Link l : networkLayer.getLinks().values()) {
			this.links.put(l.getId(), this.queueNetworkFactory.createQueueLink(l, this, this.nodes.get(l.getToNode().getId())));
		}
		for (QueueNode n : this.nodes.values()) {
			n.init();
		}
	}

	public Network getNetworkLayer() { // accessed from elsewhere
		return this.networkLayer;
	}

	/**
	 * Called whenever this object should dump a snapshot
	 * @return A collection with the current positions of all vehicles.
	 */
	/*package*/ Collection<AgentSnapshotInfo> getVehiclePositions() {
		Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
		for (QueueLink link : this.links.values()) {
			link.getVisData().getVehiclePositions(positions);
		}
		return positions;
	}

	public Map<Id, QueueLink> getLinks() { // accessed from elsewhere but I think "QueueLink" should be replaced by an interface.  kai, may'10
		return Collections.unmodifiableMap(this.links);
	}

	public Map<Id, QueueNode> getNodes() { // accessed from elsewhere but I think "QueueNode" should be replaced by an interface.  kai, may'10
		return Collections.unmodifiableMap(this.nodes);
	}

	public QueueLink getQueueLink(final Id id) { // accessed from elsewhere but I think "QueueLink" should be replaced by an interface.  kai, may'10
		return this.links.get(id);
	}

	/*package*/ QueueNode getQueueNode(final Id id) {
		return this.nodes.get(id);
	}
	
}
