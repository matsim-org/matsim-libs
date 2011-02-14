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

package playground.mrieser.core.mobsim.features.refQueueNetworkFeature;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.mrieser.core.mobsim.api.TimestepMobsimEngine;
import playground.mrieser.core.mobsim.network.api.MobsimNetwork;

/**
 * @author mrieser
 */
/*package*/ class QueueNetwork implements MobsimNetwork {

	protected final TimestepMobsimEngine simEngine;
	private final Map<Id, QueueLink> links;
	private final Map<Id, QueueNode> nodes;
	private QueueNode[] nodesArray = null;
	private double storageCapFactor = 1.0;
	private double effectiveCellSize = 7.5;
	private boolean removeStuckVehicles = true;
	private double stuckTime = 100;

	public QueueNetwork(final TimestepMobsimEngine simEngine) {
		this.simEngine = simEngine;
		this.links = new LinkedHashMap<Id, QueueLink>();
		this.nodes = new LinkedHashMap<Id, QueueNode>();
	}

	@Override
	public void beforeMobSim() {
	}

	@Override
	public void doSimStep(final double time) {
		if (this.nodesArray == null) {
			initialize();
		}
		for (QueueNode node : this.nodesArray) {
			node.moveNode(time);
		}
		for (QueueLink link : this.links.values()) {
			link.doSimStep(time);
		}
	}

	@Override
	public void afterMobSim() {
		// generate stuck events for agents still on the road
		for (QueueLink link : this.links.values()) {
			link.makeAllVehiclesStuck();
		}
	}

	private void initialize() {
		this.nodesArray = this.nodes.values().toArray(new QueueNode[this.nodes.size()]);
    // sort the nodes for deterministic order / results
    Arrays.sort(this.nodesArray, new Comparator<QueueNode>() {
      @Override
			public int compare(final QueueNode o1, final QueueNode o2) {
        return o1.getId().compareTo(o2.getId());
      }
    });
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
