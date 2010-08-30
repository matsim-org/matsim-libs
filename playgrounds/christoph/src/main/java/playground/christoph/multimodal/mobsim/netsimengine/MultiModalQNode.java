/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalQNode.java
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

package playground.christoph.multimodal.mobsim.netsimengine;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.ptproject.qsim.interfaces.QSimEngine;
import org.matsim.ptproject.qsim.netsimengine.QLinkInternalI;
import org.matsim.ptproject.qsim.netsimengine.QNode;

public class MultiModalQNode extends QNode{
	
	private static final Logger log = Logger.getLogger(MultiModalQNode.class);
	
	private QSimEngine qSimEngine;
	
	public MultiModalQNode(Node n, QSimEngine simEngine) {
		super(n, simEngine);
		
		this.qSimEngine = simEngine;
	}

	@Override
	public void moveNode(final double now, final Random random) {
		
		super.moveNode(now, random);
		
		/*
		 *  Check all incoming links for WaitingToLeaveAgents
		 *  If waiting Agents are found, we activate the Node - if not,
		 *  we do nothing (the super class decides whether the Node
		 *  should be active or not).
		 */
		int inLinksCounter = 0;
		for (QLinkInternalI link : this.inLinksArrayCache) {
			if(((MultiModalQLinkImpl) link).hasWaitingToLeaveAgents()) {
				inLinksCounter++;
			}
		}

		if (inLinksCounter > 0) {
			this.activateNode();
//			return; // Nothing to do
		}
		
		/*
		 * At the moment we do not simulate capacities in the additional
		 * modes. Therefore we move all agents over the node.
		 */
		for (QLinkInternalI link : this.inLinksArrayCache)
		{			
			PersonAgent personAgent = null;
			while ( (personAgent = ((MultiModalQLinkImpl) link).getNextWaitingAgent(now)) != null ) {
				this.moveAgentOverNode(personAgent, now);
			}
		}
	}
	
	/*
	 * activateNode is protected in QNode.
	 * We add this method because we have to reactivate the Node from MultiModalQLinkImpl
	 */
	/* package */ void activiateNode() {
		this.activateNode();
	}
	
	protected void checkNextLinkSemantics(Link currentLink, Link nextLink, PersonAgent personAgent){
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

		QLinkInternalI currentQLink = this.qSimEngine.getQSim().getQNetwork().getLinks().get(currentLinkId);
		Link currentLink = currentQLink.getLink();
		
		if (nextLinkId != null) {
			QLinkInternalI nextQLink = this.qSimEngine.getQSim().getQNetwork().getLinks().get(nextLinkId);			
			Link nextLink = nextQLink.getLink();
			
			this.checkNextLinkSemantics(currentLink, nextLink, personAgent);
			
			// move Agent over the Node
			((PersonDriverAgent)personAgent).notifyMoveOverNode();
			
			((MultiModalQLinkImpl)nextQLink).addAgentFromIntersection(personAgent, now);
	    }
		// --> nextLink == null
		else
		{
			this.qSimEngine.getQSim().getAgentCounter().decLiving();
			this.qSimEngine.getQSim().getAgentCounter().incLost();
			log.error(
					"Agent has no or wrong route! agentId=" + personAgent.getPerson().getId()
					+ " currentLink=" + currentLink.getId().toString()
					+ ". The agent is removed from the simulation.");			
		}
	    return true;
	  }

}
