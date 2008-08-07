/* *********************************************************************** *
 * project: org.matsim.*
 * QueueNode.java
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

package org.matsim.mobsim.queuesim;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.matsim.events.AgentStuckEvent;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.Node;

/**
 * Represents a node in the QueueSimulation.
 */
public class QueueNode {

	private static final Logger log = Logger.getLogger(QueueNode.class);

	private boolean cacheIsInvalid = true;

	private QueueLink[] inLinksArrayCache = null;

	private QueueLink[] tempLinks = null;

	private QueueLink[] auxLinks = null;

	private boolean active = false;

	private Node node;

	public QueueNetwork queueNetwork;

	public QueueNode(Node n, QueueNetwork queueNetwork) {
		this.node = n;
		this.queueNetwork = queueNetwork;
	}

	private void buildCache() {
		this.inLinksArrayCache = new QueueLink[this.node.getInLinks().values()
				.size()];
		int i = 0;
		for (Link l : this.node.getInLinks().values()) {
			this.inLinksArrayCache[i] = this.queueNetwork.getLinks().get(
					l.getId());
			i++;
		}
		/* As the order of nodes has an influence on the simulation results,
		 * the nodes are sorted to avoid indeterministic simulations. dg[april08] 
		 */
		Arrays.sort(this.inLinksArrayCache, new Comparator<QueueLink>() {
			public int compare(final QueueLink o1, final QueueLink o2) {
				return o1.getLink().getId().compareTo(o2.getLink().getId());
			}
		});
		this.tempLinks = new QueueLink[this.node.getInLinks().values().size()];
		this.auxLinks = new QueueLink[this.node.getInLinks().values().size()];
		this.cacheIsInvalid = false;
	}

	public Node getNode() {
		return this.node;
	}

	// ////////////////////////////////////////////////////////////////////
	// Queue related movement code
	// ////////////////////////////////////////////////////////////////////
	public boolean moveVehicleOverNode(final Vehicle veh, final double now) {
		Link nextLink = veh.chooseNextLink();
		Link currentLink = veh.getCurrentLink();
		QueueLink currentQueueLink = this.queueNetwork
				.getQueueLink(currentLink.getId());
		// veh has to move over node
		if (nextLink != null) {

		QueueLink nextQueueLink = this.queueNetwork.getQueueLink(nextLink
				.getId());

			if (nextQueueLink.hasSpace()) {
				currentQueueLink.popFirstFromBuffer();
				veh.incCurrentNode();
				nextQueueLink.add(veh);
				return true;
			}

			// check if veh is stuck!

			if ((now - veh.getLastMovedTime()) > Simulation.getStuckTime()) {
				/* We just push the vehicle further after stucktime is over, regardless
				 * of if there is space on the next link or not.. optionally we let them
				 * die here, we have a config setting for that!
				 */
				if (removeStuckVehicle()) {
					currentQueueLink.popFirstFromBuffer();
					Simulation.decLiving();
					Simulation.incLost();
					QueueSimulation.getEvents().processEvent(
							new AgentStuckEvent(now, veh.getDriver().getId().toString(), 
									veh.getCurrentLegNumber(), currentLink.getId().toString(), 
									veh.getDriver(), veh.getCurrentLeg(), currentLink));
				}
				else {
					currentQueueLink.popFirstFromBuffer();
					veh.incCurrentNode();
					nextQueueLink.add(veh);
					return true;
				}
			}
			return false;
		}

		// --> nextLink == null
		currentQueueLink.popFirstFromBuffer();
		Simulation.decLiving();
		Simulation.incLost();
		log.error(
				"Agent has no or wrong route! agentId=" + veh.getDriver().getId()
						+ " currentLegNumber=" + veh.getCurrentLegNumber()
						+ " currentLink=" + currentLink.getId().toString()
						+ ". The agent is removed from the simulation.");
		return true;
	}

	final public void activateNode() {
		this.active = true;
	}

	/**
	 * Moves vehicles from the inlinks' buffer to the outlinks where possible.<br>
	 * The inLinks are randomly chosen, and for each link all vehicles in the
	 * buffer are moved to their desired outLink as long as there is space. If the
	 * front most vehicle in a buffer cannot move across the node because there is
	 * no free space on its destination link, the work on this inLink is finished
	 * and the next inLink's buffer is handled (this means, that at the node, all
	 * links have only like one lane, and there are no separate lanes for the
	 * different outLinks. Thus if the front most vehicle cannot drive further,
	 * all other vehicles behind must wait, too, even if their links would be
	 * free).
	 *
	 * @param now
	 *          The current time in seconds from midnight.
	 */
	public void moveNode(final double now) {
		/* called by the framework, do all necessary action for node movement here */

		if (!this.active) {
			return;
		}

		if (this.cacheIsInvalid) {
			buildCache();
		}

		int inLinksCounter = 0;
		double inLinksCapSum = 0.0;
		// Check all incoming links for buffered agents
		for (QueueLink link : this.inLinksArrayCache) {
			if (!link.bufferIsEmpty()) {
				this.tempLinks[inLinksCounter] = link;
				inLinksCounter++;
				inLinksCapSum += link.getLink().getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
			}
		}

		if (inLinksCounter == 0) {
			this.active = false;
			return; // Nothing to do
		}

		int auxCounter = 0;
		// randomize based on capacity
		while (auxCounter < inLinksCounter) {
			double rndNum = Gbl.random.nextDouble() * inLinksCapSum;
			double selCap = 0.0;
			for (int i = 0; i < inLinksCounter; i++) {
				QueueLink link = this.tempLinks[i];
				if (link == null)
					continue;
				selCap += link.getLink().getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
				if (selCap >= rndNum) {
					this.auxLinks[auxCounter] = link;
					auxCounter++;
					inLinksCapSum -= link.getLink().getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
					this.tempLinks[i] = null;
					break;
				}
			}
		}

		for (int i = 0; i < auxCounter; i++) {
			QueueLink link = this.auxLinks[i];
			// Move agents/vehicle data to next link
			while (!link.bufferIsEmpty()) {
				Vehicle veh = link.getFirstFromBuffer();
				if (!moveVehicleOverNode(veh, now)) {
					break;
				}
			}
		}
	}

	static boolean removeVehInitialized = false;

	static boolean removeVehicles = true;

	static boolean removeStuckVehicle() {
		if (removeVehInitialized) {
			return removeVehicles;
		}
		removeVehicles = Gbl.getConfig().simulation().removeStuckVehicles();
		removeVehInitialized = true;
		return removeVehicles;
	}
}
