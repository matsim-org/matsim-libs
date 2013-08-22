/* *********************************************************************** *
 * project: org.matsim.*
 * CANode.java
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

package playground.christoph.mobsim.ca2;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;

public class CANode {
	
	private static final Logger log = Logger.getLogger(CANode.class);
	
	private final Node node;
	private CASimEngine simEngine;
	private final CALink[] inLinksArrayCache;
	
	/*
	 * Is set to "true" if the CANode has active inLinks.
	 */
	protected AtomicBoolean isActive = new AtomicBoolean(false);
	
	public CANode(Node node, CASimEngine simEngine) {	
		this.node = node;
		this.simEngine = simEngine;
		
		int nofInLinks = node.getInLinks().size();
		this.inLinksArrayCache = new CALink[nofInLinks];
	}
	
	/*package*/ void init() {
		int i = 0;
		for (Link link : node.getInLinks().values()) {
			this.inLinksArrayCache[i] = simEngine.getCALink(link.getId());
			i++;
		}
	}
	
	/*package*/ void setMultiModalSimEngine(CASimEngine simEngine) {
		this.simEngine = simEngine;
	}
	
	public boolean moveNode(final double now) {
		/*
		 *  Check all incoming links for WaitingToLeaveAgents
		 *  If waiting Agents are found, we activate the Node.
		 */
		boolean agentsWaitingAtInLinks = false;
		for (CALink link : this.inLinksArrayCache) {
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
		 * At the moment we do not simulate capacities in the additional
		 * modes. Therefore we move all agents over the node.
		 */
		for (CALink link : this.inLinksArrayCache) {
			AgentMoveOverNodeContext context = null;
			int outFlowCapacity = link.getOutFlowCapacity(now);
//			while (link.isAcceptingFromUpstream() && outFlowCapacity > 0 && (context = link.getNextWaitingAgent(now)) != null) {
			while (outFlowCapacity > 0 && (context = link.getNextWaitingAgent(now)) != null) {
				/*
				 * Try moving the agent over the node. This will fail if the agent's
				 * next link has no free space. In this case stop moving agents.
				 */
				if (this.moveAgentOverNode(context, now)) outFlowCapacity++;
				else break;
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
	   * otherwise (e.g. in case where the next link is jammed - not yet implemented)
	   */
	protected boolean moveAgentOverNode(final AgentMoveOverNodeContext context, final double now) {
		
		MobsimAgent mobsimAgent = context.mobsimAgent;
		Id currentLinkId = mobsimAgent.getCurrentLinkId();
		Id nextLinkId = ((MobsimDriverAgent) mobsimAgent).chooseNextLinkId();
		
		NetsimLink currentQLink = this.simEngine.getMobsim().getNetsimNetwork().getNetsimLinks().get(currentLinkId);
		Link currentLink = currentQLink.getLink();
		
		if (nextLinkId != null) {
			NetsimLink nextQLink = this.simEngine.getMobsim().getNetsimNetwork().getNetsimLinks().get(nextLinkId);
			Link nextLink = nextQLink.getLink();
			
			this.checkNextLinkSemantics(currentLink, nextLink, mobsimAgent);
			
			CALink nextCALink = this.simEngine.getCALink(nextLinkId);
			if (nextCALink.isAcceptingFromUpstream()) {
				// move Agent over the Node
				((MobsimDriverAgent) mobsimAgent).notifyMoveOverNode(nextLinkId);
				nextCALink.addAgentFromIntersection(context, now);
				
				// remove AgentContext object from agent's last link
				CALink currentCALink = this.simEngine.getCALink(currentLinkId);
				currentCALink.remveAgentContext(mobsimAgent.getId());
				
				// clear agent's cell on last link
				context.currentCell.reset();
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
