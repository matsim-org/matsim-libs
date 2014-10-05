/* *********************************************************************** *
 * project: org.matsim.*
 * CANetwork.java
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNode;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.VisLink;

// Infrastructure
public class CANetwork implements NetsimNetwork {

	private final Network network;
	private final CANetworkFactory networkFactory; 
	private final Map<Id<Node>, CANode> nodes;
	private final Map<Id<Link>, CALink> links;
	
	private CA ca;
	
	public CANetwork(Network network, CANetworkFactory networkFactory) {
		this.network = network;
		this.networkFactory = networkFactory;
		
		this.nodes = new HashMap<>();
		this.links = new HashMap<>();
	}
	
	public void initialize(CA ca) {
		this.ca = ca;
		for (Node n : network.getNodes().values()) {
			this.nodes.put(n.getId(), this.networkFactory.createNetsimNode(n, this));
		}
		for (Link l : network.getLinks().values()) {
			this.links.put(l.getId(), this.networkFactory.createNetsimLink(l, this, this.nodes.get(l.getToNode().getId())));
		}
		for (CANode n : this.nodes.values()) {
			n.init();
		}
	}
	
	@Override
	public Map<Id<Link>, ? extends VisLink> getVisLinks() {
		return null;
	}

	@Override
	public AgentSnapshotInfoFactory getAgentSnapshotInfoFactory() {
		return null;
	}

	@Override
	public Network getNetwork() {
		return this.network;
	}

	@Override
	public Map<Id<Link>, ? extends NetsimLink> getNetsimLinks() {
		return this.links;
	}

	@Override
	public Map<Id<Node>, ? extends NetsimNode> getNetsimNodes() {
		return this.nodes;
	}

	// getLinkById(id)
	@Override
	public NetsimLink getNetsimLink(Id<Link> id) {
		return this.links.get(id);
	}

	// getNodeById(id)
	@Override
	public NetsimNode getNetsimNode(Id<Node> id) {
		return this.nodes.get(id);
	}
	
//	 function this = Infrastructure(nodes, links)
//	            this.nodes = nodes;
//	            this.links = links;
//	        end
//	        
//	        function setNodeById(this, id, node)
//	            this.nodes(id) = node;
//	        end
//	        
//	        function [link] = getLink(this, startNodeId, endNodeId)
//	            % extract link from infrastructure by start and end node
//	            %
//	            % startNode -------------------------------------> endNode
//	            %              l = startNode.getFromLinks()[i]
//	            %                                                  l.getToNode()
//	            endNode = this.getNodeById(endNodeId);
//	            startNode = this.getNodeById(startNodeId);
//	            fromLinks = startNode.getFromLinks();
//	            
//	            lks = [];
//	            for i = 1:length(fromLinks) % search the link in fromlinks which has endNode as toNode
//	                l = fromLinks(i);
//	                if (l.getToNode() == endNode)
//	                    lks = [l lks];
//	                end
//	            end
//	            link = lks(randi(numel(lks)));
//	        end
//	        
//	        function setParkingProperties(this, nrLots, totSize)
//	            this.nrParkingLots = nrLots;
//	            this.totalParkingSpaces = totSize;
//	        end
//	        
//	        function [nrParkingLots, totParkingSpaces] = getParkingProperties(this)
//	            nrParkingLots = this.nrParkingLots;
//	            totParkingSpaces = this.totalParkingSpaces;
//	        end
//	        
//	    end

}
