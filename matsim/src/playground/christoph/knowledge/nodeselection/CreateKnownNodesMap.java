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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.christoph.knowledge.container.MapKnowledge;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.DeadEndRemover;

public class CreateKnownNodesMap {

	public static boolean removeDeadEnds = false;
	
	private final static Logger log = Logger.getLogger(CreateKnownNodesMap.class);
	
/*	
	public void collectAllSelectedNodes(PopulationImpl population, Network network)
	{
		for (PersonImpl person : population.getPersons().values()) 
		{
			collectAllSelectedNodes(person, network);
		}
	}
	

	private void collectAllSelectedNodes(PersonImpl person, Network network)
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
*/	
	/*
	 * The handling of the nodeSelectors should probably be outsourced...
	 * Implementing them direct in the nodeSelectors could be a good solution...
	 */

	public static void collectSelectedNodes(PersonImpl p, NetworkLayer network, SelectNodes nodeSelector)
	{		
		PlanImpl plan = p.getSelectedPlan();
/*		
		ArrayList<Leg> legs = new ArrayList<Leg>();					
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				legs.add((Leg) pe);
			}
		}
		
		int count = 0;
		for (Leg leg : legs)
		{
			NodeNetworkRoute route = (NodeNetworkRoute) leg.getRoute();
			
			count = count + route.getNodes().size();
		}

		if (count == 0)
		{
			log.error("No Nodes found in Persons Routes..." + p.getId());
		}
*/		
		// get Nodes from the Person's Knowledge
		Map<Id, NodeImpl> nodesMap = null;
		
		if (p.getCustomAttributes().get("NodeKnowledge") == null)
		{
			nodesMap = new TreeMap<Id, NodeImpl>();
			
			NodeKnowledge nodeKnowledge = new MapKnowledge();
			nodeKnowledge.setPerson(p);
			nodeKnowledge.setNetwork(network);
			nodeKnowledge.setKnownNodes(nodesMap);
			
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

				Map<Id, NodeImpl> newNodes = new HashMap<Id, NodeImpl>();
				
				nodeSelector.addNodesToMap(newNodes);
				
				if (newNodes.size() == 0) log.error("No new known Nodes found?!");
				
				/*
				 *  Add additionally all Start & End Nodes of Links that contains Activity.
				 *  This ensures that a Person knows at least the Link where the Activity
				 *  takes place.
				 */
				NodeImpl startNodeFrom = acts.get(j-1).getLink().getFromNode();			
				NodeImpl endNodeTo = acts.get(j).getLink().getToNode();
				newNodes.put(startNodeFrom.getId(), startNodeFrom);
				newNodes.put(endNodeTo.getId(), endNodeTo);
				
				nodesMap.putAll(newNodes);
			}
		}	//if instanceof SelectNodesDijkstra
			
		else if(nodeSelector instanceof SelectNodesCircular)
		{
			// do something else here...
			
			// get all acts of the selected plan
			ArrayList<ActivityImpl> acts = new ArrayList<ActivityImpl>();					
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					acts.add((ActivityImpl) pe);
				}
			}
			
			for (ActivityImpl activityImpl : acts)
			{
				((SelectNodesCircular)nodeSelector).setLink(activityImpl.getLink());
				((SelectNodesCircular)nodeSelector).addNodesToMap(nodesMap);
			}
		}
			
		else
		{
			log.error("Unkown NodeSelector!");
		}
	
//		if (nodesMap.size() == 0) log.error("No known Nodes found?!");
		
		if(removeDeadEnds) DeadEndRemover.removeDeadEnds(p);
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