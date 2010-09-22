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

package soc.ai.matsim.dbsim;

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
public class DBSimEngine {

	/* If simulateAllLinks is set to true, then the method "moveLink" will be called for every link in every timestep.
	 * If simulateAllLinks is set to false, the method "moveLink" will only be called for "active" links (links where at least one
	 * car is in one of the many queues).
	 * One should assume, that the result of a simulation is the same, no matter how "simulateAllLinks" is set. But the order how
	 * the links are processed influences the order of events within one time step. Thus, just comparing the event-files will not
	 * work, but first sorting the two event-files by time and agent-id and then comparing them, will work.
	 */
	private static boolean simulateAllLinks = false;
	private static boolean simulateAllNodes = false;

	private final List<DBSimLink> allLinks;
	/** This is the collection of links that have to be moved in the simulation */
	private final List<DBSimLink> simLinksArray = new ArrayList<DBSimLink>();
	/** This is the collection of nodes that have to be moved in the simulation */
	private final DBSimNode[] simNodesArray;
	/** This is the collection of links that have to be activated in the current time step */
	private final ArrayList<DBSimLink> simActivateThis = new ArrayList<DBSimLink>();

	private final Random random;
	private final DBSimulation sim;

	public DBSimEngine(final DBSimNetwork network, final Random random, final DBSimulation sim) {
		this(network.getLinks().values(), network.getNodes().values(), random, sim);
	}

	/*package*/ DBSimEngine(final Collection<DBSimLink> links, final Collection<DBSimNode> nodes, final Random random, final DBSimulation sim) {
		this.random = random;
		this.allLinks = new ArrayList<DBSimLink>(links);
		this.sim = sim;

		this.simNodesArray = nodes.toArray(new DBSimNode[nodes.size()]);
		//dg[april08] as the order of nodes has an influence on the simulation
		//results they are sorted to avoid indeterministic simulations
		Arrays.sort(this.simNodesArray, new Comparator<DBSimNode>() {
			@Override
			public int compare(final DBSimNode o1, final DBSimNode o2) {
				return o1.getNode().getId().compareTo(o2.getNode().getId());
			}
		});
		for (DBSimNode node : this.simNodesArray) {
			node.setSimEngine(this);
		}
		for (DBSimLink link : this.allLinks) {
			link.finishInit();
			link.setSimEngine(this);
		}
		if (simulateAllLinks) {
			this.simLinksArray.addAll(this.allLinks);
		}
	}

	protected void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (DBSimLink link : this.allLinks) {
			link.clearVehicles();
		}
	}

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	protected void simStep(final double time) {
		moveNodes(time);
		moveLinks(time);
	}

	protected void moveNodes(final double time) {
		for (DBSimNode node : this.simNodesArray) {
			if (node.isActive() || simulateAllNodes) {
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
		ListIterator<DBSimLink> simLinks = this.simLinksArray.listIterator();
		DBSimLink link;
		boolean isActive;

		while (simLinks.hasNext()) {
			link = simLinks.next();
			isActive = link.moveLink(time);
			if (!isActive && !simulateAllLinks) {
				simLinks.remove();
			}
		}
	}

	protected void activateLink(final DBSimLink link) {
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
	protected int getNumberOfSimulatedLinks() {
		return this.simLinksArray.size();
	}

	public DBSimulation getSim() {
		return sim;
	}
}
