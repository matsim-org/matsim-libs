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

package playground.sergioo.ptsim2013.qnetsimengine;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNode;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

/**
 * Represents a node in the QSimulation.
 */
public class QNode implements NetsimNode {

	private static final Logger log = Logger.getLogger(QNode.class);

	private static final QueueLinkIdComparator qlinkIdComparator = new QueueLinkIdComparator();

	private final PTQLink[] inLinksArrayCache;
	private final PTQLink[] tempLinks;

	private boolean active = false;

	private final Node node;

	// necessary if Nodes are (de)activated
	private NetElementActivator activator = null;

	// for Customizable
	private Map<String, Object> customAttributes = new HashMap<String, Object>();

	private QNetwork network;

	private Random random;

	public QNode(final Node n, final QNetwork network) {
		this.node = n;
		this.network = network; 
		this.activator = network.simEngine;	// by default (single threaded QSim)
		int nofInLinks = this.node.getInLinks().size();
		this.inLinksArrayCache = new PTQLink[nofInLinks];
		this.tempLinks = new PTQLink[nofInLinks];
		this.random = MatsimRandom.getRandom();
		if (network.simEngine.getMobsim().getScenario().getConfig().qsim().getNumberOfThreads() > 1) {
			// This could just as well be the "normal" case. The second alternative
			// is just there so some scenarios / test cases stay
			// "event-file-compatible". Consider removing the second alternative.
			this.random = MatsimRandom.getLocalInstance();
		} else {
			this.random = MatsimRandom.getRandom();
		}
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
			this.inLinksArrayCache[i] = network.getNetsimLinks().get(l.getId());
			i++;
		}
		/* As the order of nodes has an influence on the simulation results,
		 * the nodes are sorted to avoid indeterministic simulations. dg[april08]
		 */
		Arrays.sort(this.inLinksArrayCache, QNode.qlinkIdComparator);
	}

	@Override
	public Node getNode() {
		return this.node;
	}

	/*
	 * The ParallelQSim replaces the activator with the QSimEngineRunner 
	 * that handles this node.
	 */
	/*package*/ void setNetElementActivator(NetElementActivator activator) {
		this.activator = activator;
	}

	/*package*/ final void activateNode() {
		if (!this.active) {
			this.activator.activateNode(this);
			this.active = true;
		}
	}

	final boolean isActive() {
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
	/*package*/ void doSimStep(final double now) {
		
		int inLinksCounter = 0;
		double inLinksCapSum = 0.0;
		// Check all incoming links for buffered agents
		for (PTQLink link : this.inLinksArrayCache) {
			if (!link.isNotOfferingVehicle()) {
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
				PTQLink link = this.tempLinks[i];
				if (link == null)
					continue;
				selCap += link.getLink().getCapacity(now);
				if (selCap >= rndNum) {
					auxCounter++;
					inLinksCapSum -= link.getLink().getCapacity(now);
					this.tempLinks[i] = null;
					//move the link
					this.clearLinkBuffer(link, now);
					break;
				}
			}
		}
	}

	private void clearLinkBuffer(final PTQLink link, final double now){
		while (!link.isNotOfferingVehicle()) {
			QVehicle veh = link.getFirstVehicle();
			if (!moveVehicleOverNode(veh, link, now)) {
				break;
			}
		}
	}


	private void checkNextLinkSemantics(Link currentLink, Id<Link> nextLinkId, PTQLink nextQLink, QVehicle veh){
		if (nextQLink == null){
			throw new IllegalStateException("The link id " + nextLinkId + " is not available in the simulation network, but vehicle " + veh.getId() + " plans to travel on that link from link " + veh.getCurrentLink().getId());
		}
		Link nextLink = nextQLink.getLink();
		if (currentLink.getToNode() != nextLink.getFromNode()) {
			throw new RuntimeException("Cannot move vehicle " + veh.getId() +
					" from link " + currentLink.getId() + " to link " + nextLink.getId());
		}
	}

	// ////////////////////////////////////////////////////////////////////
	// Queue related movement code
	// ////////////////////////////////////////////////////////////////////
	/**
	 * @param veh
	 * @param fromLaneBuffer
	 * @param now
	 * @return <code>true</code> if the vehicle was successfully moved over the node, <code>false</code>
	 * otherwise (e.g. in case where the next link is jammed)
	 */
	private boolean moveVehicleOverNode(final QVehicle veh, final PTQLink fromLaneBuffer, final double now) {
		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		Link currentLink = veh.getCurrentLink();

		if ((!fromLaneBuffer.hasGreenForToLink(nextLinkId))) {
			if (!(vehicleIsStuck(fromLaneBuffer, now))){
				return false;
			}
		}

		if (nextLinkId == null) {
			log.error( "Agent has no or wrong route! agentId=" + veh.getDriver().getId()
					+ " currentLink=" + currentLink.getId().toString()
					+ ". The agent is removed from the simulation.");
			moveVehicleFromInlinkToAbort(veh, fromLaneBuffer, now);
			return true;
		}

		PTQLink nextQueueLink = network.getNetsimLinks().get(nextLinkId);
		this.checkNextLinkSemantics(currentLink, nextLinkId, nextQueueLink, veh);

		if (nextQueueLink.hasSpace()) {
			moveVehicleFromInlinkToOutlink(veh, fromLaneBuffer, nextQueueLink);
			return true;
		}

		if (vehicleIsStuck(fromLaneBuffer, now)) {
			/* We just push the vehicle further after stucktime is over, regardless
			 * of if there is space on the next link or not.. optionally we let them
			 * die here, we have a config setting for that!
			 */
			if (network.simEngine.getMobsim().getScenario().getConfig().qsim().isRemoveStuckVehicles()) {
				moveVehicleFromInlinkToAbort(veh, fromLaneBuffer, now);
				return false ;
			} else {
				moveVehicleFromInlinkToOutlink(veh, fromLaneBuffer, nextQueueLink);
				return true; 
				// (yyyy why is this returning `true'?  Since this is a fix to avoid gridlock, this should proceed in small steps. 
				// kai, feb'12) 
			}
		}

		return false;

	}

	private void moveVehicleFromInlinkToAbort(final QVehicle veh, final PTQLink fromLane, final double now) {
		fromLane.popFirstVehicle();
		veh.getDriver().setStateToAbort(now) ;
		network.simEngine.internalInterface.arrangeNextAgentState(veh.getDriver()) ;
	}

	private void moveVehicleFromInlinkToOutlink(final QVehicle veh, final PTQLink fromLane, PTQLink nextQueueLink) {
		fromLane.popFirstVehicle();
		veh.getDriver().notifyMoveOverNode(nextQueueLink.getLink().getId());
		nextQueueLink.addFromUpstream(veh);
	}

	private boolean vehicleIsStuck(final PTQLink fromLaneBuffer, final double now) {
		return (now - fromLaneBuffer.getLastMovementTimeOfFirstVehicle()) > network.simEngine.getStuckTime();
	}


	protected static class QueueLinkIdComparator implements Comparator<NetsimLink>, Serializable, MatsimComparator {
		private static final long serialVersionUID = 1L;
		@Override
		public int compare(final NetsimLink o1, final NetsimLink o2) {
			return o1.getLink().getId().compareTo(o2.getLink().getId());
		}
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

}
