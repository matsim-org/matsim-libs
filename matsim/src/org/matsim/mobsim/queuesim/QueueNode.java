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
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;

/**
 * Represents a node in the QueueSimulation.
 */
public class QueueNode {

	private static final Logger log = Logger.getLogger(QueueNode.class);

	private final QueueLink[] inLinksArrayCache;

	private final QueueLink[] tempLinks;

	private boolean active = false;

	private final Node node;

	public QueueNetwork queueNetwork;
	/**
	 * Indicates whether this node is signalized or not
	 */
	private boolean signalized = false;

	public QueueNode(final Node n, final QueueNetwork queueNetwork) {
		this.node = n;
		this.queueNetwork = queueNetwork;

		int nofInLinks = this.node.getInLinks().size();
		this.inLinksArrayCache = new QueueLink[nofInLinks];
		this.tempLinks = new QueueLink[nofInLinks];
	}

	/**
	 * Loads the inLinks-array with the corresponding links.
	 * Cannot be called in constructor, as the queueNetwork does not yet know
	 * the queueLinks. Should be called by QueueNetwork, after creating all
	 * QueueNodes and QueueLinks.
	 */
	/*package*/ void init() {
		int i = 0;
		for (Link l : this.node.getInLinks().values()) {
			this.inLinksArrayCache[i] = this.queueNetwork.getLinks().get(l.getId());
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
	}

	public Node getNode() {
		return this.node;
	}

	// ////////////////////////////////////////////////////////////////////
	// Queue related movement code
	// ////////////////////////////////////////////////////////////////////
	protected boolean moveVehicleOverNode(final QueueVehicle veh, final QueueLane currentLane, final double now) {
		Link nextLink = veh.getDriver().chooseNextLink();
		Link currentLink = veh.getCurrentLink();
		// veh has to move over node
		if (nextLink != null) {

			QueueLink nextQueueLink = this.queueNetwork.getQueueLink(nextLink.getId());
			// _FIXME: Ich haette gerne die alte Konstruktion wieder, dass explizit ueber den Knoten nach den outgoing links
			// gesucht wird.  Ansonsten kann man hier naemlich teleportieren.  kai, nov08
			/* This is done in PersonAgent.chooseNextLink() (see few lines above).
			 * The line here only does the lookup from Link to QueueLink. If one
			 * still wants to have the check, I propose to use
			 * currentQueueLink.getToNode() == nextQueueLink.getFromNode(), which should be
			 * more efficient then looping through all outgoing links.  marcel/04dec2008
			 */

			if (nextQueueLink.hasSpace()) {
				currentLane.popFirstFromBuffer();
				veh.getDriver().incCurrentNode();
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
					currentLane.popFirstFromBuffer();
					Simulation.decLiving();
					Simulation.incLost();
					QueueSimulation.getEvents().processEvent(
							new AgentStuckEvent(now, veh.getDriver().getPerson(), currentLink, veh.getCurrentLeg()));
				} else {
					currentLane.popFirstFromBuffer();
					veh.getDriver().incCurrentNode();
					nextQueueLink.add(veh);
					return true;
				}
			}
			return false;
		}

		// --> nextLink == null
		currentLane.popFirstFromBuffer();
		Simulation.decLiving();
		Simulation.incLost();
		log.error(
				"Agent has no or wrong route! agentId=" + veh.getDriver().getPerson().getId()
						+ " currentLegNumber=" + veh.getCurrentLeg().getNum()
						+ " currentLink=" + currentLink.getId().toString()
						+ ". The agent is removed from the simulation.");
		return true;
	}

	final public void activateNode() {
		this.active = true;
	}

	public final boolean isActive() {
		return this.active;
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
		if (this.signalized) {
			for (QueueLink link : this.inLinksArrayCache){
				for (QueueLane lane : link.getToNodeQueueLanes()) {
					lane.updateGreenState();
					if (lane.isThisTimeStepGreen()){
						this.clearLaneBuffer(lane, now);
					}
				}
			}
		}
		else { // Node is not signal controled -> inLink selection randomized based on capacity
			int inLinksCounter = 0;
			double inLinksCapSum = 0.0;
			// Check all incoming links for buffered agents
			for (QueueLink link : this.inLinksArrayCache) {
//				link.setLanesToGreen(true);
				if (!link.bufferIsEmpty()) {
					this.tempLinks[inLinksCounter] = link;
					inLinksCounter++;
					inLinksCapSum += link.getLink().getCapacity(now);
				}
			}

			if (inLinksCounter == 0) {
				this.active = false;
				return; // Nothing to do
			}

			int auxCounter = 0;
			// randomize based on capacity
			while (auxCounter < inLinksCounter) {
				double rndNum = MatsimRandom.random.nextDouble() * inLinksCapSum;
				double selCap = 0.0;
				for (int i = 0; i < inLinksCounter; i++) {
					QueueLink link = this.tempLinks[i];
					if (link == null)
						continue;
					selCap += link.getLink().getCapacity(now);
					if (selCap >= rndNum) {
//					this.auxLinks[auxCounter] = link;
						auxCounter++;
						inLinksCapSum -= link.getLink().getCapacity(now);
						this.tempLinks[i] = null;
						//move the link
						for (QueueLane lane : link.getToNodeQueueLanes()) {
							this.clearLaneBuffer(lane, now);
						}
						break;
					}
				}
			}
		}
	}
	
	private void clearLaneBuffer(QueueLane lane, double now){
		while (!lane.bufferIsEmpty()) {
			QueueVehicle veh = lane.getFirstFromBuffer();
			if (!moveVehicleOverNode(veh, lane, now)) {
				break;
			}
		}	
	}
	

	public void setSignalized(final boolean b) {
		this.signalized = b;
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
