/* *********************************************************************** *
 * project: org.matsim.*
 * BusStopAllocator.java
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

/**
 * 
 */
package playground.yu.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.matsim.transitSchedule.api.TransitScheduleWriter;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import playground.yu.utils.io.SimpleWriter;

/**
 * tries to allocate bus stop coordinations to links in MATSim "car" network
 * 
 * @author yu
 * 
 */
public class BusLineAllocator {
	private class TravelCostFunctionDistance implements TravelCost {
		/** returns only the link length */
		public double getLinkTravelCost(Link link, double time) {
			return link.getLength();
		}
	}

	private class TravelTimeFunctionFree implements TravelTime {
		/** returns only freespeedtraveltime */
		public double getLinkTravelTime(Link link, double time) {
			return ((LinkImpl) link).getFreespeedTravelTime(0);
		}
	}

	private NetworkImpl carNet;
	private Map<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> coordPairs;// <ptRouteId,<ptLink<fromNodeCoord,toNodeCoord>>>
	// private Map<Id, List<Tuple<String, Tuple<Coord, Coord>>>>
	// coordPairs4rtf;//
	// <ptRouteId,<ptLinkId:next_ptLinkId<fromNodeCoord,toNodeCoord>>>
	/*
	 * <ptRouteId,List<ptLinkId,Path_Links(shouldn't be pt linkId, but there
	 * also is exception))>>
	 */
	private Map<Id, List<Tuple<Id, List<Id>/* path */>>> paths = new HashMap<Id, List<Tuple<Id, List<Id>/*
																										 * Path.
																										 * links
																										 */>>>();
	/*
	 * <ptRouteId,List<ptLinkId:next_ptLinkId,Path(linkIds (shouldn't be pt
	 * linkIds))>>
	 */
	// private Map<Id, List<Tuple<String, List<Id>/*path*/>>> paths4rtf
	// = new HashMap<Id, List<Tuple<String, List<Id>/*
	// * Path.
	// * links
	// */>>>();
	// 
	private String outputFile;
	private Dijkstra dijkstra;
	private Link tmpPtLink = null;
	private Id tmpPtRouteId = null;
	private Set<Link> startLinks = new HashSet<Link>(),
			endLinks = new HashSet<Link>(),
			// nullLinks = new HashSet<Link>(),
			links2add = new HashSet<Link>();
	// private Set<Node> nodes2add = new HashSet<Node>();
	// private Map<Link, Node> startLinksNewToNodes = new HashMap<Link, Node>();
	private TransitSchedule schedule;
	private boolean hasStartLink = false;
	private Network multiModalNetwork;
	private static Set<TransportMode> modes = new HashSet<TransportMode>();

	/**
	 * @param netWithoutBus
	 *            a nomral MATsim <code>NetworkImpl</code>, in which there
	 *            aren't "pt" links
	 * @param stopCoords
	 *            a Collection of
	 *            <ptRouteId,<ptLinkId<fromNodeCoord,toNodeCoord>>>
	 * @param outputFile
	 *            file path of the output file
	 */
	public BusLineAllocator(NetworkImpl netWithoutBus,
			Network multiModalNetwork, TransitSchedule schedule,
			String outputFile) {
		this.carNet = netWithoutBus;
		this.multiModalNetwork = multiModalNetwork;
		this.schedule = schedule;
		this.coordPairs = createCoordPairs(schedule);
		this.outputFile = outputFile;
		this.dijkstra = new Dijkstra(netWithoutBus,
				new TravelCostFunctionDistance(), new TravelTimeFunctionFree());
		modes.add(TransportMode.car);
		// modes.add(TransportMode.pt);
		modes.add(TransportMode.bus);
	}

