/* *********************************************************************** *
 * project: org.matsim.*
 * CARoute.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;

public class CARoute {
	
	private final List<Id> nodes = new ArrayList<Id>();
	
//    methods        
//    function appendNode(this, nodeId) 
//        this.nodes(end + 1) = nodeId;
//    end
	public void appendNode(Id nodeId) {
		this.nodes.add(nodeId);
	}
	
//    function setRoute(this, nodeIds)
//        this.nodes = nodeIds;
//    end
	public void setRoute(Collection<Id> nodeIds) {
		this.nodes.clear();
		this.nodes.addAll(nodeIds);
	}
	
//    function resetRoute(this) 
//        this.nodes = {};
//    end
	public void resetRoute() {
		this.nodes.clear();
	}
	
//    function [link] = getNextLink(this, currentNode, infrastructure)
//        link = []; 
//        currentNodeId = currentNode.getId();
//        nextNodeId = this.getNextNode(currentNodeId);
//        
//        if (~isempty(nextNodeId))
//            link = infrastructure.getLink(currentNodeId, nextNodeId);
//        end
//    end  
	public CALink getNextLink(Id currentNodeId, NetsimNetwork network) {
		
		CANode currentNode = (CANode) network.getNetsimNode(currentNodeId);
		int index = this.nodes.indexOf(currentNodeId);
		if (index < 0) return null;	// the node is not part of the route
		if (index == this.nodes.size() - 1) return null;	// it is the last node
		Id nextNodeId = this.nodes.get(index + 1);
				
		for (CALink link : currentNode.getFromLinks()) {
			if (nextNodeId.equals(link.getToNode().getId())) return link;
		}
		return null;
	}
	
//    function originNode = getOriginNodeId(this)
//        originNode = this.nodes{1};
//    end
	public Id getOriginNodeId() {
		return this.nodes.get(0);
	}
	
//    function destinationNode = getDestinationNodeId(this)
//        destinationNode = this.nodes{end};
//    end
	public Id getDestinationNodeId() {
		return this.nodes.get(nodes.size() - 1);
	}
	
//    function empty = isempty(this)
//        empty = isempty(this.nodes);
//    end
	public boolean isEmpty() {
		return this.nodes.isEmpty();
	}
// end
//
//methods (Access = private)
//    function [node] = getNextNode(this, currentNodeId) 
//        % working with currentIndex instead of traversing 
//        % route again and again is much faster but overwhelmingly complex!
//        % We have waiting agents at nodes which  neverthless call
//        % getNextLink
//        node = [];
//        for i = 1:(length(this.nodes) - 1)
//            if (strcmp(this.nodes{i}, currentNodeId))
//                node = this.nodes{i + 1};
//            end
//        end
//    end
//end
}
