/* *********************************************************************** *
 * project: org.matsim.*
 * QueueNetworkLayer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.mobsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;

/**
 * @author david
 * @author mrieser // node de-/activation, activationQueue for parking vehicles
 *
 * QueueNetworkLayer is responsible for creating the QueueLinks/Nodes and for
 * implementing doSim
 */
public class QueueNetworkLayer /* extends NetworkLayer */{
	/* If simulateAllLinks is set to true, then the method "moveLink" will be called for every link in every timestep.
	 * If simulateAllLinks is set to false, the method "moveLink" will only be called for "active" links (links where at least one
	 * car is in one of the many queues).
	 * One should assume, that the result of a simulation is the same, no matter how "simulateAllLinks" is set. But the order how
	 * the links are processed influences the order of events within one time step. Thus, just comparing the event-files will not
	 * work, but first sorting the two event-files by time and agent-id and then comparing them, will work.
	 */
	private final static boolean simulateAllLinks = false;

	/** This is the collection of links that have to be moved in the simulation */
	private final ArrayList<QueueLink> simLinksArray = new ArrayList<QueueLink>();
	/** This is the collection of nodes that have to be moved in the simulation */
	private QueueNode[] simNodesArrayCache = new QueueNode[0];
	/** This is the collection of links that have to be activated in the current time step */
	private final ArrayList<QueueLink> simActivateThis = new ArrayList<QueueLink>();
	/** This is a queue of links and times, at which the links will have to be activated again. This queue is mostly used
	 * when vehicles are parking on a link but no other traffic occurs on that link. */
	private final PriorityQueue<LinkActivation> activationQueue = new PriorityQueue<LinkActivation>();

	// set to true to move vehicles from waitingList before vehQueue
	private boolean moveWaitFirst = false;

	private Map<Id, QueueLink> links;

	private Map<Id, QueueNode> nodes;

	private NetworkLayer networkLayer;

	public QueueNetworkLayer(NetworkLayer networkLayer) {
		this.networkLayer = networkLayer;
		this.links = new HashMap<Id, QueueLink>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		this.nodes = new HashMap<Id, QueueNode>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		for (Node n : networkLayer.getNodes().values()) {
			this.nodes.put(n.getId(), new QueueNode(n, this));
		}
		for (Link l : networkLayer.getLinks().values()) {
			this.links.put(l.getId(), new QueueLink(l, this, this.nodes.get(l.getToNode().getId())));
		}
	}

	public NetworkLayer getNetworkLayer() {
		return this.networkLayer;
	}

	public void beforeSim() {

		// if simNodesArrayCache was invalidated before, it needs to be reinitialized
		if (this.simNodesArrayCache == null) {
			this.simNodesArrayCache = new QueueNode[0];
		}

		this.simNodesArrayCache = getNodes().values().toArray(this.simNodesArrayCache);
		//dg[april08] as the order of nodes has an influence on the simulation
		//results they are sorted to avoid indeterministic simulations
		Arrays.sort(this.simNodesArrayCache, new Comparator<QueueNode>() {
			public int compare(final QueueNode o1, final QueueNode o2) {
				return o1.getNode().compareTo(o2.getNode());
			}
		});

		this.simLinksArray.clear();
		if (simulateAllLinks) {
			this.simLinksArray.addAll(getLinks().values());
		}

		// finish init for links
		for (QueueLink link : getLinks().values()) {
			link.finishInit();
		}
	}

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	public void simStep(final double time) {
		for (QueueNode node : this.simNodesArrayCache) {
			node.moveNode(time);
		}
		reactivateLinks(time);
		moveLinks(time);
	}

	protected void moveLinks(final double time) {
		ListIterator<QueueLink> links = this.simLinksArray.listIterator();
		QueueLink link;
		boolean isActive;

		// TODO [kn] this is in my view unstable code.  Should be
		// while (links.hasNext()) {
		//    link = links.next();
		//    if ( moveWaitFirst ) {
		//        isActive = link.moveLinkWaitFirst(time);
		//    } else {
		//          isActive = ...isActive;
		//    }
		//    if ( !isActive ...
		// kai, nov07
		/* well, we just moved the if (moveWaitFirst) outside of the while-loop,
		 * so we have the if only once and not for every link. marcel, dez07 */

		if (this.moveWaitFirst) {

			while (links.hasNext()) {
				link = links.next();
				isActive = link.moveLinkWaitFirst(time);
				if (!isActive && !simulateAllLinks) {
					links.remove();
				}
			}

		} else {

			while (links.hasNext()) {
				link = links.next();
				isActive = link.moveLink(time);
				if (!isActive && !simulateAllLinks) {
					links.remove();
				}
			}

		}
	}

	/**
	 * Called whenever this object should dump a snapshot
	 * @return A collection with the current positions of all vehicles.
	 */
	public Collection<PositionInfo> getVehiclePositions() {
		Collection<PositionInfo> positions = new ArrayList<PositionInfo>();
		for (QueueLink link : getLinks().values()) {
			link.getVehiclePositions(positions);
		}
		return positions;
	}

	/**
	 * @return Returns the simLinksArray.
	 */
	public Collection<QueueLink> getSimulatedLinks() {
		return this.simLinksArray;
	}

	/**
	 * @return Returns the simNodesArray.
	 */
	public Collection<QueueNode> getSimulatedNodes() {
		return Arrays.asList(this.simNodesArrayCache);
	}

	public void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (QueueLink link : getLinks().values()) {
			link.clearVehicles();
		}
	}

	public void addActiveLink(final QueueLink link) {
		if (!simulateAllLinks) {
			this.simActivateThis.add(link);
		}
	}

	private void reactivateLinks(final double now) {
		if (!simulateAllLinks) {
			// links being activated because somebody's leaving on that link
			LinkActivation activation = this.activationQueue.peek();
			while ((activation != null) && (activation.time <= now)) {
				activation = this.activationQueue.poll();
				activation.link.activateLink();
				activation = this.activationQueue.peek();
			}
			// links being activated because somebody's driving on them
			if (!this.simActivateThis.isEmpty()) {
				this.simLinksArray.addAll(this.simActivateThis);
				this.simActivateThis.clear();
			}
		}
	}

	public void setLinkActivation(final double time, final QueueLink link) {
		if (!simulateAllLinks) {
			this.activationQueue.add(new LinkActivation(time, link));
		}
	}

	public void moveWaitFirst(final boolean moveWaitFirst){
		//TODO [GL] may be we should invalidate simNodesArrayCache to make sure
		// that nobody changes this during the MobSim ...
		this.moveWaitFirst = moveWaitFirst;
	}

	public Map<Id, QueueLink> getLinks() {
		return Collections.unmodifiableMap(this.links);
	}

	public Map<Id, QueueNode> getNodes() {
		return Collections.unmodifiableMap(this.nodes);
	}

	final private static class LinkActivation implements Comparable<LinkActivation> {
		final public double time;
		final public QueueLink link;
		public LinkActivation(final double time, final QueueLink link) {
			this.time = time;
			this.link = link;
		}

		public int compareTo(final LinkActivation o) {
			if (this.time < o.time) return -1;
			if (this.time > o.time) return +1;
			return 0;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null) return false;
			if (!(obj instanceof LinkActivation)) return false;
			LinkActivation la = (LinkActivation)obj;
			return (this.time == la.time) && (this.link.equals(la.link));
		}

		@Override
		public int hashCode() {
			return this.link.hashCode();
		}
	}

	public QueueLink getQueueLink(Id id) {
		return this.links.get(id);
	}

	public QueueNode getQueueNode(IdImpl id) {
		return this.nodes.get(id);
	}

}
