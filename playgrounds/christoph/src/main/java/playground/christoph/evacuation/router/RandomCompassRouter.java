/* *********************************************************************** *
 * project: org.matsim.*
 * RandomCompassRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

public class RandomCompassRouter implements IntermodalLeastCostPathCalculator {
	
	private final static Logger log = Logger.getLogger(RandomCompassRouter.class);
	
	protected final Network network;
	protected final Random random;
	protected final boolean tabuSearch;	
	protected final double compassProbability;
	protected final AcosProvider acosProvider;
	protected final int maxLinks = 20000; // maximum number of links in a created leg 
	
	protected Set<String> modeRestrictions;
	
	public RandomCompassRouter(Network network, boolean tabuSearch, double compassProbability, AcosProvider acosProvider) {
		this.network = network;
		this.tabuSearch = tabuSearch;
		this.compassProbability = compassProbability;
		this.acosProvider = acosProvider;
		
		this.random = MatsimRandom.getLocalInstance();
	}
	
	@Override
	public Path calcLeastCostPath(Node fromNode, Node toNode, double startTime, final Person person, final Vehicle vehicle) {
		return findRoute(fromNode, toNode, startTime, person, vehicle);
	}
	
	private Path findRoute(Node fromNode, Node toNode, double startTime, final Person person, final Vehicle vehicle) {
		
		/*
		 * Set a new seed in the random object which only depends on the
		 * person's id and the current time. Therefore the created random numbers
		 * do not depend on the order in which routes are created.
		 */
