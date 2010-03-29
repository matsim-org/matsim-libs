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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

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
	private long timeout = 604800000; // default 1 week

	// this is not very nice...: keep the leastCostPath in mind (path on level zero) during the
	// getPaths() method.
	private Path leastCostPath = null;

	// this is also not very nice: to keep the sart time for calc one path set
	private long startTimeMilliseconds = System.currentTimeMillis();

	private int routeCnt = 0;

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

	public final void setTimeout(long timeout) {
		if ((timeout < 1000) || (timeout > (604800000))) { log.warn("timeout: "+timeout+" must be between 1 sec (1000 msec) and 7 days (604'800'000 msec). Keeping previous timeout: "+this.variationFactor); }
		else { this.timeout = timeout; }
	}

	public final boolean setODPair(Node fromNode, Node toNode) {
		if (fromNode == null) { log.warn("Origin node must exist."); return false; }
		if (network.getNodes().get(fromNode.getId()) == null) { log.warn("Origin node does not exist in the network."); return false; }

		if (toNode == null) { log.warn("Destination node must exist."); return false; }
		if (network.getNodes().get(toNode.getId()) == null) { log.warn("Destination node does not exist in the network."); return false; }

		if (fromNode.equals(toNode)) { log.warn("Origin equals to Destination not allowed."); return false; }
		origin = fromNode;
		destination = toNode;
		return true;
	}

	public final Tuple<Path,List<Path>> getPaths() {

		// set calculation start time
		startTimeMilliseconds = System.currentTimeMillis();
		log.debug(" measurement started at "+startTimeMilliseconds+" with timeout "+timeout+"...");

		// setup and run the recursion
		List<Set<Link>> excludingLinkSets = new LinkedList<Set<Link>>();
		excludingLinkSets.add(new HashSet<Link>());
		Set<Path> paths = new HashSet<Path>();
		routeCnt = 0;
		generate(0,excludingLinkSets,paths);

		// remove the least cost path from the paths
		// this is not very nice... (see generate(...) why)
		paths.remove(leastCostPath);
		// remove randomly as many paths until nofPath-1 are remaining
		List<Path> tmpPaths = new LinkedList<Path>(paths);
		while (tmpPaths.size() > (nofPaths-1)) { tmpPaths.remove(MatsimRandom.getRandom().nextInt(tmpPaths.size())); }
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
		((NodeImpl)link.getFromNode()).removeOutLink(link);
		((NodeImpl)link.getToNode()).removeInLink(link);
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
		log.debug("start level "+level);

		// for EARLY ABORT: shuffle the excludingLinkSets
		Collections.shuffle(excludingLinkSets,MatsimRandom.getRandom());

		// the set of excluding link sets for the NEXT tree level
		List<Set<Link>> newExcludingLinkSets = new LinkedList<Set<Link>>();

		// go through all given link sets for THIS level
		int setCnt = 0;
		for (Set<Link> linkSet : excludingLinkSets) {
			setCnt++;

			// remove the links from the network, calculate the least cost path and put the links back where they were
			for (Link l : linkSet) { removeLinkFromNetwork(l); }
			Path path = router.calcLeastCostPath(origin,destination,depTime);
			routeCnt++;
			for (Link l : linkSet) { addLinkToNetwork(l); }

			// check if there is a path from O to D (if not, that part of the recursion tree does not have to be expanded)
			if (path != null) {

				// add path to the path set (if not yet exists)
				if (!containsPath(paths,path)) {
					paths.add(path);
					log.debug("  path added (nofPath="+paths.size()+"; nofRemainingSets="+(excludingLinkSets.size()-setCnt)+")");
				}

				// this is not very nice...: keep the leastCostPath in mind (path on level zero)
				if (level == 0) { leastCostPath = path; }

				// EARLY ABORT: if the excludingLinkSets are shuffled already, there is no
				// need to go through the whole level anymore. Therefore,
				// if the number of paths is already enough, stop the process right here
				if (paths.size() >= (nofPaths*variationFactor)) {
					log.debug("  number of paths("+paths.size()+") >= nofPaths("+nofPaths+") * variationFactor("+variationFactor+")");
					log.debug("  ==> found enough paths from node "+origin.getId()+" to node "+destination.getId()+".");
					log.debug("end level "+level);
					printSummary(origin, destination, System.currentTimeMillis()-startTimeMilliseconds, paths.size(), routeCnt, level, "OK");
					return;
				}

				// TIMEOUT ABORT
				if (System.currentTimeMillis() > (startTimeMilliseconds+timeout)) {
					log.debug("  number of paths("+paths.size()+") < nofPaths("+nofPaths+") * variationFactor("+variationFactor+")");
					log.debug("  ==> calculation timeout ("+timeout+" msec) reached for from node "+origin.getId()+" to node "+destination.getId()+".");
					log.debug("end level "+level);
					printSummary(origin, destination, System.currentTimeMillis()-startTimeMilliseconds, paths.size(), routeCnt, level, "TIMEOUT");
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
			log.debug("  number of paths("+paths.size()+") < nofPaths("+nofPaths+") * variationFactor("+variationFactor+")");
			log.debug("  ==> there are no more paths from node "+origin.getId()+" to node "+destination.getId()+".");
			log.debug("end level "+level);
			printSummary(origin, destination, System.currentTimeMillis()-startTimeMilliseconds, paths.size(), routeCnt, level, "NOMOREPATH");
		}
		// not enough paths found yet and therefore go into the next tree level
		else {
			log.debug("  newExcludingLinkIdSets.size() = "+newExcludingLinkSets.size());
			log.debug("  paths.size()                  = "+paths.size());
			log.debug("end level "+level);
			level++;
			generate(level,newExcludingLinkSets,paths);
		}
	}

	private final void printSummary(Node o, Node d, long ctime, int pathCnt, int routeCnt, int level, String type) {
		log.info("PATHSETSUMMARY: o = "+o.getId()+"; d = "+d.getId()+"; comptime = "+ctime+"; pathCnt = "+pathCnt+"; routesCalcCnt = "+routeCnt+"; level = "+level+"; type = "+type);
	}
}
