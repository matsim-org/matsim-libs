/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeTools.java
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.util.SubNetworkCreator;

public class KnowledgeTools {

	private final static Logger log = Logger.getLogger(KnowledgeTools.class);

	private int warnCounter = 0;
	
	public KnowledgeTools() 
	{
	}

	/*
	 * Returns a Map of Nodes, if the Person has Knowledge about known Nodes.
	 */
	public Map<Id, Node> getKnownNodes(Person person) 
	{
		Map<Id, Node> knownNodesMap = null;

		// Try getting knowledge from the current Person.
		if (person != null)
		{
			Map<String, Object> customAttributes = person.getCustomAttributes();

			NodeKnowledge nodeKnowledge;
			if ((nodeKnowledge = getNodeKnowledge(customAttributes)) != null)
			{
				knownNodesMap = nodeKnowledge.getKnownNodes();	
			}
			else 
			{
				if (warnCounter < 10)
				{
					log.warn("NodeKnowledge Object was not found in Person's Custom Attributes!");
					warnCounter++;
				}
			}
		} else
		{
			log.error("person = null!");
		}

		return knownNodesMap;
	}
	
	/*
	 * Returns a Map of Nodes, if the Person has Knowledge about known Nodes.
	 */
	public NodeKnowledge getNodeKnowledge(Person person) 
	{
		NodeKnowledge nodeKnowledge = null;

		// Try getting knowledge from the current Person.
		if (person != null) 
		{
			Map<String, Object> customAttributes = person.getCustomAttributes();

			if ((nodeKnowledge = getNodeKnowledge(customAttributes)) != null)
			{
			}
			else 
			{
				if (warnCounter < 10)
				{
					log.warn("NodeKnowledge Object was not found in Person's Custom Attributes!");
					warnCounter++;
				}
			}
		} else {
			log.error("person = null!");
		}

		return nodeKnowledge;
	}

	/*
	 * Returns only those links, where Start- and Endnode are contained in the
	 * Map. If no Nodes are known, all links are returned.
	 */
	public Link[] getKnownLinks(Link[] links, Map<Id, Node> knownNodesMap)
	{
		// If the current Person has knowledge about known Nodes (Map exists and
		// has Elements)
		if ((knownNodesMap != null) && (knownNodesMap.size() != 0)) 
		{
			ArrayList<Link> knownLinks = new ArrayList<Link>();

			for (int i = 0; i < links.length; i++) 
			{
				if (knownNodesMap.containsKey(links[i].getFromNode().getId()) && knownNodesMap.containsKey(links[i].getToNode().getId())) 
				{
					knownLinks.add(links[i]);
				}
			}

			// if (links.length != knownLinks.size())
			// log.info("Reduced possible Links! Old linkcount: " + links.length
			// + " new linkcout: " + knownLinks.size());

			links = new Link[knownLinks.size()];
			for (int i = 0; i < links.length; i++)
				links[i] = knownLinks.get(i);

			knownLinks = null;
		}
		return links;
	}

	/*
	 * To save memory, some routers may want to remove a Person's Knowledge
	 * after doing their routing. An Example would be a Random Router that does
	 * only an initial planning before starting the mobsim.
	 */
	public void removeKnowledge(Person person) 
	{
//		Map<Id, NodeImpl> knownNodesMap = null;

		// Try getting knowledge from the current Person.
		if (person != null) 
		{
			Map<String, Object> customAttributes = person.getCustomAttributes();

			NodeKnowledge nodeKnowledge;
			if ((nodeKnowledge = getNodeKnowledge(customAttributes)) != null)
			{
				nodeKnowledge.clearKnowledge();
				// knownNodesMap = nodeKnowledge.getKnownNodes();
				// knownNodesMap.clear();
			}
			else 
			{
				if (warnCounter < 10)
				{
					log.warn("NodeKnowledge Object was not found in Person's Custom Attributes!");
					warnCounter++;
				}
			}
		} 
		else 
		{
			log.error("person = null!");
		}
	}
	
	private NodeKnowledge getNodeKnowledge(Map<String, Object> customAttributes)
	{
		if (customAttributes.containsKey("NodeKnowledge"))
		{
			NodeKnowledge nodeKnowledge = (NodeKnowledge) customAttributes.get("NodeKnowledge");

			return nodeKnowledge;
		} 
		else return null;
	}
	
	public Network getSubNetwork(Person person, Network network)
	{
		if (person != null)
		{
			Map<String, Object> customAttributes = person.getCustomAttributes();
			
			SubNetwork subNetwork;
			
			// if there is already a SubNetwork in the Person's Attributes
			subNetwork = (SubNetwork) customAttributes.get("SubNetwork");
			if (subNetwork != null) return subNetwork;
			
			// ... else
			NodeKnowledge nodeKnowledge;
			if ((nodeKnowledge = getNodeKnowledge(customAttributes)) != null)
			{
				if (nodeKnowledge.getKnownNodes() != null)
				{
					SubNetworkCreator snc = new SubNetworkCreator(network);
					subNetwork = snc.createSubNetwork(nodeKnowledge);
					customAttributes.put("SubNetwork", subNetwork);
					return subNetwork;
				}				
			}
			else 
			{
				if (warnCounter < 10)
				{
					log.warn("NodeKnowledge Object was not found in Person's Custom Attributes!");
					warnCounter++;
				};
			}
		}
		
		return network;
	}
	
	/*
	 * To save memory, some routers may want to remove a Person's SubNetwork
	 * after doing their routing. An Example would be a Random Router that does
	 * only an initial planning before starting the mobsim.
	 */
	public void removeSubNetwork(Person person) 
	{
		// Try getting knowledge from the current Person.
		if (person != null) 
		{
			Map<String, Object> customAttributes = person.getCustomAttributes();

			customAttributes.remove("SubNetwork");
		} 
		else 
		{
			log.error("person = null!");
		}
	}
}