	protected void allocateAllRouteLinks() {
		for (Entry<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> routeLinkCoordPair : coordPairs
				.entrySet()) {
			tmpPtRouteId = routeLinkCoordPair.getKey();
			List<Tuple<Id, List<Id>/* path */>> ptLinkIdPaths = paths
					.get(tmpPtRouteId);
			if (ptLinkIdPaths == null)
				ptLinkIdPaths = new ArrayList<Tuple<Id, List<Id>/* Path. links */>>();

			for (Tuple<Link, Tuple<Coord, Coord>> linkCoordPair : routeLinkCoordPair
					.getValue()) {
				tmpPtLink = linkCoordPair.getFirst();
				List<Id>/* path */pathLinks = allocateRouteLink(linkCoordPair
						.getSecond()/* coordPair */);
				// TODO empty check
				if (!pathLinks.isEmpty())/*
										 * think? How to handle the median link,
										 * which has the same fromNode(carNet)
										 * and toNode(carNet)? It's not problem
										 * more.
										 */{
					ptLinkIdPaths.add(new Tuple<Id, List<Id>/* path */>(
							tmpPtLink.getId(), pathLinks));
				}
				/*
				 * else{pathLinks is empty, that means, tmpPtLink doesn't
				 * correspond to any "car" link. Maybe only a node, it shouldn't
				 * be a problem}
				 */
				tmpPtLink = null;
			}
			paths.put(tmpPtRouteId, ptLinkIdPaths);
		}

		/*---------------------- add links to netWithoutBus---------------------------*/
		for (Link l2a : links2add) {
			Node ptFrom = l2a.getFromNode();
			Id ptFromId = ptFrom.getId();
			Node carFrom = carNet.getNodes().get(ptFromId);
			if (carFrom == null) { /* this node with this Id dosn't exist. */
				carFrom = carNet.getFactory().createNode(ptFromId, ptFrom.getCoord());
				carNet.addNode(carFrom);
			}
			carFrom.getOutLinks().remove(l2a.getId());

			Node ptTo = l2a.getToNode();
			Id ptToId = ptTo.getId();
			Node carTo = carNet.getNodes().get(ptToId);
			if (carTo == null) {
				carTo = carNet.getFactory().createNode(ptToId, ptTo.getCoord());
				carNet.addNode(carTo);
			}
			carTo.getInLinks().remove(l2a.getId());

			Link createdLink = carNet.getFactory().createLink(l2a.getId(), carFrom.getId(), carTo.getId());
			createdLink.setLength(l2a.getLength());
			createdLink.setFreespeed(l2a.getFreespeed(0));
			createdLink.setCapacity(l2a.getCapacity(0));
			createdLink.setNumberOfLanes(l2a.getNumberOfLanes(0));
			createdLink.setAllowedModes(modes);
		}
		/*---------- check in-/outLinks of nodes of links added to netWithoutBus-------*/
		for (Link l2a : links2add) {
			Link link = carNet.getLinks().get(l2a.getId());// get link from-
			// netWithoutBus with the Id from links2add
			Node from = link.getFromNode();
			// from.getInLinks().clear();
			Set<Id> fromInLinkIds = new HashSet<Id>();
			fromInLinkIds.addAll(from.getInLinks().keySet());
			for (Id inLinkId : l2a.getFromNode().getInLinks().keySet())/*
																		 * fromNode-
																		 * InLinks
																		 */{
				if (!fromInLinkIds.contains(inLinkId)) {
					Link inLink = carNet.getLinks().get(inLinkId);
					if (inLink != null)
						from.addInLink(inLink);
				}
			}

			Set<Id> fromOutLinkIds = new HashSet<Id>();
			fromOutLinkIds.addAll(from.getOutLinks().keySet());
			// from.getOutLinks().clear();
			for (Id outLinkId : l2a.getFromNode().getOutLinks().keySet())/*
																		 * fromNode-
																		 * OutLinks
																		 */{
				if (!fromOutLinkIds.contains(outLinkId)) {
					Link outLink = carNet.getLinks().get(outLinkId);
					if (outLink != null)
						from.addOutLink(outLink);
				}
			}

			Node to = link.getToNode();
			Set<Id> toInLinkIds = new HashSet<Id>();
			toInLinkIds.addAll(to.getInLinks().keySet());
			// to.getInLinks().clear();
			for (Id inLinkId : l2a.getToNode().getInLinks().keySet())/*
																	 * toNode-InLinks
																	 */{
				if (!toInLinkIds.contains(inLinkId)) {
					Link inLink = carNet.getLinks().get(inLinkId);
					if (inLink != null)
						to.addInLink(inLink);
				}
			}

			Set<Id> toOutLinkIds = new HashSet<Id>();
			toOutLinkIds.addAll(to.getOutLinks().keySet());
			// to.getOutLinks().clear();
			for (Id outLinkId : l2a.getToNode().getOutLinks().keySet())/*
																		 * toNode-
																		 * OutLinks
																		 */{
				if (!toOutLinkIds.contains(outLinkId)) {
					Link outLink = carNet.getLinks().get(outLinkId);
					if (outLink != null)
						to.addOutLink(outLink);
				}
			}
		}
		dijkstra/* new Instance angain */= new Dijkstra(carNet,
				new TravelCostFunctionDistance(), new TravelTimeFunctionFree());

	}

