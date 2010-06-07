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
import org.matsim.core.mobsim.queuesim.interfaces.CapacityInformationLink;
import org.matsim.core.mobsim.queuesim.interfaces.CapacityInformationNetwork;
import org.matsim.ptproject.qsim.QSimI;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.VisLink;
import org.matsim.vis.snapshots.writers.VisNetwork;
import org.matsim.vis.snapshots.writers.VisNode;

/**
 * QueueNetwork is responsible for creating the QueueLinks and QueueNodes.
 *
 * @author david
 * @author mrieser
 * @author dgrether
 */
 class QueueNetwork implements VisNetwork, CapacityInformationNetwork {
	private QSimI qSim = null ; // QueueNetwork can exist without qSim, so this is not enforced.

	private final Map<Id, QueueLink> queuelinks;

	private final Map<Id, QueueNode> queuenodes;

	private final Network networkLayer;

	private final QueueNetworkFactory<QueueNode, QueueLink> queueNetworkFactory;

	/**
	 * @param networkLayer2
	 * @param qSim2 -- may be null, in particular for tests
	 */
	/*package*/ QueueNetwork(final Network networkLayer2, QSimI qSim2) {
		this(networkLayer2, new DefaultQueueNetworkFactory(), qSim2);
	}

	/*package*/ QueueNetwork(final Network networkLayer, final QueueNetworkFactory<QueueNode, QueueLink> factory, QSimI qSim2) {
		this.networkLayer = networkLayer;
		this.queueNetworkFactory = factory;
		this.qSim = qSim2 ;
		
		this.queuelinks = new LinkedHashMap<Id, QueueLink>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		this.queuenodes = new LinkedHashMap<Id, QueueNode>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		for (Node n : networkLayer.getNodes().values()) {
			this.queuenodes.put(n.getId(), this.queueNetworkFactory.createQueueNode(n, this));
		}
		for (Link l : networkLayer.getLinks().values()) {
			this.queuelinks.put(l.getId(), this.queueNetworkFactory.createQueueLink(l, this, this.queuenodes.get(l.getToNode().getId())));
		}
		for (QueueNode n : this.queuenodes.values()) {
			n.init();
		}
	}

	public Network getNetwork() {
		return this.networkLayer;
	}

	/**
	 * Called whenever this object should dump a snapshot
	 * @return A collection with the current positions of all vehicles.
	 */
	/*package*/ Collection<AgentSnapshotInfo> getVehiclePositions() {
		Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
		for (QueueLink link : this.queuelinks.values()) {
			link.getVisData().getVehiclePositions(positions);
		}
		return positions;
	}

	/*package*/ Map<Id, QueueLink> getQueueLinks() {
		return Collections.unmodifiableMap(this.queuelinks);
	}

	@Deprecated // only used by christoph
	public Map<Id, ? extends CapacityInformationLink> getCapacityInformationLinks() {
		return Collections.unmodifiableMap( this.queuelinks ) ;
	}

	/*package*/ Map<Id, QueueNode> getQueueNodes() {
		return Collections.unmodifiableMap(this.queuenodes);
	}
	
	public Map<Id,? extends VisLink> getVisLinks() {
		return Collections.unmodifiableMap( this.queuelinks ) ;
	}
	
	public Map<Id,? extends VisNode> getVisNodes() {
		return Collections.unmodifiableMap( this.queuenodes);
	}

	/*package*/ QueueLink getQueueLink(final Id id) {
		return this.queuelinks.get(id);
	}

	/*package*/ QueueNode getQueueNode(final Id id) {
		return this.queuenodes.get(id);
	}
	
	public QSimI getQSim() {
		return qSim;
	}

}
