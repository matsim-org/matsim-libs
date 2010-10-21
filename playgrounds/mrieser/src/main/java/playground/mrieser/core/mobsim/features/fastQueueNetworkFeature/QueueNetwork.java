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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.mrieser.core.mobsim.api.TimestepSimEngine;
import playground.mrieser.core.mobsim.network.api.MobSimNetwork;

/**
 * @author mrieser
 */
/*package*/ class QueueNetwork implements MobSimNetwork {

	protected final TimestepSimEngine simEngine;
	private final Map<Id, QueueLink> links;
	private final Map<Id, QueueNode> nodes;
	private double storageCapFactor = 1.0;
	private double effectiveCellSize = 7.5;
	private boolean removeStuckVehicles = true;
	private double stuckTime = 100;
	private final Operator operator;
	private boolean simStarted = false;

	public QueueNetwork(final TimestepSimEngine simEngine, final Operator operator) {
		this.simEngine = simEngine;
		this.operator = operator;
		this.links = new LinkedHashMap<Id, QueueLink>();
		this.nodes = new LinkedHashMap<Id, QueueNode>();
	}

	@Override
	public void beforeMobSim() {
		this.simStarted = true;
		for (QueueLink link : this.links.values()) {
			// storageCapFactor could have changed since construction
			link.recalculateAttributes();
		}
		this.operator.beforeMobSim();
	}

	@Override
	public void doSimStep(final double time) {
		this.operator.doSimStep(time);
	}

	@Override
	public void afterMobSim() {
		this.operator.afterMobSim();
	}

	@Override
	public Map<Id, ? extends QueueLink> getLinks() {
		return this.links;
	}

	@Override
	public Map<Id, ? extends QueueNode> getNodes() {
		return this.nodes;
	}

	/*package*/ void addLink(final QueueLink link) {
		this.links.put(link.getId(), link);
	}

	/*package*/ void addNode(final QueueNode node) {
		this.nodes.put(node.getId(), node);
	}

	public void setStorageCapFactor(final double storageCapFactor) {
		if (this.simStarted) {
			throw new IllegalStateException("Mobility Simulation already started. Storage Capacity Factor must be set before simulation is started.");
		}
		this.storageCapFactor = storageCapFactor;
	}

	/*package*/ double getStorageCapFactor() {
		return this.storageCapFactor;
	}

	public void setEffectiveCellSize(final double effectiveCellSize) {
		this.effectiveCellSize = effectiveCellSize;
	}

	/*package*/ double getEffectiveCellSize() {
		return this.effectiveCellSize;
	}

	/*package*/ boolean isRemoveStuckVehicles() {
		return this.removeStuckVehicles;
	}

	public void setRemoveStuckVehicles(final boolean removeStuckVehicles) {
		this.removeStuckVehicles = removeStuckVehicles;
	}

	/*package*/ double getStuckTime() {
		return this.stuckTime;
	}

	public void setStuckTime(final double stuckTime) {
		this.stuckTime = stuckTime;
	}

}
