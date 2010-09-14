/* *********************************************************************** *
 * project: org.matsim.*
 * GetAllNodes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * @author Christoph Dobler
 * 
 * Liefert eine ArrayList von Links.
 * �bergabeparameter muss ein Netzwerk und eine ArrayList von Nodes sein.
 * Zur�ckgegeben wird eine ArrayList von Links, welche dem Netzwerk angeh�ren
 * und deren Start- und Endknoten in der �bergebenen ArrayList enthalten sind. 
 * Wird zus�tzlich noch eine ArrayList Links mit �bergeben, so wird diese
 * mit den neu gefundenen Links erweitert. Andernfalls wird eine neue erstellt.
 *
 */

package playground.christoph.knowledge.utils;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

public class GetAllIncludedLinks {

	/**
	 * Returns an ArrayList of Links.
	 * @param NetworkLayer network
	 * @param ArrayList< Node > includedNodes
	 *  
	 * @return A link from the network is included in the returned ArrayList, if its
	 * start- and end node are included in the includedNodes ArrayList.
	 */
	public ArrayList<Link> getAllLinks(Network network, ArrayList<Node> includedNodes)
	{
		ArrayList<Link> includedLinks = new ArrayList<Link>();
		getAllLinks(network, includedNodes, includedLinks);
		return includedLinks;
	}

	/**
	 * Returns an ArrayList of Links.
	 * @param NetworkLayer network
	 * @param Map< Id, Node > includedNodesMap
	 *  
	 * @return A link from the network is included in the returned ArrayList, if its
	 * start- and end node are included in the includedNodes ArrayList.
	 */
	public ArrayList<Link> getAllLinks(NetworkImpl network, Map<Id, Node> includedNodesMap)
	{
		ArrayList<Link> includedLinks = new ArrayList<Link>();
		getAllLinks(network, includedNodesMap, includedLinks);
		return includedLinks;
	}
	
	/**
	 * A link from the network is added to the includedLinks ArrayList, if its
	 * start- and end node are included in the includedNodes ArrayList.
	 *  
	 * @param NetworkLayer network
	 * @param ArrayList< Node > includedNodes
	 * @param ArrayList< Link > includedLinks
	 */
	public void getAllLinks(Network network, ArrayList<Node> includedNodes, ArrayList<Link> includedLinks)
	{		
		// get all links of the network
		Map<Id, ? extends Link> linkMap = network.getLinks();
		
		for (Link link : linkMap.values()) 
		{
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
			
			// check, if the node is contained in the given list
			if(includedNodes.contains(fromNode) && includedNodes.contains(toNode))
			{
				//... both nodes contained -> link contained -> add link to list
				includedLinks.add(link);
			}		
			
		}		
		//return includedLinks;
	}
	
	/**
	 * A link from the network is added to the includedLinks ArrayList, if its
	 * start- and end node are included in the includedNodes ArrayList.
	 *  
	 * @param NetworkLayer network
	 * @param Map< Id, Node > includedNodesMap
	 * @param ArrayList< Link > includedLinks
	 */
	public void getAllLinks(NetworkImpl network, Map<Id, Node> includedNodesMap, ArrayList<Link> includedLinks)
	{	
		// get all links of the network
		Map<Id, ? extends Link> linkMap = network.getLinks();
		
		for (Link link : linkMap.values()) 
		{
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
			
			// check, if the node is contained in the given list
			if(includedNodesMap.containsKey(fromNode.getId()) && includedNodesMap.containsKey(toNode.getId()))
			{
				//... both nodes contained -> link contained -> add link to list
				includedLinks.add(link);
			}		
			
		}
		
	} //return includedLinks;
	
}