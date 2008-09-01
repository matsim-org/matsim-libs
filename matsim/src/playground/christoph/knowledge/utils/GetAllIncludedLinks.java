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

import org.matsim.basic.v01.Id;
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
	 * A link from the network is added to the includedLinks ArrayList, if its
	 * start- and end node are included in the includedNodes ArrayList.
	 *  
	 * @param NetworkLayer network
	 * @param ArrayList< Node > includedNodes
	 * @param ArrayList< Link > includedLinks
	 */
	public void getAllLinks(NetworkLayer network, ArrayList<Node> includedNodes, ArrayList<Link> includedLinks)
	{		
		// Alle Links des Netzwerks holen
		TreeMap<Id, Link> linkMap = (TreeMap<Id, Link>)network.getLinks();
		
		Iterator linkIterator = linkMap.entrySet().iterator();
		
		while(linkIterator.hasNext())
		{
			// Wir wissen ja, was für Elemente zurückgegeben werden :)
			Map.Entry<Id, Link> nextLink = (Map.Entry<Id, Link>)linkIterator.next();
			//Id id = nextLink.getKey();
			Link link = nextLink.getValue();
			
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
			
			// Prüfen, ob der Node in der übergebenen Liste enthalten ist
			if(includedNodes.contains(fromNode) && includedNodes.contains(toNode))
			{
				//... also beide Nodes enthalten -> Link enthalten
				includedLinks.add(link);
			}		
			
		}	// while nodeIterator.hasNext()
		
		//return includedLinks;
	}
	
}
