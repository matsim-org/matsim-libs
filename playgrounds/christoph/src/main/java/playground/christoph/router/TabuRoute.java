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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.christoph.router.util.SimpleRouter;

public class TabuRoute extends SimpleRouter {

	protected static int errorCounter = 0;
	
	private final static Logger log = Logger.getLogger(TabuRoute.class);

	protected boolean removeLoops = false;
	protected int maxLinks = 50000; // maximum number of links in a created plan
	
	public TabuRoute(Network network) {
		super(network);
	}

	@Override
	public Path calcLeastCostPath(Node fromNode, Node toNode, double startTime, final Person person, final Vehicle vehicle) {
		return findRoute(fromNode, toNode, person, vehicle);
	}
	
	private Path findRoute(Node fromNode, Node toNode, final Person person, final Vehicle vehicle) {
		Node currentNode = fromNode;
		Link currentLink;
		double routeLength = 0.0;
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();
		
		nodes.add(fromNode);
		
		// first loop -> there is no previous node
		Node previousNode = null;
		
		while(!currentNode.equals(toNode)) {
			// stop searching if to many links in the generated Route...
			if (nodes.size() > maxLinks) 
			{
				log.warn("Route has reached the maximum allowed length - break!");
				errorCounter++;
				break;
			}
			
			Link[] linksArray = currentNode.getOutLinks().values().toArray(new Link[currentNode.getOutLinks().size()]);
			
			/*
			 * If there are no Links available something may be wrong.
			 */
			if (linksArray.length == 0) {
				log.error("Looks like Node is a dead end. Routing could not be finished!");
				break;
			}
			
			// get Links, that do not return to the previous Node
			Link[] newLinks = tabuSelector.getLinks(linksArray, previousNode);
			
			// choose link
			int nextLink = random.nextInt(newLinks.length);
			
			// make the chosen link to the new current link
			if(newLinks[nextLink] instanceof Link) {
				currentLink = newLinks[nextLink];
				previousNode = currentNode;
				currentNode = currentLink.getToNode();
				routeLength = routeLength + currentLink.getLength();
			} else {
				log.error("Return object was not from type Link! Class " + linksArray[nextLink] + " was returned!");
				break;
			}
			
			nodes.add(currentNode);
			links.add(currentLink);
		}	// while(!currentNode.equals(toNode))

		Path path = new Path(nodes, links, 0, 0);
	
		if (removeLoops) loopRemover.removeLoops(path);
				
		return path;
	}
	
	public static int getErrorCounter() {
		return errorCounter;
	}
	
	public static void setErrorCounter(int i) {
		errorCounter = i;
	}
	
	@Override
	public SimpleRouter createInstance() {
		return new TabuRoute(network);
	}
	
}