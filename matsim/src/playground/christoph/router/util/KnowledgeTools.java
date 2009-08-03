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
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PersonImpl;

import playground.christoph.knowledge.container.NodeKnowledge;

public class KnowledgeTools {

	private final static Logger log = Logger.getLogger(KnowledgeTools.class);
	
	public KnowledgeTools()
	{
	}
	
	/*
	 * Returns a Map of Nodes, if the Person has Knowledge about known Nodes. 
	 */
	public Map<Id, NodeImpl> getKnownNodes(PersonImpl person)
	{
		Map<Id, NodeImpl> knownNodesMap = null;
		
		// Try getting knowledge from the current Person.
		if(person != null)
		{		
			Map<String,Object> customAttributes = person.getCustomAttributes();
					
			if(customAttributes.containsKey("NodeKnowledge"))
			{
				NodeKnowledge nodeKnowledge = (NodeKnowledge)customAttributes.get("NodeKnowledge");
			
				knownNodesMap = nodeKnowledge.getKnownNodes();
			}
			else
			{
				log.error("NodeKnowledge Object was not found in Person's Custom Attributes!");
			}
		}
		else
		{
			log.error("person = null!");
		}
		
		return knownNodesMap;
	}
	
	/*
	 * Returns a Map of Nodes, if the Person has Knowledge about known Nodes. 
	 */
	public NodeKnowledge getNodeKnowledge(PersonImpl person)
	{
		NodeKnowledge nodeKnowledge = null;
		
		// Try getting knowledge from the current Person.
		if(person != null)
		{		
			Map<String,Object> customAttributes = person.getCustomAttributes();
					
			if(customAttributes.containsKey("NodeKnowledge"))
			{
				nodeKnowledge = (NodeKnowledge)customAttributes.get("NodeKnowledge");
			}
			else
			{
				log.error("NodeKnowledge Object was not found in Person's Custom Attributes!");
			}
		}
		else
		{
			log.error("person = null!");
		}
		
		return nodeKnowledge;
	}
	
	/*
	 * Returns only those links, where Start- and Endnode are contained in the Map.
	 * If no Nodes are known, all links are returned.
	 */
	public Link[] getKnownLinks(Link[] links, Map<Id, NodeImpl> knownNodesMap)
	{	
		// If the current Person has knowledge about known Nodes (Map exists and has Elements)
		if((knownNodesMap != null) && (knownNodesMap.size() != 0))
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
	//public static boolean knowsLink(Link link, Map<Id, Node> knownNodesMap)
/*
	public static boolean knowsLink(Link link, NodeKnowledge nodeKnowledge)
	{
		// if no Map found or the Map is empty -> Person knows the entire network, return true
		if ( nodeKnowledge == null ) return true;
		//if ( nodeKnowledge.size() == 0) return true;
				
		//if ( nodeKnowledge.containsKey(link.getFromNode().getId()) && nodeKnowledge.containsKey(link.getToNode().getId()) ) return true;
		if ( nodeKnowledge.knowsNode(link.getFromNode()) && nodeKnowledge.knowsNode(link.getToNode()) ) return true;
		else return false;
	}
*/	
	/*
	 * To save memory, some routers may want to remove a Person's Knowledge after
	 * doing their routing. An Example would be a Random Router that does only an
	 * initial planning before starting the mobsim.
	 */ 
	public void removeKnowledge(PersonImpl person)
	{
		Map<Id, NodeImpl> knownNodesMap = null;
		
		// Try getting knowledge from the current Person.
		if(person != null)
		{	
			Map<String,Object> customAttributes = person.getCustomAttributes();
			
			if(customAttributes.containsKey("NodeKnowledge"))
			{
				NodeKnowledge nodeKnowledge = (NodeKnowledge)customAttributes.get("NodeKnowledge");
			
				knownNodesMap = nodeKnowledge.getKnownNodes();
				knownNodesMap.clear();
			}
			else
			{
				log.error("NodeKnowledge Object was not found in Person's Custom Attributes!");
			}
		}
		else
		{
			log.error("person = null!");
		}
	}
}