	private void rectifyAllocations() {
		// Map<Id, List<Tuple<Id, List<Id>>>> tmpPaths = new HashMap<Id,
		// List<Tuple<Id, List<Id>>>>();
		// tmpPaths.putAll(paths);
		for (Entry<Id, List<Tuple<Id, List<Id>/* path */>>> routeId_ptLinkIdpathPairs : paths
				.entrySet()) {
			List<Tuple<Id, List<Id>/* path */>> ptLinkId_PathPairs = routeId_ptLinkIdpathPairs
					.getValue();

			for (int i = 0; i < ptLinkId_PathPairs.size() - 1; i++) {
				Tuple<Id, List<Id>/* path */> ptLinkId_pathPairA = ptLinkId_PathPairs
						.get(i), ptLinkId_pathPairB = ptLinkId_PathPairs
						.get(i + 1);
				List<Id> pathA = ptLinkId_pathPairA.getSecond(), pathB = ptLinkId_pathPairB
						.getSecond();

				// compare PahtA and pathB
				int sizeA = pathA.size();
				compare: for (int j = sizeA - 1; j >= 1; j--) {
					Id idA = pathA.get(j);
					for (int k = 0; k < pathB.size(); k++) {
						Id idB = pathB.get(k);
						if (idA.equals(idB)) {
							List<Id> sub = new ArrayList<Id>();
							sub.addAll(pathA.subList(j, sizeA));
							pathA.removeAll(sub);
							sub.clear();
							sub.addAll(pathB.subList(0, k));
							pathB.removeAll(sub);
							break compare;
						}
					}
				}// compare ends!

			}
		}
		// TODO

	}

	private static List<Id> links2Ids(List<Link> links) {
		List<Id> ids = new ArrayList<Id>();
		for (Link link : links) {
			ids.add(link.getId());
		}
		return ids;
	}

