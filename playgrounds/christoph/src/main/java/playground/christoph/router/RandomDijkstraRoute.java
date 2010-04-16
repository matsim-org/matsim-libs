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
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.christoph.network.SubLink;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.SubNode;
import playground.christoph.router.util.CloningDijkstraFactory;
import playground.christoph.router.util.SimpleRouter;

public class RandomDijkstraRoute extends SimpleRouter {

	protected static int errorCounter = 0;
	
	protected boolean removeLoops = false;
	protected double dijkstraWeightFactor = 0.5;
	protected LeastCostPathCalculator leastCostPathCalculator;
	protected PersonalizableTravelCost travelCost;
	protected TravelTime travelTime;
	protected int maxLinks = 50000; // maximum number of links in a created plan
	
	private final static Logger log = Logger.getLogger(RandomDijkstraRoute.class);
	
	public RandomDijkstraRoute(Network network, PersonalizableTravelCost travelCost, TravelTime travelTime)
	{	
		super(network);
		this.travelCost = travelCost;
		this.travelTime = travelTime;
		this.leastCostPathCalculator = new CloningDijkstraFactory().createPathCalculator((NetworkLayer)network, travelCost, travelTime);
	}
	
	public Path calcLeastCostPath(Node fromNode, Node toNode, double startTime)
	{
		return findRoute(fromNode, toNode);
	}
	
	public void setDijsktraWeightFactor(double weightFactor)
	{
		this.dijkstraWeightFactor = weightFactor;
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
			 * Replace the given Nodes with their child in the SubNetwork 
			 */
			currentNode = subNetwork.getNodes().get(currentNode.getId());
			fromNode = subNetwork.getNodes().get(fromNode.getId());
			toNode = subNetwork.getNodes().get(toNode.getId());
			
			if (currentNode == null)
			{
				log.error("null!");
			}
			
			useKnowledge = true;
			
			this.travelCost.setPerson(person);
			
			if (this.leastCostPathCalculator instanceof SubNetworkDijkstra)
			{
				((SubNetworkDijkstra)this.leastCostPathCalculator).setNetwork(subNetwork);
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
							
			/*
			 * Dijkstra
			 */
			double[] dijkstraProbabilities = new double[linksArray.length];
						
			Path path = this.leastCostPathCalculator.calcLeastCostPath(currentNode, toNode, Time.UNDEFINED_TIME);
			
			Link dijskstraNextLink = null;
			if (path.links.size() > 0) dijskstraNextLink = path.links.get(0);
			
			// Replace the dijskstraNextLink with its child in the SubNetwork
			if (useKnowledge)
			{
				SubNetwork subNetwork = (SubNetwork) nw;
				dijskstraNextLink = subNetwork.getLinks().get(dijskstraNextLink.getId());
			}

			// set Probabilities
			for(int i = 0; i < dijkstraProbabilities.length; i++)
			{
				Link link = linksArray[i];
				if(link.equals(dijskstraNextLink)) dijkstraProbabilities[i] = 1.0;
				else dijkstraProbabilities[i] = 0.0;
			}
			
			/*
			 * Random
			 */
			double[] randomProbabilities = new double[linksArray.length];
			for(int i = 0; i < randomProbabilities.length; i++) randomProbabilities[i] = 1.0 / randomProbabilities.length;
			
			/*
			 * Merge Dijkstra and Random based on the dijkstraProbability.
			 * The higher the probability the more likely the next Link is
			 * chosen based on the results from the Dijkstra Algorithm.
			 * (p = 1.0 means that a Dijsktra Route will be returned) 
			 */
			for(int i = 0; i < dijkstraProbabilities.length; i++) dijkstraProbabilities[i] = dijkstraProbabilities[i] * dijkstraWeightFactor;
			for(int i = 0; i < randomProbabilities.length; i++) randomProbabilities[i] = randomProbabilities[i] * (1.0 - dijkstraWeightFactor);
			
			double[] sumProbabilities = new double[linksArray.length];
			for (int i = 0; i < sumProbabilities.length; i++)
			{
				sumProbabilities[i] = dijkstraProbabilities[i] + randomProbabilities[i];
			}
			
			/*
			 * Select next Link based on the summarized Probabilities.
			 */
			Link nextLink = null;
			double randomDouble = random.nextDouble();
			
			double sumProb = 0.0;
			for (int i = 0; i < sumProbabilities.length; i++)
			{
				/*
				 *  If randomDouble is exactly 0.0 select the first Link.
				 *  The next if statement wouldn't do it.
				 */
				if (randomDouble == 0.0)
				{
					nextLink = linksArray[0];
					break;
				}

				if(randomDouble > sumProb && randomDouble <= sumProb + sumProbabilities[i])
				{
					nextLink = linksArray[i];
					break;
				}
							
				sumProb = sumProb + sumProbabilities[i];
			}
			
//			Replace the nextLink with its child in the SubNetwork
			if (useKnowledge)
			{
				SubNetwork subNetwork = (SubNetwork) nw;
				
				nextLink = subNetwork.getLinks().get(nextLink.getId());
			}
//-------------------------------------------------------------
//			/*
//			 * Choose next Link randomly or dijkstra based?
//			 *
//			 * Select random number, if the random number is bigger than the dijkstraProbabilty.
//			 * If that's not the case, nothing has to be done - the current nextLink is selected by
//			 * the Dijkstra Algorithm.
//			 */
//			Link nextLink = null;
//			double randomDouble = random.nextDouble();
//
//			// get the Link with the random Algorithm
//			if (randomDouble > dijkstraProbability) nextLink = linksArray[random.nextInt(linksArray.length)];
//			
//			// get the Link with the Dijkstra Algorithm
//			else
//			{			
//				Path path = this.leastCostPathCalculator.calcLeastCostPath(currentNode, toNode, Time.UNDEFINED_TIME);
//				
//				if (path.links.size() > 0) nextLink = path.links.get(0);
//				
//				/*
//				 * Replace the nextLink with its child in the SubNetwork
//				 */
//				if (useKnowledge)
//				{
//					SubNetwork subNetwork = (SubNetwork) nw;
//					
//					nextLink = subNetwork.getLinks().get(nextLink.getId());
//				}
//			}
//-------------------------------------------------------------
			
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
		clone.dijkstraWeightFactor = this.dijkstraWeightFactor;
		clone.maxLinks = this.maxLinks;
		clone.removeLoops = this.removeLoops;
		
		return clone;
	}
}