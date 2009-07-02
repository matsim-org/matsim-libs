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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

public class GetAllIncludedLinks {

	/**
	 * Returns an ArrayList of Links.
	 * @param NetworkLayer network
	 * @param ArrayList< Node > includedNodes
	 *  
	 * @return A link from the network is included in the returned ArrayList, if its
	 * start- and end node are included in the includedNodes ArrayList.
	 */
	public ArrayList<LinkImpl> getAllLinks(NetworkLayer network, ArrayList<NodeImpl> includedNodes)
	{
		ArrayList<LinkImpl> includedLinks = new ArrayList<LinkImpl>();
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
	public ArrayList<LinkImpl> getAllLinks(NetworkLayer network, Map<Id, NodeImpl> includedNodesMap)
	{
		ArrayList<LinkImpl> includedLinks = new ArrayList<LinkImpl>();
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
	public void getAllLinks(NetworkLayer network, ArrayList<NodeImpl> includedNodes, ArrayList<LinkImpl> includedLinks)
	{		
		// get all links of the network
		Map<Id, LinkImpl> linkMap = network.getLinks();
		
		for (LinkImpl link : linkMap.values()) 
		{
			NodeImpl fromNode = link.getFromNode();
			NodeImpl toNode = link.getToNode();
			
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
	public void getAllLinks(NetworkLayer network, Map<Id, NodeImpl> includedNodesMap, ArrayList<LinkImpl> includedLinks)
	{	
		// get all links of the network
		Map<Id, LinkImpl> linkMap = network.getLinks();
		
		for (LinkImpl link : linkMap.values()) 
		{
			NodeImpl fromNode = link.getFromNode();
			NodeImpl toNode = link.getToNode();
			
			// check, if the node is contained in the given list
			if(includedNodesMap.containsKey(fromNode.getId()) && includedNodesMap.containsKey(toNode.getId()))
			{
				//... both nodes contained -> link contained -> add link to list
				includedLinks.add(link);
			}		
			
		}
		
	} //return includedLinks;
	
}