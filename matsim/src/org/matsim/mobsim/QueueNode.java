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

package org.matsim.mobsim;

import org.matsim.events.EventAgentNoRoute;
import org.matsim.events.EventAgentStuck;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.network.Node;

//////////////////////////////////////////////////////////////////////
// QueueNode represents a node in the QueueSimulation
//////////////////////////////////////////////////////////////////////

public class QueueNode extends Node {

	private boolean cacheIsInvalid = true;

	private QueueLink[] inLinksArrayCache = null;
	private QueueLink[] tempLinks = null;
	private QueueLink[] auxLinks = null;

	private boolean active = false;

	/* Get/Set partitionID generated be MetisExeWrapper
	 * only used when doing DistributedSimulation,
	 * but I did not want to inherit for just this info */
	private int partitionId = 0;
	public int getPartitionId() { return this.partitionId; }
	public void setPartitionId(final int partitionId) { this.partitionId = partitionId; }

	//////////////////////////////////////////////////////////////////////
	//constructor
	//////////////////////////////////////////////////////////////////////
	protected QueueNode(final String id, final String x, final String y, final String type) {
		super(id, x, y, type);
	}

	private void buildCache() {
		this.inLinksArrayCache = new QueueLink[this.inlinks.size()];
		this.inLinksArrayCache = this.inlinks.values().toArray(this.inLinksArrayCache);
		this.tempLinks = new QueueLink[this.inlinks.size()];
		this.auxLinks = new QueueLink[this.inlinks.size()];
		this.cacheIsInvalid = false;
	}

	@Override
	public boolean addInLink(final BasicLinkI inlink) {
		this.cacheIsInvalid = true;
		return super.addInLink(inlink);
	}

	//////////////////////////////////////////////////////////////////////
	// Queue related movement code
	//////////////////////////////////////////////////////////////////////
	public boolean moveVehicleOverNode(final Vehicle veh, final double now) {
		QueueLink currentLink = veh.getCurrentLink();
		// veh has to move over node
		QueueLink nextLink = veh.chooseNextLink();

		if (nextLink != null) {
			if (nextLink.hasSpace()) {
				currentLink.popFirstFromBuffer();
				veh.incCurrentNode();
				nextLink.add(veh);
				return true;
			}

			// check if veh is stuck!

			if ((now - veh.getLastMovedTime()) > Simulation.getStuckTime()) {
				/* We just push the vehicle further after stucktime is over,
				 * regardless of if there is space on the next link or not..
				 * optionally we let them die here, we have a config setting for that!
				 */
				if (removeStuckVehicle()) {
					currentLink.popFirstFromBuffer();
					Simulation.decLiving();
					Simulation.incLost();
					QueueSimulation.getEvents().processEvent (new EventAgentStuck(now, veh.getDriverID(), veh.getCurrentLegNumber(), veh.getCurrentLink().getId().toString(), veh.getDriver(), veh.getCurrentLeg(), veh.getCurrentLink()));
				} else {
					currentLink.popFirstFromBuffer();
					veh.incCurrentNode();
					nextLink.add(veh);
					return true;
				}
			}
			return false;
		}

		currentLink.popFirstFromBuffer();
		Simulation.decLiving();
		Simulation.incLost();
		QueueSimulation.getEvents().processEvent (new EventAgentNoRoute(now, veh.getDriverID(), veh.getCurrentLegNumber(), veh.getCurrentLink().getId().toString(), veh.getDriver(), veh.getCurrentLeg(), veh.getCurrentLink()));
		return true;
	}

	final public void activateNode() { this.active = true; }

	/**
	 * Moves vehicles from the inlinks' buffer to the outlinks where possible.<br>
	 * The inLinks are randomly chosen, and for each link all vehicles in the
	 * buffer are moved to their desired outLink as long as there is space. If
	 * the front most vehicle in a buffer cannot move across the node because
	 * there is no free space on its destination link, the work on this inLink is
	 * finished and the next inLink's buffer is handled (this means, that at the
	 * node, all links have only like one lane, and there are no separate lanes
	 * for the different outLinks. Thus if the front most vehicle cannot drive
	 * further, all other vehicles behind must wait, too, even if their links
	 * would be free).
	 *
	 * @param now The current time in seconds from midnight.
	 */
	public void moveNode(final double now) {
		/* called by the framework, do all necessary action for node movement here */

		if (!this.active) {
			return;
		}

		if (this.cacheIsInvalid) {
			buildCache();
		}

		int tempCounter = 0;
		double tempCap = 0.0;
		// Check all incoming links for buffered agents
		for (QueueLink link : this.inLinksArrayCache) {
			if (!link.bufferIsEmpty()) {
				this.tempLinks[tempCounter] = link;
				tempCounter++;
				tempCap += link.getCapacity();
			}
		}

		if (tempCounter == 0) {
			this.active = false;
			return; // Nothing to do
		}

		int auxCounter = 0;
		// randomize based on capacity
		while (auxCounter < tempCounter) {
			double rndNum = Gbl.random.nextDouble() * tempCap;
			double selCap = 0.0;
			for (int i = 0; i < tempCounter; i++) {
				QueueLink link = this.tempLinks[i];
				if (link == null) continue;
				selCap += link.getCapacity();
				if ( selCap >= rndNum ) {
					this.auxLinks[auxCounter] = link;
					auxCounter++;
					tempCap -= link.getCapacity();
					this.tempLinks[i] = null;
					break ;
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
