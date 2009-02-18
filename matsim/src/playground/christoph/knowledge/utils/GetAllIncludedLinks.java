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
 * Übergabeparameter muss ein Netzwerk und eine ArrayList von Nodes sein.
 * Zurückgegeben wird eine ArrayList von Links, welche dem Netzwerk angehören
 * und deren Start- und Endknoten in der übergebenen ArrayList enthalten sind. 
 * Wird zusätzlich noch eine ArrayList Links mit übergeben, so wird diese
 * mit den neu gefundenen Links erweitert. Andernfalls wird eine neue erstellt.
 *
 */

package playground.christoph.knowledge.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class GetAllIncludedLinks {

	/**
	 * Returns an ArrayList of Links.
	 * @param NetworkLayer network
	 * @param ArrayList< Node > includedNodes
	 *  
	 * @return A link from the network is included in the returned ArrayList, if its
	 * start- and end node are included in the includedNodes ArrayList.
	 */
	public ArrayList<Link> getAllLinks(NetworkLayer network, ArrayList<Node> includedNodes)
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
	public ArrayList<Link> getAllLinks(NetworkLayer network, Map<Id, Node> includedNodesMap)
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
	public void getAllLinks(NetworkLayer network, ArrayList<Node> includedNodes, ArrayList<Link> includedLinks)
	{		
		// get all links of the network
		Map<Id, Link> linkMap = network.getLinks();
		
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
	public void getAllLinks(NetworkLayer network, Map<Id, Node> includedNodesMap, ArrayList<Link> includedLinks)
	{	
		// get all links of the network
		Map<Id, Link> linkMap = network.getLinks();
		
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