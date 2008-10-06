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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;

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
		// try getting Nodes from the Persons Knowledge
		ArrayList<Node> knownNodes = KnowledgeTools.getKnownNodes(person);
		
		// if the Person has an Activity Room in his/her Knowledge
		if(knownNodes != null)
		{
			// Nodes that must not be Dead Ends because they are parts of the Persons Activities.
			ArrayList<Node> activityNodes = getActivityNodes(person);
			
			ArrayList<Node> deadEnds = new ArrayList<Node>();
			ArrayList<Node> possibleDeadEnds = new ArrayList<Node>();

			int previousNodeCount = knownNodes.size();

			// initially put all known Nodes in the possibleDeadEnds
			possibleDeadEnds = (ArrayList<Node>)knownNodes.clone();
			
			// Repeat, until no possible Dead Ends are left
			while(possibleDeadEnds.size() != 0)
			{
				// clear previous found Dead Ends
				deadEnds.clear();
				
				// find real Dead Ends within the possible Dead Ends and remove them 
				for(int i = 0; i < possibleDeadEnds.size(); i++)
				{
					Node node = possibleDeadEnds.get(i);
					
					if(!activityNodes.contains(node) && isDeadEnd(knownNodes, node))
					{
						deadEnds.add(node);
						knownNodes.remove(node);
					}
				}
	
				// remove Dead Ends from Known Nodes
				//for(int i = 0; i < deadEnds.size(); i++) knownNodes.remove(deadEnds.get(i));
				
				// find possible Dead Ends for the next loop based on the found Dead Ends
				possibleDeadEnds.clear();
	
				for(int i = 0; i < deadEnds.size(); i++)
				{
					Node node = deadEnds.get(i);
					
					// get inNodes to the Dead End Node that are Part of the Activity Map of the Person
					ArrayList<Node> inNodes = getInNodes(knownNodes, node);
					
					for(int j = 0; j < inNodes.size(); j++)
					{
						// If the inNodes isn't already contained in the ArrayList -> add it.
						if(!possibleDeadEnds.contains(inNodes.get(j))) possibleDeadEnds.add(inNodes.get(j));
					}
				}
					
			}	// while(possibleDeadEnds.size() != 0)
			
			deadEnds.clear();
			possibleDeadEnds.clear();
			
			log.info("nodecount ... previous ... " + previousNodeCount + " ... now ... " + knownNodes.size());
		}	// if knownNodes != null
	
	}	// removeDeadEnds(Person person)
	
	
	/*
	 * Returns false, if there is at least one outgoing Link known.
	 */
	public static boolean isDeadEnd(ArrayList<Node> knownNodes, Node node)
	{
		// If the Node is not contained in the ArrayList, it should be removed...
		if (!knownNodes.contains(node)) return true;
		
		Map<Id, Node> myMap = (Map<Id, Node>)node.getOutNodes();
		
		Iterator nodeIterator = myMap.values().iterator();
		while(nodeIterator.hasNext())
		{
			Node outNode = (Node)nodeIterator.next();

			// If the OutNode is contained in the Activity Map the current Node is no Dead End -> stop searching.
			if(knownNodes.contains(outNode)) return false;
		}
		
		// No OutNode found in the Activity Map -> Node is a Dead End.
		return true;
	}
	
	/*
	 * Returns an ArrayList with the Start- and Endnodes of the Activities of the selected Plan of a Person.
	 */
	public static ArrayList<Node> getActivityNodes(Person person)
	{
		ArrayList<Node> activityNodes = new ArrayList<Node>();
		
		Plan plan = person.getSelectedPlan();
		
		ActIterator iterator = plan.getIteratorAct();
		while(iterator.hasNext())
		{
			Act act = (Act)iterator.next();
			
			Node fromNode = act.getLink().getFromNode();
			Node toNode = act.getLink().getToNode();
			
			if(!activityNodes.contains(fromNode)) activityNodes.add(fromNode);
			if(!activityNodes.contains(toNode)) activityNodes.add(toNode);
			
		}
			
		return activityNodes;
	}

	/*
	 * Returns the Startnodes of those incoming Links that are contained in the ArrayList. 
	 */
	protected static ArrayList<Node> getInNodes(ArrayList<Node> knownNodes, Node node)
	{
		ArrayList<Node> inNodes = new ArrayList<Node>();
		
		Map<Id, Node> myMap = (Map<Id, Node>)node.getInNodes();
		
		Iterator nodeIterator = myMap.values().iterator();
		while(nodeIterator.hasNext())
		{
			Node inNode = (Node)nodeIterator.next();
			
			// If the InNode is contained in the Activity Map and not already in the ArrayList -> add it.	 
			if(knownNodes.contains(inNode) && !inNodes.contains(inNode)) inNodes.add(inNode);
		}
		
		return inNodes;
	}
	
}