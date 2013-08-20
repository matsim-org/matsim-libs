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

package playground.christoph.mobsim.ca;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNode;

public class CANode implements NetsimNode {

	final private static Logger log = Logger.getLogger(CANode.class);
	
	private final Node node;
	private final CANetwork network;
	private final CALink[] inLinks;
	private final CALink[] outLinks;
	
	private CAAgent agent; 
	
	public CANode(Node node, CANetwork network) {
		this.node = node;
		this.network = network;
		
		this.inLinks = new CALink[node.getInLinks().size()];
		this.outLinks = new CALink[node.getOutLinks().size()];
		
		this.agent = null;
	}
	
	/**
	 * Loads the inLinks-array with the corresponding links.
	 * Cannot be called in constructor, as the queueNetwork does not yet know
	 * the queueLinks. Should be called by QueueNetwork, after creating all
	 * QueueNodes and QueueLinks.
	 */
	/*package*/ void init() {
		int i = 0;
		for (Link l : this.node.getInLinks().values()) {
			this.inLinks[i] = (CALink) this.network.getNetsimLinks().get(l.getId());
			i++;
		}
		i = 0;
		for (Link l : this.node.getOutLinks().values()) {
			this.outLinks[i] = (CALink) this.network.getNetsimLinks().get(l.getId());
			i++;
		}

		/* As the order of nodes has an influence on the simulation results,
		 * the nodes are sorted to avoid indeterministic simulations. dg[april08]
		 */
		LinkIdComparator comparator = new LinkIdComparator();
		Arrays.sort(this.inLinks, comparator);
		Arrays.sort(this.outLinks, comparator);
	}
	
	protected static class LinkIdComparator implements Comparator<NetsimLink>, Serializable, MatsimComparator {
		private static final long serialVersionUID = 1L;
		@Override
		public int compare(final NetsimLink o1, final NetsimLink o2) {
			return o1.getLink().getId().compareTo(o2.getLink().getId());
		}
	}
	
//    properties (Access = private)
//    toLinks = NLink.empty(0,0);      % links to this node
//    fromLinks = NLink.empty(0,0);    % links away from this node
//    currentAgent;                   % agent, which is currently on the intersection
//end
//
//methods
//    function this = NNode(id, position_x, position_y)
//        this = this@SpatialElement(id, position_x, position_y);
//    end  
//    
//    function [hasAgent] = hasAgent(this)
//        hasAgent = ~isempty(this.currentAgent);
//    end
	public boolean hasAgent() {
		return this.agent != null;
	}

//    function addToLink(this, link)
//        this.toLinks(end + 1) = link;
//    end
    
//    function addFromLink(this, link)
//        this.fromLinks(end + 1) = link;
//    end
    
//    function setAgent(this, agent)
//        this.currentAgent = agent;
//    end
	public void setAgent(CAAgent agent) {
		if (agent == null && this.agent != null) {
			log.info("Agent " + this.agent.getId() + " leaves node " + this.getId());
		} else if (agent != null) {
			log.info("Agent " + agent.getId() + " enters node " + this.getId());
		}
		this.agent = agent;
	}
    
//    function [agent] = getAgent(this)
//        agent = this.currentAgent;
//    end
    public CAAgent getAgent() {
    	return this.agent;
    }
	
//    function [toLinks] = getToLinks(this)
//        toLinks = this.toLinks;
//    end
    public CALink[] getToLinks() {
    	return this.inLinks;
    }
    
//    function [fromLinks] = getFromLinks(this)
//        fromLinks = this.fromLinks;
//    end
    public CALink[] getFromLinks() {
    	return this.outLinks;
    }
	
	@Override
	public Map<String, Object> getCustomAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node getNode() {
		return this.node;
	}
	
	public Coord getCoord() {
		return this.node.getCoord();
	}
	
	public Id getId() {
		return this.node.getId();
	}
}