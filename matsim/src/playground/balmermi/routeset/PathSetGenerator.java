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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.LeastCostPathCalculator.Path;
import org.matsim.utils.collections.Tuple;
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
	private int nofPaths = 20; // default
	private double variationFactor = 1.0; // default
	private double depTime = Time.UNDEFINED_TIME; // not sure yet if there needs a depTime defined => setting default
	
	// this is not very nice...: keep the leastCostPath in mind (path on level zero) during the
	// getPaths() method.
	private Path leastCostPath = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PathSetGenerator(final NetworkLayer network) {
		if (network == null) { throw new RuntimeException("Network must exist."); }
		this.network = network;
		this.frespeedCost = new FreespeedTravelTimeCost(new CharyparNagelScoringConfigGroup());
		PreProcessLandmarks preProcessLandmarks = new PreProcessLandmarks(this.frespeedCost);
		preProcessLandmarks.run(network);
		this.router = new AStarLandmarks(this.network,preProcessLandmarks,this.frespeedCost);
	}
	
	//////////////////////////////////////////////////////////////////////
	// public methods
	//////////////////////////////////////////////////////////////////////
	
	public final void setPathSetSize(int nofPaths) {
		if (nofPaths < 1) { log.warn("nofPaths: "+nofPaths+" < 1 not allowed. Keeping previous PathSet size = "+this.nofPaths); }
		else { this.nofPaths = nofPaths; }
	}
	
	public final void setVariationFactor(double variationFactor) {
		if (nofPaths < 1.0) { log.warn("variationFactor: "+variationFactor+" < 1.0 not allowed. Keeping previous variation factor: "+this.variationFactor); }
		else { this.variationFactor = variationFactor; }
	}

	public final void setODPair(Node fromNode, Node toNode) {
		if (fromNode == null) { throw new RuntimeException("Origin node must exist."); }
		if (network.getNode(fromNode.getId()) == null) { throw new RuntimeException("Origin node does not exist in the network."); }

		if (toNode == null) { throw new RuntimeException("Destination node must exist."); }
		if (network.getNode(toNode.getId()) == null) { throw new RuntimeException("Destination node does not exist in the network."); }

		if (fromNode.equals(toNode)) { throw new RuntimeException("Origin equals to Destination not allowed."); }
		origin = fromNode;
		destination = toNode;
	}
	
	public final Tuple<Path,List<Path>> getPaths() {
		// setup and run the recursion
		List<Set<Link>> excludingLinkSets = new LinkedList<Set<Link>>();
		excludingLinkSets.add(new HashSet<Link>());
		Set<Path> paths = new HashSet<Path>();
		generate(0,excludingLinkSets,paths);
		
		// remove the least cost path from the paths
		// this is not very nice... (see generate(...) why)
		paths.remove(leastCostPath);
		// remove randomly as many paths until nofPath-1 are remaining
		List<Path> tmpPaths = new LinkedList<Path>(paths);
		while (tmpPaths.size() > (nofPaths-1)) { tmpPaths.remove(MatsimRandom.random.nextInt(tmpPaths.size())); }
		// create the result containing the least cost path and nofPath-1 other paths
		Tuple<Path,List<Path>> tuple = new Tuple<Path,List<Path>>(leastCostPath,tmpPaths);
		// reset the least cost path
		leastCostPath = null;
		return tuple;
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
	
	private final boolean containsLinkIdSet(List<Set<Link>> linkSets, Set<Link> linkSet) {
		for (Set<Link> set : linkSets) {
			if (set.equals(linkSet)) { return true; }
		}
		return false;
	}

	private final void generate(int level, List<Set<Link>> excludingLinkSets, Set<Path> paths) {
		log.info("start level "+level);
		
		// for EARLY ABORT: shuffle the excludingLinkSets
		Collections.shuffle(excludingLinkSets,MatsimRandom.random);

		// the set of excluding link sets for the NEXT tree level
		List<Set<Link>> newExcludingLinkSets = new LinkedList<Set<Link>>();
		
		// go through all given link sets for THIS level
		int setCnt = 0;
		for (Set<Link> linkSet : excludingLinkSets) {
			setCnt++;
			
			// remove the links from the network, calculate the least cost path and put the links back where they were
			for (Link l : linkSet) { removeLinkFromNetwork(l); }
			Path path = router.calcLeastCostPath(origin,destination,depTime);
			for (Link l : linkSet) { addLinkToNetwork(l); }
			
			// check if there is a path from O to D (if not, that part of the recursion tree does not have to be expanded)
			if (path != null) {
				
				// add path to the path set (if not yet exists)
				if (!containsPath(paths,path)) {
					paths.add(path);
					log.info("  path added (nofPath="+paths.size()+"; nofRemainingSets="+(excludingLinkSets.size()-setCnt)+")");
				}
				
				// this is not very nice...: keep the leastCostPath in mind (path on level zero)
				if (level == 0) { leastCostPath = path; }

				// EARLY ABORT: if the excludingLinkSets are shuffled already, there is no
				// need to go through the whole level anymore. Therefore,
				// if the number of paths is already enough, stop the process right here
				if (paths.size() >= (nofPaths*variationFactor)) {
					log.info("  number of paths("+paths.size()+") >= nofPaths("+nofPaths+") * variationFactor("+variationFactor+")");
					log.info("  ==> found enough paths from node "+origin.getId()+" to node "+destination.getId()+".");
					log.info("end level "+level);
					return;
				}
				
				// no matter if the path already exists in the path list, that element of the recursion tree needs to be expanded.
				// Therefore, add new excluding link set for the NEXT tree level
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
		
		// nothing more to expand and therefore, no next tree level
		if (newExcludingLinkSets.isEmpty()) {
			log.info("  number of paths("+paths.size()+") < nofPaths("+nofPaths+") * variationFactor("+variationFactor+")");
			log.info("  ==> there are no more paths from node "+origin.getId()+" to node "+destination.getId()+".");
			log.info("end level "+level);
		}
		// not enough paths found yet and therefore go into the next tree level
		else {
			log.info("  newExcludingLinkIdSets.size() = "+newExcludingLinkSets.size());
			log.info("  paths.size()                  = "+paths.size());
			log.info("end level "+level);
			level++;
			generate(level,newExcludingLinkSets,paths);
		}
	}
}
