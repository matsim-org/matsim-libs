/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * Coordinates the movement of vehicles on the links and the nodes. 
 *
 * @author mrieser
 * @author dgrether
 * @author dstrippgen
 */
public class QueueSimEngine {

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
	
	private final QueueNetwork network;
	
	public QueueSimEngine(final QueueNetwork network) {
		this.network = network;
		this.simNodesArray = network.getNodes().values().toArray(new QueueNode[network.getNodes().size()]);
		//dg[april08] as the order of nodes has an influence on the simulation
		//results they are sorted to avoid indeterministic simulations
		Arrays.sort(this.simNodesArray, new Comparator<QueueNode>() {
			public int compare(final QueueNode o1, final QueueNode o2) {
				return o1.getNode().compareTo(o2.getNode());
			}
		});
		for (QueueLink link : this.network.getLinks().values()) {
			link.finishInit();
			link.setSimEngine(this);
		}
		if (simulateAllLinks) {
			this.simLinksArray.addAll(this.network.getLinks().values());
		}
	}
	
	protected void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (QueueLink link : this.network.getLinks().values()) {
			link.clearVehicles();
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

	protected void activateLink(final QueueLink link) {
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
	
	/**
	 * @return Returns the simLinksArray.
	 */
	protected Collection<QueueLink> getSimulatedLinks() {
		return this.simLinksArray;
	}
}
