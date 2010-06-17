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

package playground.mrieser.core.sim.network.queueNetwork;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.mrieser.core.sim.api.TimestepSimEngine;
import playground.mrieser.core.sim.network.api.SimLink;
import playground.mrieser.core.sim.network.api.SimNetwork;
import playground.mrieser.core.sim.network.api.SimNode;

/**
 * @author mrieser
 */
/*package*/ class QueueNetwork implements SimNetwork {

	protected final TimestepSimEngine simEngine;
	private final Map<Id, QueueLink> links;
	private final Map<Id, QueueNode> nodes;
	private double flowCapFactor = 1.0;
	private double storageCapFactor = 1.0;
	private double effectiveCellSize = 7.5;

	public QueueNetwork(final TimestepSimEngine simEngine) {
		this.simEngine = simEngine;
		this.links = new HashMap<Id, QueueLink>();
		this.nodes = new HashMap<Id, QueueNode>();
	}

	@Override
	public void doSimStep(double time) {
		for (QueueNode node : this.nodes.values()) {
			node.doSimStep(time);
		}
		for (QueueLink link : this.links.values()) {
			link.doSimStep(time);
		}
	}

	@Override
	public Map<Id, ? extends SimLink> getLinks() {
		return this.links;
	}

	@Override
	public Map<Id, ? extends SimNode> getNodes() {
		return this.nodes;
	}

	/*package*/ void addLink(final QueueLink link) {
		this.links.put(link.getId(), link);
	}

	/*package*/ void addNode(final QueueNode node) {
		this.nodes.put(node.getId(), node);
	}

	public void setFlowCapFactor(double flowCapFactor) {
		this.flowCapFactor = flowCapFactor;
	}

	public double getFlowCapFactor() {
		return this.flowCapFactor;
	}

	public void setStorageCapFactor(double storageCapFactor) {
		this.storageCapFactor = storageCapFactor;
	}

	public double getStorageCapFactor() {
		return storageCapFactor;
	}

	public void setEffectiveCellSize(double effectiveCellSize) {
		this.effectiveCellSize = effectiveCellSize;
	}

	public double getEffectiveCellSize() {
		return effectiveCellSize;
	}

}
