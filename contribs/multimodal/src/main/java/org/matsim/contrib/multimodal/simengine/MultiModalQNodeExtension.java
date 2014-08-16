/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalQNodeExtension.java
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

package org.matsim.contrib.multimodal.simengine;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

public class MultiModalQNodeExtension {
	
	private static final Logger log = Logger.getLogger(MultiModalQNodeExtension.class);
	
	private MultiModalSimEngine simEngine;
	private final MultiModalQLinkExtension[] inLinksArrayCache;
	
	/*
	 * Is set to "true" if the MultiModalNodeExtension has active inLinks.
	 */
	protected AtomicBoolean isActive = new AtomicBoolean(false);
	
	/*package*/ MultiModalQNodeExtension(MultiModalSimEngine simEngine, int numInLinks) {	
		this.simEngine = simEngine;
		
		this.inLinksArrayCache = new MultiModalQLinkExtension[numInLinks];
	}
	
	/*package*/ void init(Collection<MultiModalQLinkExtension> inLinks) {
		int i = 0;
		for (MultiModalQLinkExtension inLink : inLinks) {
			this.inLinksArrayCache[i] = inLink;
			i++;
		}
	}
	
	/*package*/ void setMultiModalSimEngine(MultiModalSimEngine simEngine) {
		this.simEngine = simEngine;
	}
	
	public boolean moveNode(final double now) {
		/*
		 *  Check all incoming links for WaitingToLeaveAgents
		 *  If waiting Agents are found, we activate the Node.
		 */
		boolean agentsWaitingAtInLinks = false;
		for (MultiModalQLinkExtension link : this.inLinksArrayCache) {
			if(link.hasWaitingToLeaveAgents()) {
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
		for (MultiModalQLinkExtension link : this.inLinksArrayCache) {			
			MobsimAgent personAgent = null;
			while ( (personAgent = link.getNextWaitingAgent(now)) != null ) {
				this.moveAgentOverNode(personAgent, now);
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
	
	private void checkNextLinkSemantics(Link currentLink, Link nextLink, MobsimAgent personAgent){
		if (currentLink.getToNode() != nextLink.getFromNode()) {
	      throw new RuntimeException("Cannot move PersonAgent " + personAgent.getId() +
	          " from link " + currentLink.getId() + " to link " + nextLink.getId());
	   	}
	}
	
	  // ////////////////////////////////////////////////////////////////////
	  // Queue related movement code
	  // ////////////////////////////////////////////////////////////////////
	  /**
	   * @param mobsimAgent
	   * @param now
	   * @return <code>true</code> if the agent was successfully moved over the node, <code>false</code>
	   * otherwise (e.g. in case where the next link is jammed - not yet implemented)
	   */
	protected boolean moveAgentOverNode(final MobsimAgent mobsimAgent, final double now) {
		
		Id currentLinkId = mobsimAgent.getCurrentLinkId();
		Id nextLinkId = ((MobsimDriverAgent) mobsimAgent).chooseNextLinkId();
		
		Link currentLink = this.simEngine.getMultiModalQLinkExtension(currentLinkId).getLink();
		
		if (nextLinkId != null) {
			Link nextLink = this.simEngine.getMultiModalQLinkExtension(nextLinkId).getLink();
			
			this.checkNextLinkSemantics(currentLink, nextLink, mobsimAgent);
			
			// move Agent over the Node
			((MobsimDriverAgent)mobsimAgent).notifyMoveOverNode(nextLinkId);
			simEngine.getMultiModalQLinkExtension(nextLinkId).addAgentFromIntersection(mobsimAgent, now);
		}
		// --> nextLink == null
		else {
			log.error("Agent has no or wrong route! agentId=" + mobsimAgent.getId()
					+ " currentLink=" + currentLink.getId().toString()
					+ ". The agent is removed from the simulation.");
			
			mobsimAgent.setStateToAbort(now);
			this.simEngine.internalInterface.arrangeNextAgentState(mobsimAgent);
		}
		return true;
	}
}