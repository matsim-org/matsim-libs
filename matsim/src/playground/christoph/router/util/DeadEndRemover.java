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
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.Node;

/*
 * Removes Dead Ends from the Activity Maps in the Knowledge of a Person.
 * 
 * A Node is a Dead End, if there are no outgoing links known and 
 * if there is no Activity at one of its incoming or outgoing links.
 */
public class DeadEndRemover {

	private final static Logger log = Logger.getLogger(DeadEndRemover.class);
	
	public static void removeDeadEnds(Person person)
	{
		Map<Id, Node> knownNodesMap;
		
		/*
		 * Try getting Nodes from the Persons Knowledge.
		 * If there is no Knowledge there can't be a Map of known Nodes
		 * so set the knownNodesMap to null.
		 */ 
		if(person.getKnowledge() != null) knownNodesMap = KnowledgeTools.getKnownNodes(person);
		else knownNodesMap = null;
		
		// if the Person has an Activity Room in his/her Knowledge
		if(knownNodesMap != null)
		{
			// Nodes that must not be Dead Ends because they are parts of the Persons Activities.
			Map<Id, Node> activityNodesMap = getActivityNodesMap(person);
			
			Map<Id, Node> deadEndsMap = new HashMap<Id, Node>();
			Map<Id, Node> possibleDeadEndsMap = new HashMap<Id, Node>();

			int previousNodeCount = knownNodesMap.size();

			// initially put all known Nodes in the possibleDeadEnds
			for(Node node : knownNodesMap.values())
			{
				possibleDeadEndsMap.put(node.getId(), node);
			}
			
			// Repeat, until no possible Dead Ends are left
			while(possibleDeadEndsMap.size() != 0)
			{
				// clear previous found Dead Ends
				deadEndsMap.clear();
				
				// find real Dead Ends within the possible Dead Ends and remove them 
				for(Node node : possibleDeadEndsMap.values())
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
				for(Node node : deadEndsMap.values())
				{				
					// get inNodes to the Dead End Node that are Part of the Activity Map of the Person
					Map<Id, Node> inNodesMap = getInNodesMap(knownNodesMap, node);
					
					for(Node inNode : inNodesMap.values())
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
	public static boolean isDeadEnd(Map<Id, Node> knownNodesMap, Node node)
	{
		// If the Node is not contained in the ArrayList, it should be removed...
		if (!knownNodesMap.containsKey(node.getId())) return true;
		
		Map<Id, Node> myMap = (Map<Id, Node>)node.getOutNodes();

		for(Node outNode : myMap.values())
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
	public static Map<Id, Node> getActivityNodesMap(Person person)
	{
		Map<Id, Node> activityNodesMap = new HashMap<Id, Node>();
		
		Plan plan = person.getSelectedPlan();
		
		ActIterator iterator = plan.getIteratorAct();
		while(iterator.hasNext())
		{
			Act act = (Act)iterator.next();
			
			Node fromNode = act.getLink().getFromNode();
			Node toNode = act.getLink().getToNode();
			
			if(!activityNodesMap.containsKey(fromNode.getId())) activityNodesMap.put(fromNode.getId(), fromNode);
			if(!activityNodesMap.containsKey(toNode.getId())) activityNodesMap.put(toNode.getId(), toNode);
		}
			
		return activityNodesMap;
	}

	/*
	 * Returns the Startnodes of those incoming Links that are contained in the Map. 
	 */
	protected static Map<Id, Node> getInNodesMap(Map<Id, Node> knownNodesMap, Node node)
	{
		Map<Id, Node> inNodesMap = new HashMap<Id, Node>();
		
		Map<Id, Node> myMap = (Map<Id, Node>)node.getInNodes();
		
		for(Node inNode : myMap.values())
		{
			// If the InNode is contained in the Activity Map and not already in the ArrayList -> add it.	 
			if(knownNodesMap.containsKey(inNode.getId()) && !inNodesMap.containsKey(inNode.getId())) inNodesMap.put(inNode.getId(), inNode);
		}

		return inNodesMap;
	}

}