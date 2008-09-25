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

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.router.util.LeastCostPathCalculator;


public class CompassRoute implements LeastCostPathCalculator {

	private final static Logger log = Logger.getLogger(CompassRoute.class);
	
	/**
	 * Default constructor.
	 *                    
	 */
	public CompassRoute() 
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
	
		nodes.add(fromNode);
		
		while(!currentNode.equals(toNode))
		{
			Link[] links = currentNode.getOutLinks().values().toArray(new Link[currentNode.getOutLinks().size()]);
			
			int linkCount = links.length;

			if (linkCount == 0)
			{
				log.error("Looks like Node is a dead end. Routing could not be finished!");
				break;
			}
			
			Link nextLink = null;
			double angle = 0.0;
			
			// den Link suchen, dessen Richtung em ehesten Richtung Ziel zeigt
			for(int i = 0; i < linkCount; i++)
			{
				if(links[i] instanceof Link)
				{
					double newAngle = calcAngle (fromNode, toNode, links[i].getToNode());
					
					// Falls die neue Richtung "besser" ist als die alte
					if (newAngle < angle)
					{
						angle = newAngle;
						nextLink = links[i];
					}
					
				}
				else
				{
					log.error("Return object was not from type Link! Class " + links[i] + " was returned!");
				}
				
			}
				
			
			// den gewählten Link zum neuen CurrentLink machen
			if(nextLink != null)
			{
				currentLink = nextLink;
				currentNode = currentLink.getToNode();
				routeLength = routeLength + currentLink.getLength();
			}
			else
			{
				log.error("Return object was not from type Link! Class " + nextLink + " was returned!");
				break;
			}
			nodes.add(currentNode);
		}	// while(!currentNode.equals(toNode))
		
		Route route = new Route();
		route.setRoute(nodes);
		route.setDist(routeLength);
		
		return route;
	}
	
	public double calcAngle(Node currentNode, Node toNode, Node nextLinkNode)
	{
		double v1x = nextLinkNode.getCoord().getX() - currentNode.getCoord().getX();
		double v1y = nextLinkNode.getCoord().getY() - currentNode.getCoord().getY();

		double v2x = toNode.getCoord().getX() - currentNode.getCoord().getX();
		double v2y = toNode.getCoord().getY() - currentNode.getCoord().getY();

		double cosPhi = (v1x*v2x + v1y*v2y)/(java.lang.Math.sqrt(v1x*v1x+v1y*v1y) + java.lang.Math.sqrt(v2x*v2x+v2y*v2y));
		
		return java.lang.Math.acos(cosPhi);
	}
}