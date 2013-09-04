/* *********************************************************************** *
 * project: org.matsim.*
 * FlexibleCellNode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.flexiblecells;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;

public class FlexibleCellNode {
	
	private static final Logger log = Logger.getLogger(FlexibleCellNode.class);
	
	private final Node node;
	private FlexibleCellSimEngine simEngine;
	private final FlexibleCellLink[] inLinksArrayCache;
	private final Random random;
	
	/*
	 * Is set to "true" if the CANode has active inLinks.
	 */
	protected AtomicBoolean isActive = new AtomicBoolean(false);
	
	public FlexibleCellNode(Node node, FlexibleCellSimEngine simEngine) {	
		this.node = node;
		this.simEngine = simEngine;
		this.random = MatsimRandom.getLocalInstance();
		
		int nofInLinks = node.getInLinks().size();
		this.inLinksArrayCache = new FlexibleCellLink[nofInLinks];		
	}
	
	/*package*/ void init() {
		int i = 0;
		for (Link link : node.getInLinks().values()) {
			this.inLinksArrayCache[i] = simEngine.getFlexibleCellLink(link.getId());
			i++;
		}
	}
	
	/*package*/ void setMultiModalSimEngine(FlexibleCellSimEngine simEngine) {
		this.simEngine = simEngine;
	}
	
	public boolean moveNode(final double now) {
		/*
		 *  Check all incoming links for WaitingToLeaveAgents
		 *  If waiting Agents are found, we activate the Node.
		 */
		boolean agentsWaitingAtInLinks = false;
		for (FlexibleCellLink link : this.inLinksArrayCache) {
			if(link.hasWaitingToLeaveAgents(now)) {
				agentsWaitingAtInLinks = true;
				break;
			}
		}

		/*
		 * If no agents are waiting to be moved at one of the incoming links,
		 * the node can be deactivated.
		 */
		if (!agentsWaitingAtInLinks) {
			this.isActive.set(false);
			return false;
		}
		
		/*
		 * TODO: add logic that selects links based on their capacity!
		 */
		for (FlexibleCellLink link : this.inLinksArrayCache) {

//			while (link.isAcceptingFromUpstream() && outFlowCapacity > 0 && (context = link.getNextWaitingAgent(now)) != null) {
			while (link.hasWaitingToLeaveAgents(now)) {
				
				// peek next svehicleCell
				VehicleCell vehicleCell = link.getNextWaitingVehicle(now);
				/*
				 * Try moving the agent over the node. This will fail if the agent's
				 * next link has no free space. In this case stop moving agents.
				 */
				if (!this.moveAgentOverNode(vehicleCell, now)) break;
			}
		}
		
		return true;
	}
	
	/*package*/ void activateNode() {
		/*
		 * If isActive is false, then it is set to true ant the
		 * node is activated. Using an AtomicBoolean is thread-safe.
		 * Otherwise, it could be activated multiple times concurrently.
		 */
		if (this.isActive.compareAndSet(false, true)) {
			simEngine.activateNode(this);
		}
	}
	
	private void checkNextLinkSemantics(Link currentLink, Link nextLink, MobsimAgent mobsimAgent){
		if (currentLink.getToNode() != nextLink.getFromNode()) {
	      throw new RuntimeException("Cannot move MobsimAgent " + mobsimAgent.getId() +
	          " from link " + currentLink.getId() + " to link " + nextLink.getId());
	   	}
	}
	
	  // ////////////////////////////////////////////////////////////////////
	  // Queue related movement code
	  // ////////////////////////////////////////////////////////////////////
	  /**
	   * @param personAgent
	   * @param now
	   * @return <code>true</code> if the agent was successfully moved over the node, <code>false</code>
	   * otherwise (e.g. in case the next link is jammed)
	   */
	protected boolean moveAgentOverNode(final VehicleCell vehicleCell, final double now) {
		
		MobsimAgent mobsimAgent = vehicleCell.getMobsimAgent();
		
		Id currentLinkId = mobsimAgent.getCurrentLinkId();
		Id nextLinkId = ((MobsimDriverAgent) mobsimAgent).chooseNextLinkId();
		
		NetsimLink currentQLink = this.simEngine.getMobsim().getNetsimNetwork().getNetsimLinks().get(currentLinkId);
		Link currentLink = currentQLink.getLink();
		
		if (nextLinkId != null) {
			NetsimLink netsimLink = this.simEngine.getMobsim().getNetsimNetwork().getNetsimLinks().get(nextLinkId);
			Link nextLink = netsimLink.getLink();
			
			this.checkNextLinkSemantics(currentLink, nextLink, mobsimAgent);
			
			FlexibleCellLink nextFlexibleCellLink = this.simEngine.getFlexibleCellLink(nextLinkId);
			if (nextFlexibleCellLink.isAcceptingFromUpstream(vehicleCell.getMinLength())) {
				
				// remove VehicleCell object from agent's last link
				FlexibleCellLink currentFlexibleCellLink = this.simEngine.getFlexibleCellLink(currentLinkId);
				
				if (currentFlexibleCellLink.removeTailVehicle() == null) {
					throw new RuntimeException("Moved agent " + vehicleCell.getMobsimAgent().getId() +
							" from link " + currentLinkId +  " over node " + this.node.getId() + 
							" to link " + nextLinkId + " but could not remove it from its from-link. Aborting!");
				}
//				currentFlexibleCellLink.remveAgentContext(mobsimAgent.getId());
						
				// move Agent over the Node
				double remainingLinkTravelDistance = currentLink.getLength() - vehicleCell.getHeadPosition();
				((MobsimDriverAgent) mobsimAgent).notifyMoveOverNode(nextLinkId);
				nextFlexibleCellLink.addAgentFromIntersection(vehicleCell, now, remainingLinkTravelDistance);
				
				// clear agent's cell on last link
//				context.currentCell.reset();
				return true;
			} else return false;
						
		}
		// --> nextLink == null
		else {
			this.simEngine.getMobsim().getAgentCounter().decLiving();
			this.simEngine.getMobsim().getAgentCounter().incLost();
			log.error(
					"Agent has no or wrong route! agentId=" + mobsimAgent.getId()
					+ " currentLink=" + currentLink.getId().toString()
					+ ". The agent is removed from the simulation.");			
			return true;
		}
	}
}