	// private void eliminateRedundancy() {
	// for (Entry<Id, List<Tuple<String, List<Id>/*path*/>>>
	// path4rtfEntry : paths4rtf
	// .entrySet()) {
	// Id routeId = path4rtfEntry.getKey();
	// List<Tuple<String, List<Id>/*path*/>> ptLinkIdsPaths4rtf =
	// path4rtfEntry
	// .getValue();
	// for (int j = 0; j < ptLinkIdsPaths4rtf.size(); j++) {
	//
	// Tuple<String, List<Id>/*path*/> ptLinkIdsPath4rtf =
	// ptLinkIdsPaths4rtf
	// .get(j);
	// String[] ptLinkIds4rtf = ptLinkIdsPath4rtf.getFirst()
	// .split(":");
	// List<Id>/*path*/pathA = null, pathB = null;
	// List<Tuple<Id, List<Id>>> ptLinkIdPaths = paths.get(routeId);
	//
	// Tuple<Id, List<Id>/*path*/> ptLinkIdPathA = ptLinkIdPaths
	// .get(j), ptLinkIdPathB = ptLinkIdPaths.get(j + 1);
	// String ptLinkIdStrA = ptLinkIdPathA.getFirst().toString(),
	// ptLinkIdStrB = ptLinkIdPathB
	// .getFirst().toString();
	// if (ptLinkIds4rtf[0].equals(ptLinkIdStrA))
	// pathA = ptLinkIdPathA.getSecond();
	// if (ptLinkIds4rtf[1].equals(ptLinkIdStrB))
	// pathB = ptLinkIdPathB.getSecond();
	// if (pathA == null && pathB == null) {
	// System.err
	// .println(">>>>>lineNo.380\tlinkIds don't match :\t"
	// + ptLinkIds4rtf + "\t<->\t" + ptLinkIdStrA
	// + "\t" + ptLinkIdStrB);
	// System.exit(1);
	// }
	//
	// List<Id>/*path*/rtfPath = ptLinkIdsPath4rtf.getSecond();
	// List<Id>/*path*/tmpLinksA = new ArrayList<Id>/*
	// * Path.links
	// */();
	// List<Id>/*path*/tmpLinksB = new ArrayList<Id>/*
	// * Path.links
	// */();
	// List<Id>/*path*/tmpLinksRtf = new ArrayList<Id>/*
	// * Path.links
	// */();
	// tmpLinksA.addAll(pathA);
	// for (Id linkId : tmpLinksA)
	// if (!rtfPath.contains(linkId)) {
	// pathA.remove(linkId);
	// }
	// tmpLinksB.addAll(pathB);
	// for (Id linkId : tmpLinksB)
	// if (!rtfPath.contains(linkId)) {
	// pathB.remove(linkId);
	// }
	//
	// if (!isSubList(pathA, pathB, rtfPath)) {
	// pathA.clear();
	// pathB.clear();
	// pathB.addAll(tmpLinksB);
	// pathA.addAll(tmpLinksA);
	// }
	// /* already reduced ? */
	// Id lastA = pathA.get(pathA.size() - 1), firstB = pathB.get(0);
	// Node lastANode = netWithoutBus.getLink(lastA).getToNode(), firstBNode
	// = netWithoutBus
	// .getLink(firstB).getFromNode();
	// if (!lastANode.equals(firstBNode))/* different nodes */{
	// if (!lastANode.getCoord().equals(firstBNode.getCoord()))/*
	// * differen
	// * coord
	// */{
	// Path path = dijkstra.calcLeastCostPath(lastANode,
	// firstBNode, 0);
	//
	// if (path != null) {
	// if (!path.links.isEmpty()) {
	// Coord ptLinkAToCoord = multiModalNetwork
	// .getLink(ptLinkIdStrA).getToNode()
	// .getCoord();
	// double minDist = 10000;
	// Link nearestLink = null;
	// int nearestPos = -1;
	// for (int i = 0; i < path.links.size(); i++) {
	// Link link = path.links.get(i);
	// double tmpDist = CoordUtils.calcDistance(
	// ptLinkAToCoord, link.getCoord());
	// if (tmpDist < minDist) {
	// minDist = tmpDist;
	// nearestLink = link;
	// nearestPos = i;
	// }
	// }
	// if (nearestLink != null)
	// for (int i = 0; i < path.links.size(); i++) {
	// Link link = path.links.get(i);
	// if (i <= nearestPos)
	// pathA.add(link.getId());
	// else
	// pathB.add(i - nearestPos - 1, link
	// .getId());
	// }
	// } else {
	// System.out
	// .println(">>>>>line424\tempty path.links");
	// }
	// } else/* path==null */{
	// Link ptLinkA = multiModalNetwork
	// .getLink(ptLinkIdStrA);
	// Link newLink = netWithoutBus.createAndAddLink(
	// new IdImpl(ptLinkIdStrA + "-"
	// + ptLinkIdStrB), lastANode,
	// firstBNode, CoordUtils.calcDistance(
	// lastANode.getCoord(), firstBNode
	// .getCoord()), ptLinkA
	// .getFreespeed(0), ptLinkA
	// .getCapacity(0), ptLinkA
	// .getNumberOfLanes(0));
	// pathA.add(newLink.getId());
	// }
	// } else/* same Coord */{
	// Link ptLinkA = multiModalNetwork.getLink(ptLinkIdStrA);
	// Id newLinkId = new IdImpl(ptLinkIdStrA + "-"
	// + ptLinkIdStrB);
	//
	// Link newLink = netWithoutBus.getLink(newLinkId);
	// if (newLink == null)
	// newLink = netWithoutBus.createAndAddLink(newLinkId,
	// lastANode, firstBNode, 0, ptLinkA
	// .getFreespeed(0), ptLinkA
	// .getCapacity(0), ptLinkA
	// .getNumberOfLanes(0));
	// newLink.setAllowedModes(modes);
	// pathA.add(newLink.getId());
	// }
	// }
	// }
	// }
	// netWithoutBus.connect();
	// }

