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

package soc.ai.matsim.dbsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vis.snapshots.writers.PositionInfo;

/**
 * QueueNetwork is responsible for creating the QueueLinks and QueueNodes.
 *
 * @author david
 * @author mrieser
 * @author dgrether
 */
public class DBSimNetwork{

	private final Map<Id, DBSimLink> links;

	private final Map<Id, DBSimNode> nodes;

	private final Network networkLayer;

	private final DBSimNetworkFactory<DBSimNode, DBSimLink> queueNetworkFactory;

	public DBSimNetwork(final Network networkLayer2) {
		this(networkLayer2, new DefaultDBSimNetworkFactory());
	}

	public DBSimNetwork(final Network networkLayer, final DBSimNetworkFactory<DBSimNode, DBSimLink> factory) {
		this.networkLayer = networkLayer;
		this.queueNetworkFactory = factory;
		this.links = new LinkedHashMap<Id, DBSimLink>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		this.nodes = new LinkedHashMap<Id, DBSimNode>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		for (Node n : networkLayer.getNodes().values()) {
			this.nodes.put(n.getId(), this.queueNetworkFactory.newQueueNode(n, this));
		}
		for (Link l : networkLayer.getLinks().values()) {
			this.links.put(l.getId(), this.queueNetworkFactory.newQueueLink(l, this, this.nodes.get(l.getToNode().getId())));
		}
		for (DBSimNode n : this.nodes.values()) {
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
	public Collection<PositionInfo> getVehiclePositions() {
		Collection<PositionInfo> positions = new ArrayList<PositionInfo>();
		for (DBSimLink link : this.links.values()) {
			link.getVisData().getVehiclePositions(positions);
		}
		return positions;
	}

	public Map<Id, DBSimLink> getLinks() {
		return Collections.unmodifiableMap(this.links);
	}

	public Map<Id, DBSimNode> getNodes() {
		return Collections.unmodifiableMap(this.nodes);
	}

	public DBSimLink getQueueLink(final Id id) {
		return this.links.get(id);
	}

	public DBSimNode getQueueNode(final Id id) {
		return this.nodes.get(id);
	}
	
}
