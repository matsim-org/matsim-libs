/* *********************************************************************** *
 * project: org.matsim.*
 * TabuRoute.java
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

package playground.christoph.router;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.router.util.LeastCostPathCalculator;

import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.LoopRemover;
import playground.christoph.router.util.PersonLeastCostPathCalculator;
import playground.christoph.router.util.RouteChecker;
import playground.christoph.router.util.TabuSelector;


public class TabuRoute extends PersonLeastCostPathCalculator {

	private final static Logger log = Logger.getLogger(TabuRoute.class);

	protected boolean removeLoops = true;
	protected int maxLinks = 50000; // maximum number of links in a created plan
	 
	/**
	 * Default constructor.
	 *
	 * @param random
	 * 			  Random number generator. Needed to create reproducible results.           
	 *            
	 */
	public TabuRoute() 
	{
	}

	
	public Route calcLeastCostPath(Node fromNode, Node toNode, double startTime)
	{
		return findRoute(fromNode, toNode);
	}
	
	protected Route findRoute(Node fromNode, Node toNode)
	{
		Node currentNode = fromNode;
		Link currentLink;
		double routeLength = 0.0;
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		Map<Id, Node> knownNodesMap = null;
		
		// try getting Nodes from the Persons Knowledge
		knownNodesMap = KnowledgeTools.getKnownNodes(this.person);
		
		nodes.add(fromNode);
	
		// first loop -> there is no previous node
		Node previousNode = null;
		
		while(!currentNode.equals(toNode))
		{
			// stop searching if to many links in the generated Route...
			if (nodes.size() > maxLinks) break;
			
			Link[] links = currentNode.getOutLinks().values().toArray(new Link[currentNode.getOutLinks().size()]);

			// Removes links, if their Start- and Endnodes are not contained in the known Nodes.
			links = KnowledgeTools.getKnownLinks(links, knownNodesMap);
			
			/*
			 * If there are no Links available something may be wrong.
			 */
			if (links.length == 0)
			{
				log.error("Looks like Node is a dead end. Routing could not be finished!");
				break;
			}
			
			// get Links, that do not return to the previous Node
			Link[] newLinks = TabuSelector.getLinks(links, previousNode);
			
			// choose link
			int nextLink = MatsimRandom.random.nextInt(newLinks.length);
			
			// make the chosen link to the new current link
			if(newLinks[nextLink] instanceof Link)
			{
				currentLink = newLinks[nextLink];
				previousNode = currentNode;
				currentNode = currentLink.getToNode();
				routeLength = routeLength + currentLink.getLength();
			}
			else
			{
				log.error("Return object was not from type Link! Class " + links[nextLink] + " was returned!");
				break;
			}
			nodes.add(currentNode);
		}	// while(!currentNode.equals(toNode))
		
		Route route = new Route();
		route.setRoute(nodes);
		route.setDist(routeLength);
		
		if (removeLoops) LoopRemover.removeLoops(route);
		
		return route;
	}
	
}