/* *********************************************************************** *
 * project: org.matsim.*
 * TabuSelector.java
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

package playground.christoph.router.util;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/*
 * Returns those outgoing links from a Node, which don't return directly to a given previous node.
 * If all links return to the previous node, all links are returned. 
 * 
 */
public class TabuSelector {

	private final static Logger log = Logger.getLogger(TabuSelector.class);
	
	public Link[] getLinks(Link[] links, Node previousNode)
	{	
		/*
		 * If there is no previous Node (i.e. Person is at the first Node of a Leg)
		 * all available Links can be chosen!
		 */ 
		if(previousNode == null) return links;
		
		// remove Links to the previous node, if other Links are available
		ArrayList<Link> newLinks = new ArrayList<Link>();
		for(int i = 0; i < links.length; i++)
		{
			// if the link doesn't head to the previous Node -> add to possible Links
			if(!links[i].getToNode().equals(previousNode)) newLinks.add(links[i]);
		}
			
		// if all links return to the previous node -> all are possible 
		if (links.length > 0 && newLinks.size() == 0)
		{
			for(int i = 0; i < links.length; i++) newLinks.add(links[i]);
			
//			log.info("All available outgoing links return to the previous node, so choosing one of them!");
		}
//		else log.info("Found outgoing Links to new Nodes!");
			
		// create Array that is returned
		Link[] returnedLinks = new Link[newLinks.size()];
		for(int i = 0; i < newLinks.size(); i++) returnedLinks[i] = newLinks.get(i);			
		
		return returnedLinks;
	}
	
}
