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

package playground.balmermi.routeset2;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
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

	private final NetworkImpl network;
	private final double avLinkDensityPerNodeNetwork;
	private final double avLinkDensityPerNonePassNodeNetwork;

	private final Map<Id,StreetSegment> l2sMapping = new HashMap<Id,StreetSegment>();
	private final double avIncidentNodeDensityPerNodeNetwork;
	private final double avIncidentNodeDensityPerNonePassNodeNetwork;

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

	public PathSetGenerator(final NetworkImpl network) {
		if (network == null) { throw new RuntimeException("Network must exist."); }
		this.network = network;
		this.frespeedCost = new FreespeedTravelTimeCost(new CharyparNagelScoringConfigGroup());
		PreProcessLandmarks preProcessLandmarks = new PreProcessLandmarks(this.frespeedCost);
		preProcessLandmarks.run(network);
		this.router = new AStarLandmarks(this.network,preProcessLandmarks,this.frespeedCost);
		this.initStreetSegments();

		// calc network densities
		double linkDensity = 0.0;
		double nodeDensity = 0.0;
		for (Node n : network.getNodes().values()) {
			linkDensity += ((NodeImpl) n).getIncidentLinks().size();
			nodeDensity += ((NodeImpl) n).getIncidentNodes().size();
		}
		linkDensity /= network.getNodes().size();
		nodeDensity /= network.getNodes().size();
		this.avLinkDensityPerNodeNetwork = linkDensity;
		this.avIncidentNodeDensityPerNodeNetwork = nodeDensity;

		// calc street segment node density
		Set<Id> nodeIds = new HashSet<Id>(this.network.getNodes().size());
		for (StreetSegment s : l2sMapping.values()) {
			nodeIds.add(s.getFromNode().getId());
			nodeIds.add(s.getToNode().getId());
		}
		linkDensity = 0.0;
		nodeDensity = 0.0;
		for (Id nid : nodeIds) {
			linkDensity += ((NodeImpl) this.network.getNodes().get(nid)).getIncidentLinks().size();
			nodeDensity += ((NodeImpl) this.network.getNodes().get(nid)).getIncidentNodes().size();
		}
		this.avLinkDensityPerNonePassNodeNetwork = linkDensity/nodeIds.size();
		this.avIncidentNodeDensityPerNonePassNodeNetwork = nodeDensity/nodeIds.size();

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
		List<Set<StreetSegment>> excludingStreetSegmentSets = new LinkedList<Set<StreetSegment>>();
		excludingStreetSegmentSets.add(new HashSet<StreetSegment>());
		Set<Path> paths = new HashSet<Path>();
		routeCnt = 0;
		generate(0,excludingStreetSegmentSets,paths);

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

	public final void printL2SMapping() {
		for (Id id : l2sMapping.keySet()) {
			System.out.println(id.toString()+"\t"+l2sMapping.get(id).getId());
		}
	}
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void initStreetSegments() {
		log.info("init street segments...");
		for (Link l : network.getLinks().values()) {

			// find the beginning of the "oneway path" or "twoway path"
			Link currLink = l;
			NodeImpl fromNode = (NodeImpl) currLink.getFromNode();
			while ((fromNode.getIncidentNodes().size() == 2) &&
					(((fromNode.getOutLinks().size() == 1) && (fromNode.getInLinks().size() == 1)) ||
					((fromNode.getOutLinks().size() == 2) && (fromNode.getInLinks().size() == 2)))) {
				Iterator<? extends Link> linkIt = fromNode.getInLinks().values().iterator();
				Link prevLink = linkIt.next();
				if (prevLink.getFromNode().getId().equals(currLink.getToNode().getId())) { prevLink = linkIt.next(); }
				currLink = prevLink;
				fromNode = (NodeImpl) currLink.getFromNode();
			}

			// create the street segment for the whole "one- or twoway path" (if not already exists)
			StreetSegment s = l2sMapping.get(currLink.getId());
			if (s == null) {
				s = new StreetSegment(new IdImpl("s"+currLink.getId()),currLink.getFromNode(),currLink.getToNode(),network,1,1,1,1);
				s.links.add(currLink);
				l2sMapping.put(currLink.getId(),s);
				NodeImpl toNode = (NodeImpl) currLink.getToNode();
				while ((toNode.getIncidentNodes().size() == 2) &&
						(((toNode.getOutLinks().size() == 1) && (toNode.getInLinks().size() == 1)) ||
						((toNode.getOutLinks().size() == 2) && (toNode.getInLinks().size() == 2)))) {
					Iterator<? extends Link> linkIt = toNode.getOutLinks().values().iterator();
					Link nextLink = linkIt.next();
					if (nextLink.getToNode().getId().equals(currLink.getFromNode().getId())) { nextLink = linkIt.next(); }
					currLink = nextLink;
					toNode = (NodeImpl) currLink.getToNode();
					s.links.add(currLink);
					l2sMapping.put(currLink.getId(),s);
					s.setToNode(toNode);
				}
			}
		}
//		log.info("  Number of links in the network:         "+network.getLinks().size());
//		log.info("  Number of links in the mapping:         "+l2sMapping.size());
//		log.info("  Number of street segments: "+streetSegmentCnt);
//		Set<StreetSegment> segments = new HashSet<StreetSegment>(l2sMapping.values());
//		log.info("  Number of street segments:              "+segments.size());
//		int lcnt = 0;
//		for (StreetSegment s : segments) { for (Link l : s.links) { lcnt++; } }
//		log.info("  Number of links in the street segments: "+lcnt);
		log.info("done.");
	}

	private void addLinkToNetwork(Link link) {
		link.getFromNode().addOutLink(link);
		link.getToNode().addInLink(link);
	}

	private void removeLinkFromNetwork(Link link) {
		((NodeImpl) link.getFromNode()).removeOutLink(link);
		((NodeImpl) link.getToNode()).removeInLink(link);
	}

	private final boolean containsPath(Set<Path> paths, Path path) {
		for (Path p : paths) {
			if (p.links.equals(path.links)) { return true; }
		}
		return false;
	}

	private final boolean containsStreetSegmentIdSet(List<Set<StreetSegment>> streetSegmentSets, Set<StreetSegment> streetSegmentSet) {
		for (Set<StreetSegment> set : streetSegmentSets) {
			if (set.equals(streetSegmentSet)) { return true; }
		}
		return false;
	}

	private final void generate(int level, List<Set<StreetSegment>> excludingStreetSegmentSets, Set<Path> paths) {
		log.debug("start level "+level);

		// for EARLY ABORT: shuffle the excludingLinkSets
		Collections.shuffle(excludingStreetSegmentSets,MatsimRandom.getRandom());

		// the set of excluding link sets for the NEXT tree level
		List<Set<StreetSegment>> newExcludingStreetSegmentSets = new LinkedList<Set<StreetSegment>>();

		// go through all given link sets for THIS level
		int setCnt = 0;
		for (Set<StreetSegment> streetSegmentSet : excludingStreetSegmentSets) {
			setCnt++;

			// remove the links from the network, calculate the least cost path and put the links back where they were
			for (StreetSegment segment : streetSegmentSet) {
				for (Link l : segment.links) {
					removeLinkFromNetwork(l);
				}
			}
			Path path = router.calcLeastCostPath(origin,destination,depTime);
			routeCnt++;
			for (StreetSegment segment : streetSegmentSet) {
				for (Link l : segment.links) {
					addLinkToNetwork(l);
				}
			}

			// check if there is a path from O to D (if not, that part of the recursion tree does not have to be expanded)
			if (path != null) {

				// add path to the path set (if not yet exists)
				if (!containsPath(paths,path)) {
					paths.add(path);
					log.debug("  path added (nofPath="+paths.size()+"; nofRemainingSets="+(excludingStreetSegmentSets.size()-setCnt)+")");
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
					printSummary(origin, destination, System.currentTimeMillis()-startTimeMilliseconds, paths.size(), routeCnt, level,"OK", leastCostPath);
					return;
				}

				// TIMEOUT ABORT
				if (System.currentTimeMillis() > (startTimeMilliseconds+timeout)) {
					log.debug("  number of paths("+paths.size()+") < nofPaths("+nofPaths+") * variationFactor("+variationFactor+")");
					log.debug("  ==> calculation timeout ("+timeout+" msec) reached for from node "+origin.getId()+" to node "+destination.getId()+".");
					log.debug("end level "+level);
					printSummary(origin, destination, System.currentTimeMillis()-startTimeMilliseconds, paths.size(), routeCnt, level, "TIMEOUT", leastCostPath);
					return;
				}

				// no matter if the path already exists in the path list, that element of the recursion tree needs to be expanded.
				// Therefore, add new excluding link set for the NEXT tree level
				// TODO balmermi: set the right street segments
				for (Link l : path.links) {
					Set<StreetSegment> newExcludingStreetSegmentSet = new HashSet<StreetSegment>(streetSegmentSet.size()+1);
					newExcludingStreetSegmentSet.addAll(streetSegmentSet);
					StreetSegment s = l2sMapping.get(l.getId());
					if (s == null) { log.fatal("THIS MUST NOT HAPPEN (linkid="+l.getId()+")"); }
					newExcludingStreetSegmentSet.add(l2sMapping.get(l.getId()));
					if (!containsStreetSegmentIdSet(newExcludingStreetSegmentSets,newExcludingStreetSegmentSet)) {
						newExcludingStreetSegmentSets.add(newExcludingStreetSegmentSet);
					}

//					Set<Link> newExcludingLinkSet = new HashSet<Link>(linkSet.size()+1);
//					newExcludingLinkSet.addAll(linkSet);
//					newExcludingLinkSet.add(l);
//					if (!containsLinkIdSet(newExcludingLinkSets,newExcludingLinkSet)) {
//						newExcludingLinkSets.add(newExcludingLinkSet);
//					}
				}
			}
		}

		// nothing more to expand and therefore, no next tree level
		if (newExcludingStreetSegmentSets.isEmpty()) {
			log.debug("  number of paths("+paths.size()+") < nofPaths("+nofPaths+") * variationFactor("+variationFactor+")");
			log.debug("  ==> there are no more paths from node "+origin.getId()+" to node "+destination.getId()+".");
			log.debug("end level "+level);
			printSummary(origin, destination, System.currentTimeMillis()-startTimeMilliseconds, paths.size(), routeCnt, level, "NOMOREPATH", leastCostPath);
		}
		// not enough paths found yet and therefore go into the next tree level
		else {
			log.debug("  newExcludingLinkIdSets.size() = "+newExcludingStreetSegmentSets.size());
			log.debug("  paths.size()                  = "+paths.size());
			log.debug("end level "+level);
			level++;
			generate(level,newExcludingStreetSegmentSets,paths);
		}
	}

	private final void printSummary(Node o, Node d, long ctime, int pathCnt, int routeCnt, int level, String type, Path leastCostPath) {
		double eDist = Math.sqrt(
				(d.getCoord().getX()-o.getCoord().getX())*(d.getCoord().getX()-o.getCoord().getX())+
				(d.getCoord().getY()-o.getCoord().getY())*(d.getCoord().getY()-o.getCoord().getY()));
		double distLCP = 0.0;
		for (Link l : leastCostPath.links) { distLCP += l.getLength(); }
		int nofNonePassNodesLCP = 0;
		double avLinkDensityPerNodeLCP = 0.0;
		double avIncidentNodeDensityPerNodeLCP = 0.0;
		double avLinkDensityPerNonePassNodeLCP = 0.0;
		double avIncidentNodeDensityPerNonePassNodeLCP = 0.0;
		for (Node n2 : leastCostPath.nodes) {
			NodeImpl n = (NodeImpl) n2;
			if ((n.getIncidentNodes().size() == 2) &&
			    (((n.getOutLinks().size() == 1) && (n.getInLinks().size() == 1)) ||
			     ((n.getOutLinks().size() == 2) && (n.getInLinks().size() == 2)))) {
			}
			else {
				nofNonePassNodesLCP++;
				avLinkDensityPerNonePassNodeLCP += n.getIncidentLinks().size();
				avIncidentNodeDensityPerNonePassNodeLCP += n.getIncidentNodes().size();
			}
			avLinkDensityPerNodeLCP += n.getIncidentLinks().size();
			avIncidentNodeDensityPerNodeLCP += n.getIncidentNodes().size();
		}
		avLinkDensityPerNodeLCP /= leastCostPath.nodes.size();
		avIncidentNodeDensityPerNodeLCP /= leastCostPath.nodes.size();
		avLinkDensityPerNonePassNodeLCP /= nofNonePassNodesLCP;
		avIncidentNodeDensityPerNonePassNodeLCP /= nofNonePassNodesLCP;

		log.info("PATHSETSUMMARY: o = "+o.getId()+
				"; d = "+d.getId()+
				"; comptime = "+ctime+
				"; pathCnt = "+pathCnt+
				"; routesCalcCnt = "+routeCnt+
				"; level = "+level+
				"; type = "+type+
				"; eDistOD = "+eDist+
				"; distLCP = "+distLCP+
				"; nofNodesLCP = "+leastCostPath.nodes.size()+
				"; nofNonePassNodesLCP = "+nofNonePassNodesLCP+
				"; avLinkDensityPerNodeLCP = "+avLinkDensityPerNodeLCP+
				"; avIncidentNodeDensityPerNodeLCP = "+avIncidentNodeDensityPerNodeLCP+
				"; avLinkDensityPerNonePassNodeLCP = "+avLinkDensityPerNonePassNodeLCP+
				"; avIncidentNodeDensityPerNonePassNodeLCP = "+avIncidentNodeDensityPerNonePassNodeLCP+
				"; avLinkDensityPerNodeNetwork = "+this.avLinkDensityPerNodeNetwork+
				"; avIncidentNodeDensityPerNodeNetwork = "+this.avIncidentNodeDensityPerNodeNetwork+
				"; avLinkDensityPerNonePassNodeNetwork = "+this.avLinkDensityPerNonePassNodeNetwork+
				"; avIncidentNodeDensityPerNonePassNodeNetwork = "+this.avIncidentNodeDensityPerNonePassNodeNetwork);
	}
}
