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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.jfree.util.Log;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.vis.snapshots.writers.PositionInfo;

/**
 * QueueNetwork is responsible for creating the QueueLinks/Nodes and for
 * implementing doSim
 *
 * @author david
 * @author mrieser
 * @author dgrether
 */
public class QueueNetwork{
	/* If simulateAllLinks is set to true, then the method "moveLink" will be called for every link in every timestep.
	 * If simulateAllLinks is set to false, the method "moveLink" will only be called for "active" links (links where at least one
	 * car is in one of the many queues).
	 * One should assume, that the result of a simulation is the same, no matter how "simulateAllLinks" is set. But the order how
	 * the links are processed influences the order of events within one time step. Thus, just comparing the event-files will not
	 * work, but first sorting the two event-files by time and agent-id and then comparing them, will work.
	 */
	private static boolean simulateAllLinks = false;
	private static boolean simulateAllNodes = false;

	/** This is the collection of links that have to be moved in the simulation */
	private final List<QueueLink> simLinksArray = new ArrayList<QueueLink>();
	/** This is the collection of nodes that have to be moved in the simulation */
	private final QueueNode[] simNodesArray;
	/** This is the collection of links that have to be activated in the current time step */
	private final ArrayList<QueueLink> simActivateThis = new ArrayList<QueueLink>();
	/** This is a queue of links and times, at which the links will have to be activated again. This queue is mostly used
	 * when vehicles are parking on a link but no other traffic occurs on that link. */
//	private final PriorityQueue<LinkActivation> activationQueue = new PriorityQueue<LinkActivation>();

	// set to true to move vehicles from waitingList before vehQueue
	private boolean moveWaitFirst = false;

	private final Map<Id, QueueLink> links;

	private final Map<Id, QueueNode> nodes;

	private final Network networkLayer;

	private final QueueNetworkFactory<QueueNode, QueueLink> queueNetworkFactory;

	public QueueNetwork(final Network networkLayer) {
		this(networkLayer, new DefaultQueueNetworkFactory());
	}

	public QueueNetwork(final Network networkLayer, final QueueNetworkFactory<QueueNode, QueueLink> factory) {
		this.networkLayer = networkLayer;
		this.queueNetworkFactory = factory;
		this.links = new LinkedHashMap<Id, QueueLink>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		this.nodes = new LinkedHashMap<Id, QueueNode>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		for (Node n : networkLayer.getNodes().values()) {
			this.nodes.put(n.getId(), this.queueNetworkFactory.newQueueNode(n, this));
		}
		for (Link l : networkLayer.getLinks().values()) {
			this.links.put(l.getId(), this.queueNetworkFactory.newQueueLink(l, this, this.nodes.get(l.getToNode().getId())));
		}
		for (QueueNode n : this.nodes.values()) {
			n.init();
		}

		this.simNodesArray = this.nodes.values().toArray(new QueueNode[this.nodes.size()]);
		//dg[april08] as the order of nodes has an influence on the simulation
		//results they are sorted to avoid indeterministic simulations
		Arrays.sort(this.simNodesArray, new Comparator<QueueNode>() {
			public int compare(final QueueNode o1, final QueueNode o2) {
				return o1.getNode().compareTo(o2.getNode());
			}
		});
	}

	public Network getNetworkLayer() {
		return this.networkLayer;
	}

	protected void beforeSim() {
		this.simLinksArray.clear();
		if (simulateAllLinks) {
			this.simLinksArray.addAll(this.links.values());
		}

		// finish init for links
		for (QueueLink link : this.links.values()) {
			link.finishInit();
		}
	}

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	protected void simStep(final double time) {
		moveNodes(time);
		reactivateLinks();
		moveLinks(time);
	}
	
	private void moveNodes(final double time) {
		for (QueueNode node : this.simNodesArray) {
			if (node.isActive() || node.isSignalized() || simulateAllNodes) {
				/* It is faster to first test if the node is active, and only then call moveNode(),
				 * than calling moveNode() directly and that one returns immediately when it's not
				 * active. Most likely, the getter isActive() can be in-lined by the compiler, while
				 * moveNode() cannot, resulting in fewer method-calls when isActive() is used.
				 * -marcel/20aug2008
				 */
				node.moveNode(time);
			}
		}
	}
	

	private void moveLinks(final double time) {
		ListIterator<QueueLink> simLinks = this.simLinksArray.listIterator();
		QueueLink link;
		boolean isActive;

		while (simLinks.hasNext()) {
			link = simLinks.next();
			isActive = link.moveLink(time);
			if (!isActive && !simulateAllLinks) {
				simLinks.remove();
			}
		}
	}

	/**
	 * Called whenever this object should dump a snapshot
	 * @return A collection with the current positions of all vehicles.
	 */
	public Collection<PositionInfo> getVehiclePositions() {
		Collection<PositionInfo> positions = new ArrayList<PositionInfo>();
		for (QueueLink link : this.links.values()) {
			link.getVisData().getVehiclePositions(positions);
		}
		return positions;
	}

	/**
	 * @return Returns the simLinksArray.
	 */
	protected Collection<QueueLink> getSimulatedLinks() {
		return this.simLinksArray;
	}

	protected void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (QueueLink link : this.links.values()) {
			link.clearVehicles();
		}
	}

	protected void addActiveLink(final QueueLink link) {
		if (!simulateAllLinks) {
			this.simActivateThis.add(link);
		}
	}

	private void reactivateLinks() {
		if (!simulateAllLinks) {
			if (!this.simActivateThis.isEmpty()) {
				this.simLinksArray.addAll(this.simActivateThis);
				this.simActivateThis.clear();
			}
		}
	}

	public void setMoveWaitFirst(final boolean moveWaitFirst){
		this.moveWaitFirst = moveWaitFirst;
	}
	
	protected boolean isMoveWaitFirst() {
		return this.moveWaitFirst;
	}

	public Map<Id, QueueLink> getLinks() {
		return Collections.unmodifiableMap(this.links);
	}

	public Map<Id, QueueNode> getNodes() {
		return Collections.unmodifiableMap(this.nodes);
	}

	public QueueLink getQueueLink(final Id id) {
		return this.links.get(id);
	}

	public QueueNode getQueueNode(final IdImpl id) {
		return this.nodes.get(id);
	}
	
	public static void setSimulateAllLinks(boolean simulateAll) {
		Log.warn("ATTENTION: simulateAllLinks is set. Make sure this is not happening while the simulation is running AND ONLY FOR TESTING PURPOSES!!!");
		simulateAllLinks = simulateAll;
	}

	public static void setSimulateAllNodes(boolean simulateAll) {
		Log.warn("ATTENTION: simulateAllNodes is set. Make sure this is not happening while the simulation is running AND ONLY FOR TESTING PURPOSES!!!");
		simulateAllNodes = simulateAll;
	}
	
}
