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

package org.matsim.core.mobsim.queuesim;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.gbl.Gbl;

/**
 * Represents a node in the QueueSimulation.
 */
public class QueueNode {

	private static final Logger log = Logger.getLogger(QueueNode.class);

	private static final QueueLinkIdComparator qlinkIdComparator = new QueueLinkIdComparator();

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
		Arrays.sort(this.inLinksArrayCache, QueueNode.qlinkIdComparator);
	}

	public Node getNode() {
		return this.node;
	}

	// ////////////////////////////////////////////////////////////////////
	// Queue related movement code
	// ////////////////////////////////////////////////////////////////////
	/**
	 * @param veh
	 * @param currentLane
	 * @param now
	 * @return <code>true</code> if the vehicle was successfully moved over the node, <code>false</code>
	 * otherwise (e.g. in case where the next link is jammed)
	 */
	protected boolean moveVehicleOverNode(final QueueVehicle veh, final QueueLane currentLane, final double now) {
		Link nextLink = veh.getDriver().chooseNextLink();
		Link currentLink = currentLane.queueLink.getLink();

		// veh has to move over node
		if (nextLink != null) {

			if (currentLink.getToNode() != nextLink.getFromNode()) {
				throw new RuntimeException("Cannot move vehicle " + veh.getId() +
						" from link " + currentLink.getId() + " to link " + nextLink.getId());
			}
			if ((!currentLane.isOriginalLane()) && (!currentLane.getDestinationLinks().contains(nextLink))) {
				StringBuilder b = new StringBuilder();
				b.append("Link Id ");
				b.append(nextLink.getId());
				b.append(" is not accessible from lane id ");
				b.append(currentLane.getLaneId());
				b.append(" on Link Id " );
				b.append(currentLink.getId());
				b.append(". Check the definition of the lane and add the link as toLink!");
				log.error(b.toString());
				throw new IllegalStateException(b.toString());
			}
			
			QueueLink nextQueueLink = this.queueNetwork.getQueueLink(nextLink.getId());

			if (nextQueueLink.hasSpace()) {
				currentLane.popFirstFromBuffer();
				veh.getDriver().moveOverNode();
				nextQueueLink.add(veh);
				return true;
			}

			// check if veh is stuck!

			if ((now - currentLane.bufferLastMovedTime) > AbstractSimulation.getStuckTime()) {
				/* We just push the vehicle further after stucktime is over, regardless
				 * of if there is space on the next link or not.. optionally we let them
				 * die here, we have a config setting for that!
				 */
				if (Gbl.getConfig().simulation().isRemoveStuckVehicles()) {
					currentLane.popFirstFromBuffer();
					AbstractSimulation.decLiving();
					AbstractSimulation.incLost();
					QueueSimulation.getEvents().processEvent(
							new AgentStuckEventImpl(now, veh.getDriver().getPerson().getId(), currentLink.getId(), veh.getDriver().getCurrentLeg().getMode()));
				} else {
					currentLane.popFirstFromBuffer();
					veh.getDriver().moveOverNode();
					nextQueueLink.add(veh);
					return true;
				}
			}
			return false;
		}

		// --> nextLink == null
		currentLane.popFirstFromBuffer();
		AbstractSimulation.decLiving();
		AbstractSimulation.incLost();
		log.error(
				"Agent has no or wrong route! agentId=" + veh.getDriver().getPerson().getId()
						+ " currentLink=" + currentLink.getId().toString()
						+ ". The agent is removed from the simulation.");
		return true;
	}

	protected final void activateNode() {
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
	 * @param random the random number generator to be used
	 */
	public void moveNode(final double now, final Random random) {
		/* called by the framework, do all necessary action for node movement here */
		if (this.signalized) {
			for (QueueLink link : this.inLinksArrayCache){
				for (QueueLane lane : link.getToNodeQueueLanes()) {
					lane.updateGreenState(now);
					if (lane.isThisTimeStepGreen()){
						this.clearLaneBuffer(lane, now);
					}
				}
			}
		}
		else { // Node is not signal controlled -> inLink selection randomized based on capacity
			int inLinksCounter = 0;
			double inLinksCapSum = 0.0;
			// Check all incoming links for buffered agents
			for (QueueLink link : this.inLinksArrayCache) {
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
				double rndNum = random.nextDouble() * inLinksCapSum;
				double selCap = 0.0;
				for (int i = 0; i < inLinksCounter; i++) {
					QueueLink link = this.tempLinks[i];
					if (link == null)
						continue;
					selCap += link.getLink().getCapacity(now);
					if (selCap >= rndNum) {
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

	private void clearLaneBuffer(final QueueLane lane, final double now){
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

	public boolean isSignalized(){
		return this.signalized;
	}

	protected static class QueueLinkIdComparator implements Comparator<QueueLink>, Serializable {
		private static final long serialVersionUID = 1L;
		public int compare(final QueueLink o1, final QueueLink o2) {
			return o1.getLink().getId().compareTo(o2.getLink().getId());
		}
	}

}
