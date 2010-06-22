/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.sim.network.queueNetwork;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.events.AgentStuckEventImpl;

import playground.mrieser.core.sim.api.SimVehicle;
import playground.mrieser.core.sim.network.api.SimNode;

/*package*/ class QueueNode implements SimNode {

	private final static Logger log = Logger.getLogger(QueueNode.class);

	private final QueueNetwork network;
	private final Node node;
	private final QueueLink[] tempLinks;

	public QueueNode(final Node node, final QueueNetwork network) {
		this.node = node;
		this.network = network;
		this.tempLinks = new QueueLink[node.getInLinks().size()];
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
	/*package*/ final void moveNode(final double now, final Random random) {
		int inLinksCounter = 0;
		double inLinksCapSum = 0.0;
		// Check all incoming links for buffered agents
		for (Id linkId : this.node.getInLinks().keySet()) {
			QueueLink link = this.network.getLinks().get(linkId);
			if (link.buffer.getFirstVehicleInBuffer() != null) {
				this.tempLinks[inLinksCounter] = link;
				inLinksCounter++;
				inLinksCapSum += link.link.getCapacity(now);
			}
		}

		if (inLinksCounter == 0) {
//			this.active = false;
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
				selCap += link.link.getCapacity(now);
				if (selCap >= rndNum) {
					auxCounter++;
					inLinksCapSum -= link.link.getCapacity(now);
					this.tempLinks[i] = null;
					//move the link
					this.clearLinkBuffer(link.buffer, now);
					break;
				}
			}
		}
	}

	private void clearLinkBuffer(final QueueBuffer buffer, final double now) {
		SimVehicle veh;
		while ((veh = buffer.getFirstVehicleInBuffer()) != null) {
      if (!moveVehicleOverNode(veh, buffer, now)) {
        break;
      }
    }
	}

	/**
	 * @param vehicle
	 * @param buffer
	 * @param now
	 * @return <code>true</code> if the vehicle was successfully moved over the node,
	 * 	<code>false</code> otherwise (e.g. in case where the next link is jammed)
	 */
	private boolean moveVehicleOverNode(final SimVehicle vehicle, final QueueBuffer buffer, final double now) {
		Id nextLinkId = vehicle.getDriver().getNextLinkId();

    // veh has to move over node
    if (nextLinkId != null) {
    	QueueLink nextLink = this.network.getLinks().get(nextLinkId);

      this.checkNextLinkSemantics(buffer.link, nextLink, vehicle);
      if (nextLink.hasSpace()) {
        buffer.removeFirstVehicleInBuffer();
        vehicle.getDriver().notifyMoveToNextLink();
        nextLink.addVehicleFromIntersection(vehicle);
        return true;
      }

      // check if veh is stuck!

      if ((now - buffer.getLastMovedTime()) > this.network.getStuckTime()) {
        /* We just push the vehicle further after stucktime is over, regardless
         * of if there is space on the next link or not.. optionally we let them
         * die here, we have a config setting for that!
         */
        if (this.network.isRemoveStuckVehicles()) {
          buffer.removeFirstVehicleInBuffer();
          this.network.simEngine.getEventsManager().processEvent(
              new AgentStuckEventImpl(now, vehicle.getId(), buffer.link.getId(), TransportMode.car));
        } else {
        	buffer.removeFirstVehicleInBuffer();
          vehicle.getDriver().notifyMoveToNextLink();
          nextLink.addVehicleFromIntersection(vehicle);
          return true;
        }
      }
      return false;
    }

    // --> nextLink == null
    buffer.removeFirstVehicleInBuffer();
    log.error(
        "Agent has no or wrong route! vehicleId=" + vehicle.getId()
            + " currentLink=" + buffer.link.getId().toString()
            + ". The agent is removed from the simulation.");
    return true;
	}

	private void checkNextLinkSemantics(final QueueLink currentLink, final QueueLink nextLink, SimVehicle veh){
    if (currentLink.link.getToNode() != nextLink.link.getFromNode()) {
      throw new RuntimeException("Cannot move vehicle " + veh.getId() +
          " from link " + currentLink.getId() + " to link " + nextLink.getId());
    }
	}

	@Override
	public Id getId() {
		return this.node.getId();
	}

}