	/**
	 * @param pathA
	 * @param pathB
	 * @param rtfPath
	 * @return isSubLIst==true, if pathA is the subList of rtfPath at beginning,
	 *         pahtB also is that at the end
	 */
	private boolean isSubList(List<Id> pathA, List<Id> pathB, List<Id> rtfPath) {
		if (pathA.isEmpty() || pathB.isEmpty())
			return false;
		else {
			int sizeA = pathA.size();
			for (int i = 0; i < sizeA; i++)
				if (!pathA.get(i).equals(rtfPath.get(i)))
					return false;
			int sizeB = pathB.size();
			int sizeRtf = rtfPath.size();
			for (int i = sizeB - 1; i >= 0; i--)
				if (!pathB.get(i).equals(rtfPath.get(i + sizeRtf - sizeB)))
					return false;
		}
		return true;
	}

	/**
	 * @return 
	 *         Map<TransitRouteId,List<Tuple<TransitLinkId,Tuple<fromCoord,toCoord
	 *         >>>>
	 */
	private Map<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> createCoordPairs(
			TransitSchedule schedule) {
		Map<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> coordPairs = new HashMap<Id, List<Tuple<Link, Tuple<Coord, Coord>>>>();
		for (TransitLine ptLine : schedule.getTransitLines().values()) {
			for (TransitRoute ptRoute : ptLine.getRoutes().values()) {
				if (ptRoute.getTransportMode().equals(TransportMode.bus)) {
					Id ptRouteId = ptRoute.getId();
					List<Tuple<Link, Tuple<Coord, Coord>>> ptLinkCoordPairs = coordPairs
							.get(ptRouteId);
					if (ptLinkCoordPairs == null)
						ptLinkCoordPairs = new ArrayList<Tuple<Link, Tuple<Coord, Coord>>>();

					NetworkRouteWRefs route = ptRoute.getRoute();

					hasStartLink = false;
					// route. startLink
					Link startLink = route.getStartLink();
					createCoordPair(startLink, ptLinkCoordPairs);

					// route. links
					for (Link link : route.getLinks())
						createCoordPair(link, ptLinkCoordPairs);

					// route. endLink
					Link endLink = route.getEndLink();
					createCoordPair(endLink, ptLinkCoordPairs);
					endLinks.add(endLink);

					coordPairs.put(ptRouteId, ptLinkCoordPairs);
				}
			}
		}
		return coordPairs;
	}

	private void createCoordPair(Link link,
			List<Tuple<Link, Tuple<Coord, Coord>>> ptLinkCoordPairs) {
		Coord fromCoord = link.getFromNode().getCoord(), toCoord = link
				.getToNode().getCoord();
		ptLinkCoordPairs.add(new Tuple<Link, Tuple<Coord, Coord>>(link,
				new Tuple<Coord, Coord>(fromCoord, toCoord)));
		if (!hasStartLink) {
			startLinks.add(link);
			hasStartLink = true;
		}
	}

