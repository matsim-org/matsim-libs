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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.christoph.knowledge.container.MapKnowledge;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.DeadEndRemover;

public class CreateKnownNodesMap {

	public static boolean removeDeadEnds = false;

	private final static Logger log = Logger.getLogger(CreateKnownNodesMap.class);

	/*
	 * The handling of the nodeSelectors should probably be outsourced...
	 * Implementing them direct in the nodeSelectors could be a good solution...
	 */

	public void collectSelectedNodes(Person p, Network network, SelectNodes nodeSelector)
	{
		Plan plan = p.getSelectedPlan();

		// get Nodes from the Person's Knowledge
		Map<Id, Node> nodesMap = null;

		if (p.getCustomAttributes().get("NodeKnowledge") == null)
		{
			nodesMap = new TreeMap<Id, Node>();

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
			ArrayList<Activity> acts = new ArrayList<Activity>();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					acts.add((Activity) pe);
				}
			}

			for(int j = 1; j < acts.size(); j++)
			{
				Node startNode = network.getLinks().get(acts.get(j-1).getLinkId()).getToNode();
				Node endNode = network.getLinks().get(acts.get(j).getLinkId()).getFromNode();

				((SelectNodesDijkstra)nodeSelector).setStartNode(startNode);
				((SelectNodesDijkstra)nodeSelector).setEndNode(endNode);

				Map<Id, Node> newNodes = new HashMap<Id, Node>();

				nodeSelector.addNodesToMap(newNodes);

				if (newNodes.size() == 0) log.error("No new known Nodes found?!");

				/*
				 *  Add additionally all Start & End Nodes of Links that contains Activities.
				 *  This ensures that a Person knows at least the Link where the Activity
				 *  takes place.
				 */
				Node startNodeFrom = network.getLinks().get(acts.get(j-1).getLinkId()).getFromNode();
				Node endNodeTo = network.getLinks().get(acts.get(j).getLinkId()).getToNode();
				newNodes.put(startNodeFrom.getId(), startNodeFrom);
				newNodes.put(endNodeTo.getId(), endNodeTo);

				nodesMap.putAll(newNodes);
			}
		}	//if instanceof SelectNodesDijkstra

		else if(nodeSelector instanceof SelectNodesCircular)
		{
			// do something else here...

			// get all acts of the selected plan
			ArrayList<Activity> acts = new ArrayList<Activity>();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					acts.add((Activity) pe);
				}
			}

			for (Activity activityImpl : acts)
			{
				((SelectNodesCircular)nodeSelector).setLink(network.getLinks().get(activityImpl.getLinkId()));
				((SelectNodesCircular)nodeSelector).addNodesToMap(nodesMap);
			}
		}

		else
		{
			log.error("Unkown NodeSelector!");
		}

//		if (nodesMap.size() == 0) log.error("No known Nodes found?!");

		if(removeDeadEnds) DeadEndRemover.removeDeadEnds(p, network);
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