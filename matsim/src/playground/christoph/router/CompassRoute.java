/* *********************************************************************** *
 * project: org.matsim.*
 * CompassRoute.java
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
import org.matsim.network.Link;
import org.matsim.network.Node;

import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.LoopRemover;
import playground.christoph.router.util.PersonLeastCostPathCalculator;
import playground.christoph.router.util.TabuSelector;


public class CompassRoute extends PersonLeastCostPathCalculator {

	protected boolean removeLoops = true;
	protected boolean tabuSearch = true;
	protected int maxLinks = 50000; // maximum number of links in a created plan
	
	private final static Logger log = Logger.getLogger(CompassRoute.class);
	
	/**
	 * Default constructor.
	 *                    
	 */
	public CompassRoute() 
	{
		
	}

	
	public Path calcLeastCostPath(Node fromNode, Node toNode, double startTime)
	{
		return findRoute(fromNode, toNode);
	}
	
	protected Path findRoute(Node fromNode, Node toNode)
	{
		Node previousNode = null;
		Node currentNode = fromNode;
		Link currentLink;
		double routeLength = 0.0;
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		Map<Id, Node> knownNodesMap = null;
		
		// try getting Nodes from the Persons Knowledge
		knownNodesMap = KnowledgeTools.getKnownNodes(this.person);
		
		nodes.add(fromNode);
		
		while(!currentNode.equals(toNode))
		{
			// stop searching if to many links in the generated Route...
			if (nodes.size() > maxLinks) break;
			
			Link[] links = currentNode.getOutLinks().values().toArray(new Link[currentNode.getOutLinks().size()]);
			
			// Removes links, if their Start- and Endnodes are not contained in the known Nodes.
			links = KnowledgeTools.getKnownLinks(links, knownNodesMap);
	
			// if a route should not return to the previous node from the step before
			if (tabuSearch) links = TabuSelector.getLinks(links, previousNode);
			
			if (links.length == 0)
			{
				log.error("Looks like Node is a dead end. Routing could not be finished!");
				break;
			}
			
			Link nextLink = null;
			double angle = Math.PI;	// worst possible start value
			
			// get the Link with the nearest direction to the destination node
			for(int i = 0; i < links.length; i++)
			{
				if(links[i] instanceof Link)
				{
					double newAngle = calcAngle (fromNode, toNode, links[i].getToNode());
					
					//if the new direction is better than the existing one
					if (newAngle <= angle)
					{
						angle = newAngle;
						nextLink = links[i];
					}
/*					else
					{
						log.info("Angle " + angle + " newAngle " + newAngle);
					}
*/					
				}
				else
				{
					log.error("Return object was not from type Link! Class " + links[i] + " was returned!");
				}	
			}
				
			
			// make the chosen link to the current link
			if(nextLink != null)
			{
				currentLink = nextLink;
				previousNode = currentNode;
				currentNode = currentLink.getToNode();
				routeLength = routeLength + currentLink.getLength();
			}
			else
			{
				log.error("Number of Links " + links.length);
				log.error("Return object was not from type Link! Class " + nextLink + " was returned!");
				break;
			}
			nodes.add(currentNode);
		}	// while(!currentNode.equals(toNode))
		
		Path path = new Path(nodes, null, 0, 0); // FIXME [MR] collect links
		
		if (maxLinks == path.links.size())
		{
			log.info("LinkCount " + path.links.size() + " distance " + routeLength);
		}
		
		if (removeLoops) LoopRemover.removeLoops(path);
				
		return path;
	}
	
	protected double calcAngle(Node currentNode, Node toNode, Node nextLinkNode)
	{
		double v1x = nextLinkNode.getCoord().getX() - currentNode.getCoord().getX();
		double v1y = nextLinkNode.getCoord().getY() - currentNode.getCoord().getY();

		double v2x = toNode.getCoord().getX() - currentNode.getCoord().getX();
		double v2y = toNode.getCoord().getY() - currentNode.getCoord().getY();

		/* 
		 * If the link returns to the current Node no angle can't be calculated.
		 * choosing this link would be a bad idea, so return the worst possible angle.
		 * 
		 */
		if (v1x == 0.0 && v1y == 0.0) return Math.PI;
		
		/*
		 * If the nextLinkNode is the TargetNode return 0.0 so this link is chosen.
		 */
		if (nextLinkNode.equals(toNode)) return 0.0;
		
		double cosPhi = (v1x*v2x + v1y*v2y)/(java.lang.Math.sqrt(v1x*v1x+v1y*v1y) * java.lang.Math.sqrt(v2x*v2x+v2y*v2y));
		
		double phi = java.lang.Math.acos(cosPhi);

		/* 
		 * If the angle is exactly 180� return a value that is slightly smaller.
		 * Reason: if there are only links that return to the current node and links
		 * with an angle of 180� a loop could be generated.
		 * Solution: slightly reduce angles of 180� so one of them is chosen. 
		 */
		if(phi == Math.PI) phi = Math.PI - Double.MIN_VALUE;
		
		//if (phi == Double.NaN)
		if(String.valueOf(phi).equals("NaN"))
		{
			log.error("v1x " + v1x);
			log.error("v1y " + v1y);
			log.error("v2x " + v2x);
			log.error("v2y " + v2y);
		}
		
		return phi;
	}
}