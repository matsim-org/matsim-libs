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
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.router.util.LeastCostPathCalculator;


public class TabuRoute implements LeastCostPathCalculator {

	private final static Logger log = Logger.getLogger(TabuRoute.class);

	protected Random random;
	
	/**
	 * Default constructor.
	 *
	 * @param random
	 * 			  Random number generator. Needed to create reproducible results.           
	 *            
	 */
	public TabuRoute(Random random)
	{
		this.random = random;
	}
	
	public TabuRoute() 
	{
		this.random = new Random();
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
	
		// erster Schleifendurchlauf -> kein vorheriger Node!
		Node previousNode = null;
		
		while(!currentNode.equals(toNode))
		{
			Link[] links = currentNode.getOutLinks().values().toArray(new Link[currentNode.getOutLinks().size()]);
			
			int linkCount = links.length;

			if (linkCount == 0)
			{
				log.error("Looks like Node is a dead end. Routing could not be finished!");
				break;
			}
			
			// Link zum vorherigen Node entfernen, falls noch weitere Links verfügbar sind
			ArrayList<Link> newLinks = new ArrayList<Link>();
			for(int i = 0; i < links.length; i++)
			{
				// Falls der Link nicht zurück zum aktuellen Node führt -> Link übernehmen
				if (previousNode == null) newLinks.add(links[i]);	// erster Schleifendurchlauf
				else if(!links[i].getToNode().equals(previousNode)) newLinks.add(links[i]);
			}
			
			// Falls alle Links zurück zum letzten Node führen -> alle zur Auswahl hinzufügen
			if (links.length > 0 && newLinks.size() == 0)
			{
				for(int i = 0; i < links.length; i++) newLinks.add(links[i]);
				
				log.info("All available outgoing links return to the previous node, so choosing one of them!");
			}
			
			// ArrayList wieder in ein Array packen.
			links = new Link[newLinks.size()];
			for(int i = 0; i < newLinks.size(); i++) links[i] = newLinks.get(i);
			
			// Node wählen
			int nextLink = random.nextInt(linkCount);
			
			// den gewählten Link zum neuen CurrentLink machen
			if(links[nextLink] instanceof Link)
			{
				currentLink = links[nextLink];
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
		
		return route;
	}
	
}