	private List<Id>/* path */allocateRouteLink(Tuple<Coord, Coord> coordPair) {
		Coord coordA = coordPair.getFirst(), coordB = coordPair.getSecond();
		Node nodeA = carNet.getNearestNode(coordA), nodeB = carNet
				.getNearestNode(coordB);
		boolean AoutOfRange = CoordUtils.calcDistance(coordA, nodeA.getCoord()) > 100, BoutOfRange = CoordUtils
				.calcDistance(coordB, nodeB.getCoord()) > 100;

		List<Id> pathLinks = new ArrayList<Id>();

		if (!nodeA.equals(nodeB)) {
			Path path = dijkstra.calcLeastCostPath(nodeA, nodeB, 0);
			if (path != null)
				pathLinks.addAll(links2Ids(path.links));
			else {
				tmpPtLink.setFromNode(nodeA);
				tmpPtLink.setToNode(nodeB);
				links2add.add(tmpPtLink);
				pathLinks.add(tmpPtLink.getId());
			}
		}
		// else/* nodeA==nodeB */{}

		if (AoutOfRange) {
			Id newLinkId = new IdImpl(this.tmpPtLink.getId() + "-2-"
					+ nodeA.getId());
			Link newLink = multiModalNetwork.getLinks().get(newLinkId);
			if (newLink == null) {
				Node A = tmpPtLink.getFromNode();
				newLink = multiModalNetwork.getFactory().createLink(newLinkId, A.getId(), nodeA.getId());
				newLink.setLength(CoordUtils.calcDistance(coordA, nodeA.getCoord()));
				newLink.setFreespeed(tmpPtLink.getFreespeed(0));
				newLink.setCapacity(tmpPtLink.getCapacity(0));
				newLink.setNumberOfLanes(tmpPtLink.getNumberOfLanes(0));
			}
			links2add.add(newLink);
			pathLinks.add(0, newLinkId);// first position
		}

		if (BoutOfRange) {
			Id newLinkId = new IdImpl(this.tmpPtLink.getId() + "-from-"
					+ nodeB.getId());
			Link newLink = multiModalNetwork.getLinks().get(newLinkId);
			if (newLink == null) {
				Node B = tmpPtLink.getToNode();
				newLink = multiModalNetwork.getFactory().createLink(newLinkId, nodeB.getId(), B.getId());
				newLink.setLength(CoordUtils.calcDistance(coordB, nodeB.getCoord()));
				newLink.setFreespeed(tmpPtLink.getFreespeed(0));
				newLink.setCapacity(tmpPtLink.getCapacity(0));
				newLink.setNumberOfLanes(tmpPtLink.getNumberOfLanes(0));
			}
			links2add.add(newLink);
			pathLinks.add(newLinkId);// last position
		}

		return pathLinks;
	}

	private void startCase(List<Id> linkIds, Node to) {
		tmpPtLink.setToNode(to);
		links2add.add(tmpPtLink);
		linkIds.add(tmpPtLink.getId());
	}

	private void endCase(List<Id> linkList, Node from) {
		tmpPtLink.setFromNode(from);
		links2add.add(tmpPtLink);
		linkList.add(tmpPtLink.getId());
	}

	private void output() {
		SimpleWriter writer = new SimpleWriter(outputFile);
		writer.writeln("ptRouteId\t:\tptlinkId\t:\tlinks");
		for (Entry<Id, List<Tuple<Id, List<Id>/* path */>>> routeLinkPathEntry : paths
				.entrySet()) {
			for (Tuple<Id, List<Id>/* path */> linkPathPair : routeLinkPathEntry
					.getValue()) {
				StringBuffer line = new StringBuffer(routeLinkPathEntry
						.getKey()
						+ "\t:\t" + linkPathPair.getFirst() + "\t:\t");
				for (Id linkId : linkPathPair.getSecond())
					line.append(linkId + "\t");
				writer.writeln(line.toString());
			}
		}
		writer.writeln("----->>>>>HALFWAY RECTIFICATION RESULTS<<<<<-----");

		// for (Entry<Id, List<Tuple<String, List<Id>/*path*/>>>
		// routeLinkPathEntry : paths4rtf
		// .entrySet()) {
		// for (Tuple<String, List<Id>/*path*/> linkPathPair :
		// routeLinkPathEntry
		// .getValue()) {
		// StringBuffer line = new StringBuffer(routeLinkPathEntry
		// .getKey()
		// + "\t:\t" + linkPathPair.getFirst() + "\t:\t");
		// for (Id linkId : linkPathPair.getSecond())
		// line.append(linkId + "\t");
		// writer.writeln(line.toString());
		// }
		// }
		System.out
				.println(">>>>> >>>>> output has written HALFWAY RECTIFICATION RESULTS!!!");
		writer.writeln("----->>>>>RECTIFICATION RESULTS<<<<<-----");

		// eliminateRedundancy();/* very important */
		System.out.println(">>>>> >>>>> eliminateRedundancy ENDS!!!");

		writer.writeln("ptRouteId\t:\tptlinkId\t:\tlinks");
		for (Entry<Id, List<Tuple<Id, List<Id>/* path */>>> routeLinkPathEntry : paths
				.entrySet()) {
			for (Tuple<Id, List<Id>/* path */> linkPathPair : routeLinkPathEntry
					.getValue()) {
				StringBuffer line = new StringBuffer(routeLinkPathEntry
						.getKey()
						+ "\t:\t" + linkPathPair.getFirst() + "\t:\t");
				for (Id linkId : linkPathPair.getSecond())
					line.append(linkId + "\t");
				writer.writeln(line.toString());
			}
		}
		System.out
				.println(">>>>> >>>>> output has written THE END RECTIFICATION RESULTS!!!");
		writer
				.writeln("----->>>>>PLEASE DON'T FORGET THE STARTLINE OF A BUSLINE<<<<<-----");
		writer.close();
	}

