/* *********************************************************************** *
 * project: org.matsim.*
 * RandomCompassRoute.java
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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Person;

public class KnowledgeTools {

	private final static Logger log = Logger.getLogger(KnowledgeTools.class);	
	
	/*
	 * Returns an ArrayList of Nodes, if the Person has Knowledge about known Nodes. 
	 * 
	 */
	public static ArrayList<Node> getKnownNodes(Person person)
	{
		ArrayList<Node> knownNodes = null;
		
		// Try getting knowledge from the current Person.
		if(person != null)
		{		
			if(person.getKnowledge() != null)
			{
				Map<String,Object> customAttributes = person.getKnowledge().getCustomAttributes();
				
				if(customAttributes.containsKey("Nodes"))
				{
					knownNodes = (ArrayList<Node>)customAttributes.get("Nodes");
				}
				else
				{
					log.error("no knowledge found!");
				}
			}
			else
			{
				log.error("knowledge = null!");
			}
		}
		else
		{
			log.error("person = null!");
		}
		
		return knownNodes;
	}
	
	/*
	 * Return only those links, where Start- and Endnode are contained in the ArrayList.
	 * If no Nodes are known, all links are returned.
	 */
	public static Link[] getKnownLinks(Link[] links, ArrayList<Node> knownNodes)
	{
		// If the current Person has knowledge about known Nodes
		if(knownNodes != null)
		{
			ArrayList<Link> knownLinks = new ArrayList<Link>();
			
			for(int i = 0; i < links.length; i++)
			{
				if ( knownNodes.contains(links[i].getFromNode()) && knownNodes.contains(links[i].getToNode()) )
				{
					knownLinks.add(links[i]);
				}
			}

//			if (links.length != knownLinks.size())
//				log.info("Reduced possible Links! Old linkcount: " + links.length + " new linkcout: " + knownLinks.size());
			
			links = new Link[knownLinks.size()];
			for(int i = 0; i < links.length; i++) links[i] = knownLinks.get(i);
			
			knownLinks = null;
		}
		return links;
	}

	/*
	 * Returns true, if the Start- and Endnode of the Link are included in the ArrayList.
	 * Returns true, if no known nodes are stored in the current Person.
	 */
	public static boolean knowsLink(Link link, ArrayList<Node> knownNodes)
	{
		if ( knownNodes == null ) return true;
		if ( knownNodes.contains(link.getFromNode()) && knownNodes.contains(link.getToNode()) ) return true;
		else return false;
	}
	
}
