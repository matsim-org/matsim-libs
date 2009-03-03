/* *********************************************************************** *
 * project: org.matsim.*
 * PathSetGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi.routeset;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.router.Dijkstra;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.router.util.LeastCostPathCalculator.Path;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.misc.Time;

public class PathSetGenerator {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private final static Logger log = Logger.getLogger(PathSetGenerator.class);

	private final NetworkLayer network;
	private final FreespeedTravelTimeCost frespeedCost;
	private final LeastCostPathCalculator router;

	private Node origin = null;
	private Node destination = null;
	private int nofRoutes = 20; // default
	private double variationFactor = 1.0; // default
	private double depTime = Time.UNDEFINED_TIME;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PathSetGenerator(final NetworkLayer network) {
		if (network == null) { throw new RuntimeException("Network must exist."); }
		this.network = network;
		this.frespeedCost = new FreespeedTravelTimeCost(new CharyparNagelScoringConfigGroup());
		this.router = new Dijkstra(this.network,this.frespeedCost,this.frespeedCost);
	}
	
	//////////////////////////////////////////////////////////////////////
	// public methods
	//////////////////////////////////////////////////////////////////////
	
	public final void setOrigin(Node node) {
		if (node == null) { throw new RuntimeException("Origin node must exist."); }
		if (network.getNode(node.getId()) == null) { throw new RuntimeException("Origin node does not exist in the network."); }
		origin = node;
	}
	
	public final void setDestination(Node node) {
		if (node == null) { throw new RuntimeException("Destination node must exist."); }
		if (network.getNode(node.getId()) == null) { throw new RuntimeException("Destination node does not exist in the network."); }
		destination = node;
	}
	
	public final Set<Path> getPaths() {
		Set<Set<Link>> excludingLinkSets = new HashSet<Set<Link>>();
		excludingLinkSets.add(new HashSet<Link>());
		Set<Path> paths = new HashSet<Path>();
		generate(0,excludingLinkSets,paths);
		return paths;
	}
	
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private void addLinkToNetwork(Link link) {
		link.getFromNode().addOutLink(link);
		link.getToNode().addInLink(link);
	}

	private void removeLinkFromNetwork(Link link) {
		link.getFromNode().removeOutLink(link);
		link.getToNode().removeInLink(link);
	}

	private final boolean containsPath(Set<Path> paths, Path path) {
		for (Path p : paths) {
			if (p.links.equals(path.links)) { return true; }
		}
		return false;
	}
	
	private final boolean containsLinkIdSet(Set<Set<Link>> linkSets, Set<Link> linkSet) {
		for (Set<Link> set : linkSets) {
			if (set.equals(linkSet)) { return true; }
		}
		return false;
	}

	private final void generate(int level, Set<Set<Link>> excludingLinkSets, Set<Path> paths) {
		log.info("--- start level "+level+" ---");
		
		Set<Set<Link>> newExcludingLinkSets = new HashSet<Set<Link>>();
		
		for (Set<Link> linkSet : excludingLinkSets) {
			for (Link l : linkSet) { removeLinkFromNetwork(l); }
			Path path = router.calcLeastCostPath(origin,destination,Time.UNDEFINED_TIME);
			for (Link l : linkSet) { addLinkToNetwork(l); }
			if (path != null) {
				if (!containsPath(paths,path)) {
					paths.add(path);
				}
				for (Link l : path.links) {
					Set<Link> newExcludingLinkSet = new HashSet<Link>(linkSet.size()+1);
					newExcludingLinkSet.addAll(linkSet);
					newExcludingLinkSet.add(l);
					if (!containsLinkIdSet(newExcludingLinkSets,newExcludingLinkSet)) {
						newExcludingLinkSets.add(newExcludingLinkSet);
					}
				}
			}
		}
		
		log.info("  newExcludingLinkIdSets.size() = "+newExcludingLinkSets.size());
		log.info("  paths.size()                  = "+paths.size());
		if (!newExcludingLinkSets.isEmpty()) {
			log.info("--- end level "+level+" ---");
			level++;
			generate(level,newExcludingLinkSets,paths);
		}
		else {
			log.info("--- end level "+level+" ---");
			log.info("--- end of recursion ---");
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// main method
	//////////////////////////////////////////////////////////////////////
	
	public static void main(String[] args) {
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("../../input/network.xml.gz");
		PathSetGenerator gen = new PathSetGenerator(network);
		gen.setOrigin(network.getNode(new IdImpl(1)));
		gen.setDestination(network.getNode(new IdImpl(8)));
		Set<Path> paths = gen.getPaths();
		log.info("PATH_SIZE: "+paths.size());
	}
}
