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

package playground.mrieser.core.mobsim.network.fastQueueNetwork;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.mrieser.core.mobsim.api.TimestepSimEngine;
import playground.mrieser.core.mobsim.network.api.SimNetwork;

/**
 * @author mrieser
 */
/*package*/ class QueueNetwork implements SimNetwork {

	protected final TimestepSimEngine simEngine;
	private final Map<Id, QueueLink> links;
	private final Map<Id, QueueNode> nodes;
	private double storageCapFactor = 1.0;
	private double effectiveCellSize = 7.5;
	private boolean removeStuckVehicles = true;
	private double stuckTime = 100;
	private final LinkedList<QueueLink> activeLinks = new LinkedList<QueueLink>();
	private final LinkedList<QueueNode> activeNodes = new LinkedList<QueueNode>();
	private final ArrayList<QueueLink> linksToActivate = new ArrayList<QueueLink>(100);
	private final ArrayList<QueueNode> nodesToActivate = new ArrayList<QueueNode>(100);

	public QueueNetwork(final TimestepSimEngine simEngine) {
		this.simEngine = simEngine;
		this.links = new LinkedHashMap<Id, QueueLink>();
		this.nodes = new LinkedHashMap<Id, QueueNode>();
	}

	@Override
	public void doSimStep(final double time) {
		this.activeNodes.addAll(this.nodesToActivate);
		this.nodesToActivate.clear();
		ListIterator<QueueNode> simNodes = this.activeNodes.listIterator();
		while (simNodes.hasNext()) {
			QueueNode node = simNodes.next();
			node.moveNode(time);
			if (!node.isActive()) {
				simNodes.remove();
			}
		}

		this.activeLinks.addAll(this.linksToActivate);
		this.linksToActivate.clear();
		ListIterator<QueueLink> simLinks = this.activeLinks.listIterator();
		while (simLinks.hasNext()) {
			QueueLink link = simLinks.next();
			link.doSimStep(time);
			if (!link.isActive()) {
				simLinks.remove();
			}
		}
		if (time % 3600 == 0) {
			System.out.println("@@@ # active links = " + this.activeLinks.size());
			System.out.println("@@@ # active nodes = " + this.activeNodes.size());
		}
	}

	/*package*/ void activateLink(final QueueLink link) {
		this.linksToActivate.add(link);
	}

	/*package*/ void activateNode(final QueueNode node) {
		this.nodesToActivate.add(node);
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
