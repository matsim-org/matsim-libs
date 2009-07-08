/* *********************************************************************** *
 * project: org.matsim.*
 * DeadEndRemover.java
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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

/*
 * Removes Dead Ends from the Activity Maps in the Knowledge of a Person.
 * 
 * A Node is a Dead End, if there are no outgoing links known and 
 * if there is no Activity at one of its incoming or outgoing links.
 */
public class DeadEndRemover {

	private final static Logger log = Logger.getLogger(DeadEndRemover.class);
	
	public static void removeDeadEnds(PersonImpl person)
	{
		Map<Id, NodeImpl> knownNodesMap;
		
		/*
		 * Try getting Nodes from the Persons NodeKnowledge.
		 * If there is no NodeKnowledge there can't be a Map of known Nodes
		 * so set the knownNodesMap to null.
		 */ 
		if(person.getCustomAttributes().containsKey("NodeKnowledge")) knownNodesMap = KnowledgeTools.getKnownNodes(person);
		else knownNodesMap = null;
		
		// if the Person has an Activity Room in his/her Knowledge
		if(knownNodesMap != null)
		{
			// Nodes that must not be Dead Ends because they are parts of the Persons Activities.
			Map<Id, NodeImpl> activityNodesMap = getActivityNodesMap(person);
			
			Map<Id, NodeImpl> deadEndsMap = new HashMap<Id, NodeImpl>();
			Map<Id, NodeImpl> possibleDeadEndsMap = new HashMap<Id, NodeImpl>();

			int previousNodeCount = knownNodesMap.size();

			// initially put all known Nodes in the possibleDeadEnds
			for(NodeImpl node : knownNodesMap.values())
			{
				possibleDeadEndsMap.put(node.getId(), node);
			}
			
			// Repeat, until no possible Dead Ends are left
			while(possibleDeadEndsMap.size() != 0)
			{
				// clear previous found Dead Ends
				deadEndsMap.clear();
				
				// find real Dead Ends within the possible Dead Ends and remove them 
				for(NodeImpl node : possibleDeadEndsMap.values())
				{				
					if(!activityNodesMap.containsKey(node.getId()) && isDeadEnd(knownNodesMap, node))
					{
						deadEndsMap.put(node.getId(), node);
						knownNodesMap.remove(node.getId());
					}
				}
								
				// find possible Dead Ends for the next loop based on the found Dead Ends
				possibleDeadEndsMap.clear();
	
				// Find possible Dead Ends to check in the next Loop.
				for(NodeImpl node : deadEndsMap.values())
				{				
					// get inNodes to the Dead End Node that are Part of the Activity Map of the Person
					Map<Id, NodeImpl> inNodesMap = getInNodesMap(knownNodesMap, node);
					
					for(NodeImpl inNode : inNodesMap.values())
					{
						// If the inNodes isn't already contained in the ArrayList -> add it.
						if(!possibleDeadEndsMap.containsKey(inNode.getId())) possibleDeadEndsMap.put(inNode.getId(), inNode);
					}
				}

				
			}	// while(possibleDeadEndsMap.size() != 0)
			
			deadEndsMap.clear();
			possibleDeadEndsMap.clear();
			
//			log.info("nodecount ... previous ... " + previousNodeCount + " ... now ... " + knownNodesMap.size());
		}	// if knownNodes != null
	
	}	// removeDeadEnds(Person person)
	
	
	/*
	 * Returns false, if there is at least one outgoing Link known.
	 */
	public static boolean isDeadEnd(Map<Id, NodeImpl> knownNodesMap, NodeImpl node)
	{
		// If the Node is not contained in the ArrayList, it should be removed...
		if (!knownNodesMap.containsKey(node.getId())) return true;
		
		Map<Id, NodeImpl> myMap = (Map<Id, NodeImpl>)node.getOutNodes();

		for(NodeImpl outNode : myMap.values())
		{
			// If the OutNode is contained in the Activity Map the current Node is no Dead End -> stop searching.
			if(knownNodesMap.containsKey(outNode.getId())) return false;
		}

		// No OutNode found in the Activity Map -> Node is a Dead End.
		return true;
	}
	
	/*
	 * Returns a Map with the Start- and Endnodes of the Activities of the selected Plan of a Person.
	 */
	//public static ArrayList<Node> getActivityNodes(Person person)
	public static Map<Id, NodeImpl> getActivityNodesMap(PersonImpl person)
	{
		Map<Id, NodeImpl> activityNodesMap = new HashMap<Id, NodeImpl>();
		
		PlanImpl plan = person.getSelectedPlan();
		
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				
				NodeImpl fromNode = act.getLink().getFromNode();
				NodeImpl toNode = act.getLink().getToNode();
				
				if(!activityNodesMap.containsKey(fromNode.getId())) activityNodesMap.put(fromNode.getId(), fromNode);
				if(!activityNodesMap.containsKey(toNode.getId())) activityNodesMap.put(toNode.getId(), toNode);
			}
		}
			
		return activityNodesMap;
	}

	/*
	 * Returns the Startnodes of those incoming Links that are contained in the Map. 
	 */
	protected static Map<Id, NodeImpl> getInNodesMap(Map<Id, NodeImpl> knownNodesMap, NodeImpl node)
	{
		Map<Id, NodeImpl> inNodesMap = new HashMap<Id, NodeImpl>();
		
		Map<Id, NodeImpl> myMap = (Map<Id, NodeImpl>)node.getInNodes();
		
		for(NodeImpl inNode : myMap.values())
		{
			// If the InNode is contained in the Activity Map and not already in the ArrayList -> add it.	 
			if(knownNodesMap.containsKey(inNode.getId()) && !inNodesMap.containsKey(inNode.getId())) inNodesMap.put(inNode.getId(), inNode);
		}

		return inNodesMap;
	}

}