	public void run() {
		allocateAllRouteLinks();
		System.out.println(">>>>> allocateAllRouteLinks ends!!!");
		rectifyAllocations();
		System.out.println(">>>>> rectifyAllocations ends!!!");
		System.out.println(">>>>> output begins!!!");
		output();
	}

	private void generateNewSchedule(String newTransitScheduleFilename) {
		Map<Id, List<Id>/* path */> ptLinkIdPaths = convertResult();
		/*-------------------STOPS---------------------------*/
		for (TransitStopFacility stop : schedule.getFacilities().values()) {
			Id stopLinkId = stop.getLinkId();
			List<Id>/* path */path = ptLinkIdPaths.get(stopLinkId);
			if (path != null) {
				if (path.size() > 0)
					/*------with carNet-----*/
					stop.setLink(carNet.getLinks().get(path.get(path.size() - 1)));
				else
					stop.setLink(null);
			} else {
				stop.setLink(null);
			}
		}
		/*---------------------TRANSITROUTES--------------------------*/
		Map<Id, NetworkRouteWRefs> idRoutes = convertResult2();
		for (TransitLine line : schedule.getTransitLines().values())
			for (Entry<Id, TransitRoute> idRoutePair : line.getRoutes()
					.entrySet()) {
				Id id = idRoutePair.getKey();
				if (idRoutes.containsKey(id))
					idRoutePair.getValue().setRoute(idRoutes.get(id));
			}

		try {
			new TransitScheduleWriter(schedule)
					.writeFile(newTransitScheduleFilename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Map<Id, List<Id>/* path */> convertResult() {
		Map<Id, List<Id>/* path */> ptLinkIdPathMap = new HashMap<Id, List<Id>/*
																			 * Path.
																			 * links
																			 */>();
		for (List<Tuple<Id, List<Id>/* path */>> ptLinkIdPathPairs : paths
				.values())
			for (Tuple<Id, List<Id>/* path */> idPathPair : ptLinkIdPathPairs) {
				Id id = idPathPair.getFirst();
				List<Id> path = ptLinkIdPathMap.get(id);
				if (path == null)
					path = idPathPair.getSecond();
				else if (path.size() < idPathPair.getSecond().size())
					path = idPathPair.getSecond();

				for (Id linkId : path) {
					carNet.getLinks().get(linkId).setAllowedModes(modes);
				}

				ptLinkIdPathMap.put(id, path /* path */);
			}

		return ptLinkIdPathMap;
	}

	/**
	 * @return Map<ptRouteId,Route of a TransitRoute>
	 */
	private Map<Id, NetworkRouteWRefs> convertResult2() {
		Map<Id, NetworkRouteWRefs> routes = new HashMap<Id, NetworkRouteWRefs>();
		for (Entry<Id, List<Tuple<Id, List<Id>/* path */>>> routeId_linkPathPair : paths
				.entrySet()) {
			List<Tuple<Id, List<Id>/* path */>> linkId_Paths = routeId_linkPathPair
					.getValue();
			Link startLink = null, endLink = null;
			LinkedList<Id>/* path */routeLinks = new LinkedList<Id>/*
																 * Path. links
																 */();
			int size = linkId_Paths.size();
			for (int i = 0; i < size; i++) {
				Tuple<Id, List<Id>/* path */> linkId_PathPair = linkId_Paths
						.get(i);
				List<Id>/* path */linkIds = linkId_PathPair.getSecond();
				routeLinks.addAll(linkIds);

			}
			/*-----with carNetwork-----*/
			endLink = carNet.getLinks().get(routeLinks.removeLast());
			startLink = carNet.getLinks().get(routeLinks.remove(0));
			NetworkRouteWRefs route = new LinkNetworkRouteImpl(startLink,
					endLink);
			route.setLinks(startLink, ids2links(routeLinks), endLink);
			routes.put(routeId_linkPathPair.getKey()/* routeId */, route);
		}
		return routes;
	}

	private List<Link> ids2links(List<Id> ids) {
		List<Link> links = new ArrayList<Link>();
		for (Id id : ids) {
			/*-----with carNetwork-----*/
			links.add(carNet.getLinks().get(id));
		}
		return links;
	}

	private void generateNewPlans(PopulationImpl pop, String newPopFile) {
		Map<Id, List<Id>/* path */> linkIdPahs = convertResult();

		for (Person person : pop.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<PlanElement> pes = plan.getPlanElements();

				for (int i = 0; i < pes.size(); i += 2) {
					ActivityImpl act = (ActivityImpl) pes.get(i);
					Id linkId = act.getLinkId();
					if (act.getType().equals("pt interaction")
							&& linkId.toString().startsWith("tr_")) {
						Link actLink = carNet.getLinks().get(linkId);
						if (actLink == null)
							act.setLink(carNet.getNearestLink(multiModalNetwork
									.getLinks().get(linkId).getCoord()));
					}
				}
			}
		}
		new PopulationWriter(pop).writeFile(newPopFile);
	}

	private void generateNewNetwork(String newNetFilename) {
		/*-----------carNetwork with only Bus----------*/
		new NetworkWriter(carNet).writeFile(newNetFilename);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// String multiModalNetworkFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.multimodal.mini.xml";
		// String transitScheduleFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/transitSchedule.networkOevModellBln.xml";
		// String carNetFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.car.mini.xml";
		// String outputFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/busLineAllocation2.txt";
		// String popFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/plan.routedOevModell.BVB344.moreLegPlan_Agent.xml";
		//
		// String newNetworkFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newNetTest2.xml";
		// String newTransitScheduleFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newScheduleTest2.xml";
		// String newPopFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newPopTest2.xml";

		String multiModalNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/network.multimodal.xml.gz";
		String transitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/transitSchedule.networkOevModellBln.xml.gz";
		String carNetFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newMultiModalNetBiggerWithoutBusLinkCleanedTest.xml.gz";
		String outputFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/busLineAllocationBigger.txt";
		String popFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/plan.routedOevModell.xml.gz";

		String newNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newNetWithBusBiggerTest.xml.gz";
		String newTransitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newScheduleBigger.xml.gz";
		String newPopFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newPopBiggerTest.xml.gz";

		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);

		NetworkImpl multiModalNetwork = scenario.getNetwork();
		new MatsimNetworkReader(multiModalNetwork)
				.readFile(multiModalNetworkFile);

		TransitSchedule schedule = scenario.getTransitSchedule();
		try {
			new TransitScheduleReader(scenario).readFile(transitScheduleFile);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		NetworkLayer carNet = new NetworkLayer();
		new MatsimNetworkReader(carNet).readFile(carNetFile);

		BusLineAllocator busLineAllocator = new BusLineAllocator(carNet,
				multiModalNetwork, schedule, outputFile);
		busLineAllocator.run();
		busLineAllocator.generateNewNetwork(newNetworkFile);
		busLineAllocator.generateNewSchedule(newTransitScheduleFile);

		PopulationImpl pop = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(popFile);
		busLineAllocator.generateNewPlans(pop, newPopFile);

		System.out.println(">>>>done!!!!!");
	}
}
