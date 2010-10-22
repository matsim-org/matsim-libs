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

package org.matsim.ptproject.qsim.multimodalsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;

public class MultiModalQNodeExtension {
	
	private static final Logger log = Logger.getLogger(MultiModalQNodeExtension.class);
	
	private final Node node;
	private MultiModalSimEngine simEngine;
	private final MultiModalQLinkExtension[] inLinksArrayCache;
	
	/*
	 * Is set to "true" if the MultiModalNodeExtension has active inLinks.
	 */
	protected boolean isActive = false;
	
	public MultiModalQNodeExtension(Node node, MultiModalSimEngine simEngine) {	
		this.node = node;
		this.simEngine = simEngine;
		
		int nofInLinks = node.getInLinks().size();
		this.inLinksArrayCache = new MultiModalQLinkExtension[nofInLinks];
	}
	
	/*package*/ void init() {
		int i = 0;
		for (Link link : node.getInLinks().values()) {
			NetsimLink qLink = simEngine.getMobsim().getNetsimNetwork().getNetsimLinks().get(link.getId());
			this.inLinksArrayCache[i] = simEngine.getMultiModalQLinkExtension(qLink);
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
		int inLinksCounter = 0;
		for (MultiModalQLinkExtension link : this.inLinksArrayCache) {
			if(link.hasWaitingToLeaveAgents()) {
				inLinksCounter++;
			}
		}

		if (inLinksCounter > 0) {
			this.activateNode();
		}
		else return false;
		
		/*
		 * At the moment we do not simulate capacities in the additional
		 * modes. Therefore we move all agents over the node.
		 */
		for (MultiModalQLinkExtension link : this.inLinksArrayCache) {			
			PersonAgent personAgent = null;
			while ( (personAgent = link.getNextWaitingAgent(now)) != null ) {
				this.moveAgentOverNode(personAgent, now);
			}
		}
		
		return true;
	}
	
	/*
	 * activateNode is protected in QNode.
	 * We add this method because we have to reactivate the Node from MultiModalQLinkImpl
	 */
	/*package*/ void activateNode() {
		this.isActive = true;
		simEngine.activateNode(this);
	}
	
	private void checkNextLinkSemantics(Link currentLink, Link nextLink, PersonAgent personAgent){
		if (currentLink.getToNode() != nextLink.getFromNode()) {
	      throw new RuntimeException("Cannot move PersonAgent " + personAgent.getPerson().getId() +
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
	protected boolean moveAgentOverNode(final PersonAgent personAgent, final double now) {
		
		Id currentLinkId = ((PersonDriverAgent)personAgent).getCurrentLinkId();
		Id nextLinkId = ((PersonDriverAgent)personAgent).chooseNextLinkId();
		
		NetsimLink currentQLink = this.simEngine.getMobsim().getNetsimNetwork().getNetsimLinks().get(currentLinkId);
		Link currentLink = currentQLink.getLink();
		
		if (nextLinkId != null) {
			NetsimLink nextQLink = this.simEngine.getMobsim().getNetsimNetwork().getNetsimLinks().get(nextLinkId);			
			Link nextLink = nextQLink.getLink();
			
			this.checkNextLinkSemantics(currentLink, nextLink, personAgent);
			
			// move Agent over the Node
			((PersonDriverAgent)personAgent).notifyMoveOverNode();
			simEngine.getMultiModalQLinkExtension(nextQLink).addAgentFromIntersection(personAgent, now);
		}
		// --> nextLink == null
		else
		{
			this.simEngine.getMobsim().getAgentCounter().decLiving();
			this.simEngine.getMobsim().getAgentCounter().incLost();
			log.error(
					"Agent has no or wrong route! agentId=" + personAgent.getPerson().getId()
					+ " currentLink=" + currentLink.getId().toString()
					+ ". The agent is removed from the simulation.");			
		}
		return true;
	}
}
