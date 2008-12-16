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
import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Person;

public class KnowledgeTools {

	private final static Logger log = Logger.getLogger(KnowledgeTools.class);	
	
	/*
	 * Returns a Map of Nodes, if the Person has Knowledge about known Nodes. 
	 */
	public static Map<Id, Node> getKnownNodes(Person person)
	{
		Map<Id, Node> knownNodesMap = null;
		
		// Try getting knowledge from the current Person.
		if(person != null)
		{		
			if(person.getKnowledge() != null)
			{
				Map<String,Object> customAttributes = person.getKnowledge().getCustomAttributes();
				
				if(customAttributes.containsKey("Nodes"))
				{
					knownNodesMap = (Map<Id, Node>)customAttributes.get("Nodes");
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
		
		return knownNodesMap;
	}
	
	/*
	 * Returns only those links, where Start- and Endnode are contained in the Map.
	 * If no Nodes are known, all links are returned.
	 */
	public static Link[] getKnownLinks(Link[] links, Map<Id, Node> knownNodesMap)
	{	
		// If the current Person has knowledge about known Nodes (Map exists and has Elements)
		if(knownNodesMap != null && knownNodesMap.size() != 0)
		{
			ArrayList<Link> knownLinks = new ArrayList<Link>();
			
			for(int i = 0; i < links.length; i++)
			{
				if ( knownNodesMap.containsKey(links[i].getFromNode().getId()) && knownNodesMap.containsKey(links[i].getToNode().getId()) )
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
	 * Returns true, if the Start- and Endnode of the Link are included in the Map.
	 * Returns true, if no known nodes are stored in the current Person.
	 */
	//public static boolean knowsLink(Link link, ArrayList<Node> knownNodes)
	public static boolean knowsLink(Link link, Map<Id, Node> knownNodesMap)
	{
		// if no Map found or the Map is empty -> Person knows the entire network, return true
		if ( knownNodesMap == null ) return true;
		if ( knownNodesMap.size() == 0) return true;
		
		if ( knownNodesMap.containsKey(link.getFromNode().getId()) && knownNodesMap.containsKey(link.getToNode().getId()) ) return true;
		else return false;
	}
	
	/*
	 * To save memory, some routers may want to remove a Person's Knowledge after
	 * doing their routing. An Example would be a Random Router that does only an
	 * initial planning before starting the mobsim.
	 */ 
	public static void removeKnowledge(Person person)
	{
		Map<Id, Node> knownNodesMap = null;
		
		// Try getting knowledge from the current Person.
		if(person != null)
		{		
			if(person.getKnowledge() != null)
			{
				Map<String,Object> customAttributes = person.getKnowledge().getCustomAttributes();
				
				if(customAttributes.containsKey("Nodes"))
				{
					knownNodesMap = (Map<Id, Node>)customAttributes.get("Nodes");
					
					// remove all known nodes!
					knownNodesMap.clear();
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
	}
}
