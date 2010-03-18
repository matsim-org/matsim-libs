/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEngineImpl.java
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

package org.matsim.ptproject.qsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;


/**
 * Coordinates the movement of vehicles on the links and the nodes.
 *
 * @author mrieser
 * @author dgrether
 * @author dstrippgen
 */
public class QSimEngineImpl implements QSimEngine{

	/* If simulateAllLinks is set to true, then the method "moveLink" will be called for every link in every timestep.
	 * If simulateAllLinks is set to false, the method "moveLink" will only be called for "active" links (links where at least one
	 * car is in one of the many queues).
	 * One should assume, that the result of a simulation is the same, no matter how "simulateAllLinks" is set. But the order how
	 * the links are processed influences the order of events within one time step. Thus, just comparing the event-files will not
	 * work, but first sorting the two event-files by time and agent-id and then comparing them, will work.
	 */
	/*package*/ static boolean simulateAllLinks = false;
	/*package*/ static boolean simulateAllNodes = false;

	/*package*/ final List<QLink> allLinks;
	/** This is the collection of links that have to be moved in the simulation */
	/*package*/ final List<QLink> simLinksArray = new ArrayList<QLink>();
	/** This is the collection of nodes that have to be moved in the simulation */
	/*package*/ final QNode[] simNodesArray;
	/** This is the collection of links that have to be activated in the current time step */
	/*package*/ final ArrayList<QLink> simActivateThis = new ArrayList<QLink>();

	private final Random random;

	public QSimEngineImpl(final QNetwork network, final Random random) {
		this(network.getLinks().values(), network.getNodes().values(), random);
	}

	/*package*/ QSimEngineImpl(final Collection<QLink> links, final Collection<QNode> nodes, final Random random) {
		this.random = random;
		this.allLinks = new ArrayList<QLink>(links);

		this.simNodesArray = nodes.toArray(new QNode[nodes.size()]);
		//dg[april08] as the order of nodes has an influence on the simulation
		//results they are sorted to avoid indeterministic simulations
		Arrays.sort(this.simNodesArray, new Comparator<QNode>() {
			public int compare(final QNode o1, final QNode o2) {
				return o1.getNode().getId().compareTo(o2.getNode().getId());
			}
		});
		for (QLink link : this.allLinks) {
			link.setQSimEngine(this);
		}
		if (simulateAllLinks) {
			this.simLinksArray.addAll(this.allLinks);
		}
	}

	public void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (QLink link : this.allLinks) {
			link.clearVehicles();
		}
	}

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	public void simStep(final double time) {
		moveNodes(time);
		moveLinks(time);
	}

	protected void moveNodes(final double time) {
		for (QNode node : this.simNodesArray) {
			if (node.isActive() || node.isSignalized() || simulateAllNodes) {
				/* It is faster to first test if the node is active, and only then call moveNode(),
				 * than calling moveNode() directly and that one returns immediately when it's not
				 * active. Most likely, the getter isActive() can be in-lined by the compiler, while
				 * moveNode() cannot, resulting in fewer method-calls when isActive() is used.
				 * -marcel/20aug2008
				 */
				node.moveNode(time, random);
			}
		}
	}

	protected void moveLinks(final double time) {
		reactivateLinks();
		ListIterator<QLink> simLinks = this.simLinksArray.listIterator();
		QLink link;
		boolean isActive;

		while (simLinks.hasNext()) {
			link = simLinks.next();
			isActive = link.moveLink(time);
			if (!isActive && !simulateAllLinks) {
				simLinks.remove();
			}
		}
	}

	public void activateLink(final QLink link) {
		if (!simulateAllLinks) {
			this.simActivateThis.add(link);
		}
	}

	private void reactivateLinks() {
		if ((!simulateAllLinks) && (!this.simActivateThis.isEmpty())) {
			this.simLinksArray.addAll(this.simActivateThis);
			this.simActivateThis.clear();
		}
	}

	/**
	 * @return Returns the simLinksArray.
	 */
	public int getNumberOfSimulatedLinks() {
		return this.simLinksArray.size();
	}
}