//		random.setSeed((long) (person.getId().hashCode() + startTime));
		random.setSeed(person.getId().hashCode() + ((int) startTime));
		
		Node previousNode = null;
		Node currentNode = fromNode;
		Link currentLink;
		double routeLength = 0.0;
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();
		
		nodes.add(fromNode);
		
		if (fromNode.getId().equals(toNode.getId())) {
			return new Path(nodes, links, 0, 0);
		}
		
		/*
		 * If the toNode is a rescue node, we have to exchange it with another node because
		 * it has no real coordinate and therefore the compass algorithm would produce
		 * wrong results. 
		 */
		boolean toRescueNode = toNode.getId().toString().contains("rescueNode");
		Node orgToNode = null;
		Link toRescueNodeLink = null;
		if (toRescueNode) {
			orgToNode = toNode;
			Collection<? extends Link> inLinks = toNode.getInLinks().values();
			
			double distance = Double.MAX_VALUE;
			for (Link link : inLinks) {
				double d = CoordUtils.calcDistance(link.getFromNode().getCoord(), fromNode.getCoord());
				if (d < distance) {
					distance = d;
					toRescueNodeLink = link;
				}
			}
			toNode = toRescueNodeLink.getFromNode();
		}
		
		while(!currentNode.equals(toNode)) {

			// stop searching if to many links in the generated Route...
			if (nodes.size() > maxLinks) {
//				log.warn("Route has reached the maximum allowed length - aborting!");
				break;
			}
			
			List<Link> outLinks = new ArrayList<Link>(currentNode.getOutLinks().values());
			Iterator<Link> iter;
			
			// if it is not a route to a rescue node, remove all rescue links
			iter = outLinks.iterator();
			while (iter.hasNext()) {
				Link link = iter.next();
				if (link.getId().toString().contains("rescueLink")) iter.remove();
			}
			
			// remove links which do not offer a compatible mode
			iter = outLinks.iterator();
			while (iter.hasNext()) {
				Link link = iter.next();
								
				// if no restrictions are set
				if (this.modeRestrictions == null) break;
				
				// if the link offers at least one required mode
				boolean keepLink = false;
				Set<String> allowedModes = link.getAllowedModes();
				for (String mode : this.modeRestrictions) {
					if (allowedModes.contains(mode)) {
						keepLink = true;
						break;
					}
				}
				
				// no compatible mode was found, therefore remove link from list
				if (!keepLink) iter.remove();
			}
			
			// if a route should not return to the previous node from the step before
			if (tabuSearch) removeNonTabuLinks(outLinks, previousNode);
		
			if (outLinks.size() == 0) {
				log.error("Looks like Node is a dead end. Routing could not be finished!");
				break;
			}
			
			Link nextLink = null;
			double angle = Math.PI;	// worst possible start value
			
			// get the Link with the nearest direction to the destination node
			for(Link link : outLinks) {
				
				double newAngle = calcAngle(fromNode, toNode, link.getToNode());
				
				// if the new direction is better than the existing one
				if (newAngle <= angle) {
					angle = newAngle;
					nextLink = link;
				}
			}

			// select next Link
			if(nextLink != null) {
				double randomDouble = random.nextDouble();
				
				/*
				 * Select random link, if the random number is bigger than the compassProbabilty.
				 * If that's not the case, nothing has to be done - the current nextLink is selected by
				 * the Compass Algorithm.
				 */
				if (randomDouble > compassProbability) nextLink = outLinks.get(random.nextInt(outLinks.size()));
			}
			
			// Compass Algorithm didn't find a link -> only choose randomly
			else {
				// choose Link
				nextLink = outLinks.get(random.nextInt(outLinks.size()));
			}
		
			// make the chosen link to the current link
			if(nextLink != null) {
				currentLink = nextLink;
				previousNode = currentNode;
				currentNode = currentLink.getToNode();
				routeLength = routeLength + currentLink.getLength();
			} else {
				log.error("nextLink was null. aborting.");
				break;
			}
			
			nodes.add(currentNode);
			links.add(currentLink);
		}	// while(!currentNode.equals(toNode))

		/*
		 * If the toNode is a rescue node, we re-add it to the route
		 */
		if (toRescueNode) {
			routeLength = routeLength + toRescueNodeLink.getLength();
			nodes.add(orgToNode);
			links.add(toRescueNodeLink);
		}
		
		Path path = new Path(nodes, links, 0, 0);

		if (maxLinks == path.links.size()) {
//			log.info("LinkCount " + path.links.size() + " distance " + routeLength);
		}
		
		return path;
	}
	
	private double calcAngle(Node currentNode, Node toNode, Node nextLinkNode) {
		double v1x = nextLinkNode.getCoord().getX() - currentNode.getCoord().getX();
		double v1y = nextLinkNode.getCoord().getY() - currentNode.getCoord().getY();

		double v2x = toNode.getCoord().getX() - currentNode.getCoord().getX();
		double v2y = toNode.getCoord().getY() - currentNode.getCoord().getY();

		/* 
		 * If the link returns to the current Node no angle can't be calculated.
		 * choosing this link would be a bad idea, so return the worst possible angle.
		 * 
		 */
		if ((v1x == 0.0) && (v1y == 0.0)) return Math.PI;
		
		/*
		 * If the nextLinkNode is the TargetNode return 0.0 so this link is chosen.
		 */
		if (nextLinkNode.equals(toNode)) return 0.0;
		
		double cosPhi = (v1x*v2x + v1y*v2y)/(Math.sqrt(v1x*v1x + v1y*v1y) * Math.sqrt(v2x*v2x + v2y*v2y));
		
//		double phi = Math.acos(cosPhi);
		double phi = acosProvider.getAcos(cosPhi);

		/* 
		 * If the angle is exactly 180 degrees return a value that is slightly smaller.
		 * Reason: if there are only links that return to the current node and links
		 * with an angle of 180 degrees a loop could be generated.
		 * Solution: slightly reduce angles of 180 degrees so one of them is chosen. 
		 */
		if(phi == Math.PI) phi = Math.PI - Double.MIN_VALUE;
		
		return phi;
	}
	
	/*
	 * Returns those outgoing links from a Node, which don't return directly to a given previous node.
	 * If all links return to the previous node, all links are returned.
	 */
	private void removeNonTabuLinks(Collection<Link> links, Node previousNode) {	
		/*
		 * If there is no previous Node (i.e. Person is at the first Node of a Leg)
		 * all available Links can be chosen!
		 */ 
		if(previousNode == null) return;
		
		// remove Links to the previous node, if other Links are available
		List<Link> counterLinks = new ArrayList<Link>();
		Iterator<Link> iter = links.iterator();
		while (iter.hasNext()) {
			Link link = iter.next();
			
			if (link.getToNode().getId().equals(previousNode.getId())) {
				counterLinks.add(link);
				iter.remove();
			}
		}
		// if no links are left, re-add counter links
		if (links.size() == 0) links.addAll(counterLinks);
	}
	
	@Override
	public void setModeRestriction(Set<String> modeRestrictions) {
		this.modeRestrictions = modeRestrictions;
	}
}