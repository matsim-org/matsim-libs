/* *********************************************************************** *
 * project: org.matsim.*
 * QLinkNode
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
package org.matsim.ptproject.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.gbl.Gbl;


/**
 * @author dgrether
 *
 */
public class QLinkNode extends QueueNode {
  
  private static final Logger log = Logger.getLogger(QLinkNode.class);
  /**
   * @param n
   * @param queueNetwork
   */
  public QLinkNode(Node n, QueueNetwork queueNetwork) {
    super(n, queueNetwork);
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
  protected boolean moveVehicleOverNode(final QueueVehicle veh, final QueueLinkImpl currentLane, final double now) {
    Id nextLinkId = veh.getDriver().chooseNextLinkId();
    Link currentLink = currentLane.getLink();

    // veh has to move over node
    if (nextLinkId != null) {
      Link nextLink = this.queueNetwork.getNetworkLayer().getLinks().get(nextLinkId);

      if (currentLink.getToNode() != nextLink.getFromNode()) {
        throw new RuntimeException("Cannot move vehicle " + veh.getId() +
            " from link " + currentLink.getId() + " to link " + nextLinkId);
      }
      
      QueueLink nextQueueLink = this.queueNetwork.getQueueLink(nextLinkId);

      if (nextQueueLink.hasSpace()) {
        (currentLane).popFirstFromBuffer();
        veh.getDriver().moveOverNode();
        nextQueueLink.add(veh);
        return true;
      }

      // check if veh is stuck!

      if ((now - currentLane.bufferLastMovedTime) > Simulation.getStuckTime()) {
        /* We just push the vehicle further after stucktime is over, regardless
         * of if there is space on the next link or not.. optionally we let them
         * die here, we have a config setting for that!
         */
        if (Gbl.getConfig().getQSimConfigGroup().isRemoveStuckVehicles()) {
          currentLane.popFirstFromBuffer();
          Simulation.decLiving();
          Simulation.incLost();
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
    Simulation.decLiving();
    Simulation.incLost();
    log.error(
        "Agent has no or wrong route! agentId=" + veh.getDriver().getPerson().getId()
            + " currentLink=" + currentLink.getId().toString()
            + ". The agent is removed from the simulation.");
    return true;
  }

  @Override
  protected void clearLaneBuffer(QueueLane lane, double now) {
    throw new UnsupportedOperationException("wrong instance!");    
  }

  @Override
  protected void clearLinkBuffer(final QueueLinkImpl lane, final double now){
    while (!lane.bufferIsEmpty()) {
      QueueVehicle veh = lane.getFirstFromBuffer();
      if (!moveVehicleOverNode(veh, lane, now)) {
        break;
      }
    }
  }
}
