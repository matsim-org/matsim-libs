/* *********************************************************************** *
 * project: org.matsim.*
 * CreateKnownNodesMap.java
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

package playground.christoph.knowledge.nodeselection;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;

import playground.christoph.knowledge.container.MapKnowledge;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.DeadEndRemover;

public class CreateKnownNodesMap {

	public static boolean removeDeadEnds = true;
	
	private final static Logger log = Logger.getLogger(CreateKnownNodesMap.class);
	
	public static void collectAllSelectedNodes(PopulationImpl population, NetworkLayer network)
	{
		for (PersonImpl person : population.getPersons().values()) 
		{
			collectAllSelectedNodes(person, network);
		}
	}
	
	private static void collectAllSelectedNodes(PersonImpl person, NetworkLayer network)
	{					
		ArrayList<SelectNodes> personNodeSelectors = (ArrayList<SelectNodes>)person.getCustomAttributes().get("NodeSelectors");
		
		// for all node selectors of the person
		for (SelectNodes nodeSelector : personNodeSelectors)
		{
			collectSelectedNodes(person, network, nodeSelector);
		}
		// if Flag is set, remove Dead Ends from the Person's Activity Room
		if(removeDeadEnds) DeadEndRemover.removeDeadEnds(person);
	}
	
	/*
	 * The handling of the nodeSelectors should probably be outsourced...
	 * Implementing them direct in the nodeSelectors could be a good solution...
	 */
	public static void collectSelectedNodes(PersonImpl p, NetworkLayer network, SelectNodes nodeSelector)
	{		
		PlanImpl plan = p.getSelectedPlan();
		
		// get Nodes from the Person's Knowledge
		Map<Id, NodeImpl> nodesMap = null;
		
		if (p.getCustomAttributes().get("NodeKnowledge") == null)
		{
			nodesMap = new TreeMap<Id, NodeImpl>();
			
			NodeKnowledge nodeKnowledge = new MapKnowledge(nodesMap);
			nodeKnowledge.setPerson(p);
			nodeKnowledge.setNetwork(network);
			
			p.getCustomAttributes().put("NodeKnowledge", nodeKnowledge);
		}
		else
		{
			NodeKnowledge nodeKnowledge = (NodeKnowledge)p.getCustomAttributes().get("NodeKnowledge");
			nodesMap = nodeKnowledge.getKnownNodes();
		}
		
		if(nodeSelector instanceof SelectNodesDijkstra)
		{
			// get all acts of the selected plan
			ArrayList<ActivityImpl> acts = new ArrayList<ActivityImpl>();					
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					acts.add((ActivityImpl) pe);
				}
			}
			
			for(int j = 1; j < acts.size(); j++)
			{						
				NodeImpl startNode = acts.get(j-1).getLink().getToNode();
				NodeImpl endNode = acts.get(j).getLink().getFromNode();
					
				((SelectNodesDijkstra)nodeSelector).setStartNode(startNode);
				((SelectNodesDijkstra)nodeSelector).setEndNode(endNode);

				nodeSelector.addNodesToMap(nodesMap);
			}
		}	//if instanceof SelectNodesDijkstra
			
		else if(nodeSelector instanceof SelectNodesCircular)
		{
			// do something else here...
		}
			
		else
		{
			log.error("Unkown NodeSelector!");
		}
	
	}
	
	public static void setRemoveDeadEnds(boolean value)
	{
		removeDeadEnds = value; 
	}
	
	public static boolean getRemoveDeadEnds()
	{
		return removeDeadEnds;
	}
}