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

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.events.AgentStuckEventImpl;

import playground.mrieser.core.mobsim.api.MobsimVehicle;
import playground.mrieser.core.mobsim.network.api.MobsimNode2;

/*package*/ class QueueNode implements MobsimNode2 {

	private final static Logger log = Logger.getLogger(QueueNode.class);

	private final static QueueLinkIdComparator linkIdComparator = new QueueLinkIdComparator();

	private final QueueNetwork network;
	/*package*/ final Node node;
	private final QueueLink[] inLinks;
	private final QueueLink[] tempLinks;
	private final Random random;
	private boolean active = false;
	private Operator operator;

	public QueueNode(final Node node, final QueueNetwork network, final Operator operator, final Random random) {
		this.node = node;
		this.network = network;
		this.operator = operator;
		this.random = random;
		this.inLinks = new QueueLink[node.getInLinks().size()];
		this.tempLinks = new QueueLink[node.getInLinks().size()];
		int idx = 0;
		for (Id linkId : this.node.getInLinks().keySet()) {
			this.inLinks[idx] = this.network.getLinks().get(linkId);
			idx++;
		}
		Arrays.sort(this.inLinks, linkIdComparator); // make it deterministic
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
	/*package*/ final void moveNode(final double now) {
		int inLinksCounter = 0;
		double inLinksCapSum = 0.0;
		// Check all incoming links for buffered agents
		for (QueueLink link : this.inLinks) {
			if (link.buffer.getFirstVehicleInBuffer() != null) {
				this.tempLinks[inLinksCounter] = link;
				inLinksCounter++;
				inLinksCapSum += link.link.getCapacity(now);
			}
		}

		if (inLinksCounter == 0) {
			return; // Nothing to do
		}

		int auxCounter = 0;
		// randomize based on capacity
		while (auxCounter < inLinksCounter) {
			double rnd = this.random.nextDouble();
//			System.out.println(now + "  NODE " + this.node.getId() + "  RND " + rnd);
			double rndNum = rnd * inLinksCapSum;
			double selCap = 0.0;
			for (int i = 0; i < inLinksCounter; i++) {
				QueueLink link = this.tempLinks[i];
				if (link == null) {
					continue;
				}
//				System.out.println("          LINK " + link.link.getId() + "  CAP " + link.link.getCapacity());
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
		MobsimVehicle veh;
		while ((veh = buffer.getFirstVehicleInBuffer()) != null) {
      if (!moveVehicleOverNode(veh, buffer, now)) {
//      	System.out.println("       MOVE failed.");
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
	private boolean moveVehicleOverNode(final MobsimVehicle vehicle, final QueueBuffer buffer, final double now) {
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
        	buffer.link.removeVehicle(vehicle);
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

	private void checkNextLinkSemantics(final QueueLink currentLink, final QueueLink nextLink, final MobsimVehicle veh){
    if (currentLink.link.getToNode() != nextLink.link.getFromNode()) {
      throw new RuntimeException("Cannot move vehicle " + veh.getId() +
          " from link " + currentLink.getId() + " to link " + nextLink.getId());
    }
	}

	@Override
	public Id getId() {
		return this.node.getId();
	}

	protected static class QueueLinkIdComparator implements Comparator<QueueLink>, Serializable {
		private static final long serialVersionUID = 1L;
		@Override
		public int compare(final QueueLink o1, final QueueLink o2) {
			return o1.link.getId().compareTo(o2.link.getId());
		}
	}

	/*package*/ boolean isActive() {
		boolean a = false;
		for (QueueLink link : this.inLinks) {
			if (link.buffer.getFirstVehicleInBuffer() != null) {
				a = true;
				break;
			}
		}
		this.active = a;
		return this.active;
	}

	/*package*/ void activate() {
		if (!this.active) {
			this.active = true;
			this.operator.activateNode(this);
		}
	}

	/*package*/ void setOperator(final Operator operator) {
		this.operator = operator;
	}
}
