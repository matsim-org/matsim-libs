/* *********************************************************************** *
 * project: org.matsim.*
 * RandomDijkstraRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.christoph.network.SubLink;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.SubNode;
import playground.christoph.router.util.MyDijkstraFactory;
import playground.christoph.router.util.PersonLeastCostPathCalculator;
import playground.christoph.router.util.SimpleRouter;

public class RandomDijkstraRoute extends SimpleRouter implements Cloneable{

	protected static int errorCounter = 0;
	
	protected boolean removeLoops = false;
	protected double dijkstraProbability = 0.0;
	protected LeastCostPathCalculator leastCostPathCalculator;
	protected TravelCost travelCost;
	protected TravelTime travelTime;
	protected int maxLinks = 50000; // maximum number of links in a created plan
	
	private final static Logger log = Logger.getLogger(RandomDijkstraRoute.class);
	
	public RandomDijkstraRoute(Network network, TravelCost travelCost, TravelTime travelTime)
	{	
		super(network);
		this.travelCost = travelCost;
		this.travelTime = travelTime;
		this.leastCostPathCalculator = new MyDijkstraFactory().createPathCalculator((NetworkLayer)network, travelCost, travelTime);
	}
		
	public Path calcLeastCostPath(Node fromNode, Node toNode, double startTime)
	{
		return findRoute(fromNode, toNode);
	}
	
	private Path findRoute(Node fromNode, Node toNode)
	{
		Node currentNode = fromNode;
		Link currentLink;
		double routeLength = 0.0;
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();
		
		Network nw = knowledgeTools.getSubNetwork(this.person, this.network);
		
		boolean useKnowledge = false;
		if (nw instanceof SubNetwork)
		{
			SubNetwork subNetwork = (SubNetwork) nw;
			
			/*
			 * Replace the CurrentNode with its child in the SubNetwork
			 */
			currentNode = subNetwork.getNodes().get(currentNode.getId());
						
			useKnowledge = true;
			
			if (this.leastCostPathCalculator instanceof PersonLeastCostPathCalculator)
			{
				((PersonLeastCostPathCalculator) this.leastCostPathCalculator).setPerson(this.person);
			}
			
			if (this.leastCostPathCalculator instanceof MyDijkstra)
			{
				((MyDijkstra)this.leastCostPathCalculator).setNetwork(subNetwork);
			}
		}
		
		nodes.add(fromNode);
		
		while(!currentNode.equals(toNode))
		{
			// stop searching if to many links in the generated Route...
			if (nodes.size() > maxLinks)
			{
				log.warn("Route has reached the maximum allowed length - break!");
				errorCounter++;
				break;
			}
			if (nodes.size() > maxLinks)
			{
				log.warn("Routelength has reached the maximum allowed number of links - stop searching!");
				break;
			}
			
			Link[] linksArray = currentNode.getOutLinks().values().toArray(new Link[currentNode.getOutLinks().size()]);
					
			if (linksArray.length == 0)
			{
				log.error("Looks like Node is a dead end. Routing could not be finished!");
				break;
			}
			
			Link nextLink = null;
			
			/*
			 * Choose next Link randomly or dijkstra based?
			 *
			 * Select random number, if the random number is bigger than the dijkstraProbabilty.
			 * If that's not the case, nothing has to be done - the current nextLink is selected by
			 * the Dijkstra Algorithm.
			 */
			double randomDouble = random.nextDouble();

			// get the Link with the random Algorithm
			if (randomDouble > dijkstraProbability) nextLink = linksArray[random.nextInt(linksArray.length)];
			
			// get the Link with the Dijkstra Algorithm
			else
			{			
				Path path = this.leastCostPathCalculator.calcLeastCostPath(currentNode, toNode, Time.UNDEFINED_TIME);
				
				if (path.links.size() > 0) nextLink = path.links.get(0);
				
				/*
				 * Replace the nextLink with its child in the SubNetwork
				 */
				if (useKnowledge)
				{
					SubNetwork subNetwork = (SubNetwork) nw;
					
					if (nextLink == null)
					{
						log.error("Link not found!");
					}
					
					nextLink = subNetwork.getLinks().get(nextLink.getId());
					if (nextLink == null)
					{
						log.error("Link not found!");
					}
				}
			}
						
			// make the chosen link to the current link
			if(nextLink != null)
			{
				currentLink = nextLink;
				currentNode = currentLink.getToNode();
				routeLength = routeLength + currentLink.getLength();
			}
			else
			{
				log.error("Number of Links " + linksArray.length);
				log.error("Return object was null!");
				break;
			}
			
			if (useKnowledge)
			{
				nodes.add(((SubNode)currentNode).getParentNode());
				links.add(((SubLink)currentLink).getParentLink());
			}
			else
			{
				nodes.add(currentNode);
				links.add(currentLink);
			}
		}	// while(!currentNode.equals(toNode))

		Path path = new Path(nodes, links, 0, 0);

		if (maxLinks == path.links.size())
		{
//			log.info("LinkCount " + path.links.size() + " distance " + routeLength);
		}

		if (removeLoops) loopRemover.removeLoops(path);
				
		return path;
	}
		
	public static int getErrorCounter()
	{
		return errorCounter;
	}
	
	public static void setErrorCounter(int i)
	{
		errorCounter = i;
	}
	
	@Override
	public RandomDijkstraRoute clone()
	{		
		RandomDijkstraRoute clone = new RandomDijkstraRoute(this.network, this.travelCost, this.travelTime);
		clone.dijkstraProbability = this.dijkstraProbability;
		clone.maxLinks = this.maxLinks;
		clone.removeLoops = this.removeLoops;
		
		return clone;
	}
}