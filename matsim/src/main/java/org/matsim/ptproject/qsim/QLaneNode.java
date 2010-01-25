/* *********************************************************************** *
 * project: org.matsim.*
 * QLaneNode
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

import java.util.Random;

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
public class QLaneNode extends QNode {
  
  private static final Logger log = Logger.getLogger(QLaneNode.class);
  
  /**
   * @param n
   * @param queueNetwork
   */
  public QLaneNode(Node n, QNetwork queueNetwork) {
    super(n, queueNetwork);
  }
  
  
  @Override
  public void moveNode(final double now, final Random random) {
    /* called by the framework, do all necessary action for node movement here */
    if (this.isSignalized()) {
      for (QLink link : this.inLinksArrayCache){
        for (QLane lane : ((QLinkLanesImpl)link).getToNodeQueueLanes()) {
          lane.updateGreenState(now);
          if (lane.isThisTimeStepGreen()){
            while (!lane.bufferIsEmpty()) {
              QVehicle veh = lane.getFirstFromBuffer();
              if (!moveVehicleOverNode(veh, lane, now)) {
                break;
              }
            }
          }
        }
      }
    }
    else {
      super.moveNode(now, random);
    }
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
  @Override
  protected boolean moveVehicleOverNode(final QVehicle veh, final QLane currentLane, final double now) {
    Id nextLinkId = veh.getDriver().chooseNextLinkId();
    Link currentLink = currentLane.getQueueLink().getLink();

    // veh has to move over node
    if (nextLinkId != null) {
      Link nextLink = this.queueNetwork.getNetworkLayer().getLinks().get(nextLinkId);

      if (currentLink.getToNode() != nextLink.getFromNode()) {
        throw new RuntimeException("Cannot move vehicle " + veh.getId() +
            " from link " + currentLink.getId() + " to link " + nextLinkId);
      }
      if ((!currentLane.isOriginalLane()) && (!currentLane.getDestinationLinkIds().contains(nextLinkId))) {
        StringBuilder b = new StringBuilder();
        b.append("Link Id ");
        b.append(nextLinkId);
        b.append(" is not accessible from lane id ");
        b.append(currentLane.getLaneId());
        b.append(" on Link Id " );
        b.append(currentLink.getId());
        b.append(". Check the definition of the lane and add the link as toLink!");
        log.error(b.toString());
        throw new IllegalStateException(b.toString());
      }
      
      QLink nextQueueLink = this.queueNetwork.getQueueLink(nextLinkId);

      if (nextQueueLink.hasSpace()) {
        currentLane.popFirstFromBuffer();
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
          QSim.getEvents().processEvent(
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


}
