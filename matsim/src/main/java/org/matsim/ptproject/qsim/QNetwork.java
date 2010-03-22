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

package org.matsim.ptproject.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

/**
 * QueueNetwork is responsible for creating the QueueLinks and QueueNodes.
 *
 * @author david
 * @author mrieser
 * @author dgrether
 */
public class QNetwork {

	private final Map<Id, QLink> links;

	private final Map<Id, QNode> nodes;

	private final Network networkLayer;

	private final QNetworkFactory<QNode, QLink> queueNetworkFactory;

	public QNetwork(final QSim qs) {
		this( qs, qs.getScenario().getNetwork(), new DefaultQueueNetworkFactory() ) ;
	}

	/**
	 * this constructor is deprecated and it's usage will produce errors
	 */
	@Deprecated // use one of the other constructors
	public QNetwork(final Network networkLayer2) {
		this(null, networkLayer2, new DefaultQueueNetworkFactory());
	}

	public QNetwork(final QSim qs, final QNetworkFactory<QNode, QLink> factory ) {
		this( qs, qs.getScenario().getNetwork(), factory ) ;
	}

	private QNetwork(final QSim qs, final Network networkLayer, final QNetworkFactory<QNode, QLink> factory) {
		this.networkLayer = networkLayer;
		this.queueNetworkFactory = factory;
		this.links = new LinkedHashMap<Id, QLink>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		this.nodes = new LinkedHashMap<Id, QNode>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		for (Node n : networkLayer.getNodes().values()) {
			this.nodes.put(n.getId(), this.queueNetworkFactory.newQueueNode(n, this));
		}
		for (Link l : networkLayer.getLinks().values()) {
			this.links.put(l.getId(), this.queueNetworkFactory.newQueueLink(l, qs.simEngine, this.nodes.get(l.getToNode().getId())));
		}
		for (QNode n : this.nodes.values()) {
			n.init();
		}
	}

  public Network getNetworkLayer() {
		return this.networkLayer;
	}

	/**
	 * Called whenever this object should dump a snapshot
	 * @return A collection with the current positions of all vehicles.
	 */
	public Collection<AgentSnapshotInfo> getVehiclePositions(double time) {
		Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
		for (QLink link : this.links.values()) {
			link.getVisData().getVehiclePositions(positions);
		}
		return positions;
	}

	public Map<Id, QLink> getLinks() {
		return Collections.unmodifiableMap(this.links);
	}

	public Map<Id, QNode> getNodes() {
		return Collections.unmodifiableMap(this.nodes);
	}

	public QLink getQueueLink(final Id id) {
		return this.links.get(id);
	}

	public QNode getQueueNode(final Id id) {
		return this.nodes.get(id);
	}

}
