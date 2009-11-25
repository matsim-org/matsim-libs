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

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
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

	private NetworkLayer netWithoutBus;
	private Map<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> coordPairs;// <ptRouteId,<ptLink<fromNodeCoord,toNodeCoord>>>
	// private Map<Id, List<Tuple<String, Tuple<Coord, Coord>>>>
	// coordPairs4rtf;//
	// <ptRouteId,<ptLinkId:next_ptLinkId<fromNodeCoord,toNodeCoord>>>
	/*
	 * <ptRouteId,List<ptLinkId,Path_Links(shouldn't be pt linkId, but there
	 * also is exception))>>
	 */
	private Map<Id, List<Tuple<Id, List<Id>/* Path.links */>>> paths = new HashMap<Id, List<Tuple<Id, List<Id>/*
																												 * Path.
																												 * links
																												 */>>>();
	/*
	 * <ptRouteId,List<ptLinkId:next_ptLinkId,Path(linkIds (shouldn't be pt
	 * linkIds))>>
	 */
	private Map<Id, List<Tuple<String, List<Id>/* Path.links */>>> paths4rtf = new HashMap<Id, List<Tuple<String, List<Id>/*
																															 * Path.
																															 * links
																															 */>>>();
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
	private NetworkLayer multiModalNetwork;
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
	public BusLineAllocator(NetworkLayer netWithoutBus,
			NetworkLayer multiModalNetwork, TransitSchedule schedule,
			String outputFile) {
		this.netWithoutBus = netWithoutBus;
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
			List<Tuple<Id, List<Id>/* Path.links */>> ptLinkIdPaths = paths
					.get(tmpPtRouteId);
			if (ptLinkIdPaths == null)
				ptLinkIdPaths = new ArrayList<Tuple<Id, List<Id>/* Path. links */>>();

			for (Tuple<Link, Tuple<Coord, Coord>> linkCoordPair : routeLinkCoordPair
					.getValue()) {
				tmpPtLink = linkCoordPair.getFirst();
				List<Id>/* Path.links */pathLinks = allocateRouteLink(linkCoordPair
						.getSecond()/* coordPair */);
				// TODO empty check
				if (!pathLinks.isEmpty())/*
										 * think? How to handle the median link,
										 * which has the same fromNode(carNet)
										 * and toNode(carNet)? It's not problem
										 * more.
										 */{
					ptLinkIdPaths.add(new Tuple<Id, List<Id>/* Path.links */>(
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
			Node carFrom = netWithoutBus.getNode(ptFromId);
			if (carFrom == null)/* this node with this Id dosn't exist. */
				carFrom = netWithoutBus.createAndAddNode(ptFromId, ptFrom
						.getCoord());
			carFrom.getOutLinks().remove(l2a.getId());

			Node ptTo = l2a.getToNode();
			Id ptToId = ptTo.getId();
			Node carTo = netWithoutBus.getNode(ptToId);
			if (carTo == null)
				carTo = netWithoutBus.createAndAddNode(ptToId, ptTo.getCoord());
			carTo.getInLinks().remove(l2a.getId());

			Link createdLink = netWithoutBus.createAndAddLink(l2a.getId(),
					carFrom, carTo, l2a.getLength(), l2a.getFreespeed(0), l2a
							.getCapacity(0), l2a.getNumberOfLanes(0));
			createdLink.setAllowedModes(modes);
		}
		netWithoutBus.connect();
		/*---------- check in-/outLinks of nodes of links added to netWithoutBus-------*/
		for (Link l2a : links2add) {
			Link link = netWithoutBus.getLink(l2a.getId());// get link from-
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
					Link inLink = netWithoutBus.getLink(inLinkId);
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
					Link outLink = netWithoutBus.getLink(outLinkId);
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
					Link inLink = netWithoutBus.getLink(inLinkId);
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
					Link outLink = netWithoutBus.getLink(outLinkId);
					if (outLink != null)
						to.addOutLink(outLink);
				}
			}
		}
		netWithoutBus.connect();
		dijkstra/* new Instance angain */= new Dijkstra(netWithoutBus,
				new TravelCostFunctionDistance(), new TravelTimeFunctionFree());

	}

	private void rectifyAllocations() {
		for (Entry<Id, List<Tuple<Id, List<Id>/* Path.links */>>> ptRouteIdPtLinkIdpaths : paths
				.entrySet()) {

			Id ptRouteId = ptRouteIdPtLinkIdpaths.getKey();
			List<Tuple<String/* ptLinkIdpair */, List<Id>/* Path.links */>> ptLinkIdPaths4rtf = paths4rtf
					.get(ptRouteId);
			if (ptLinkIdPaths4rtf == null)
				ptLinkIdPaths4rtf = new ArrayList<Tuple<String, List<Id>/*
																		 * Path.links
																		 */>>();

			List<Tuple<Id, List<Id>/* Path.links */>> ptLinkIdPaths = ptRouteIdPtLinkIdpaths
					.getValue();
			for (int i = 0; i < ptLinkIdPaths.size() - 1; i++) {
				System.out.println(">>>>>Line 296 BEGINS!!!");
				Tuple<Id, List<Id>/* Path.links */> ptLinkIdPathA = ptLinkIdPaths
						.get(i), ptLinkIdPathB = ptLinkIdPaths.get(i + 1);
				List<Id> pathA = ptLinkIdPathA.getSecond(), pathB = ptLinkIdPathB
						.getSecond();

				Node from = netWithoutBus.getLink(pathA.get(0)).getFromNode(), to = netWithoutBus
						.getLink(pathB.get(pathB.size() - 1)).getToNode();

				Id idA = ptLinkIdPathA.getFirst(), idB = ptLinkIdPathB
						.getFirst();
				List<Id> linkIds = new ArrayList<Id>();
				Link lastALink = netWithoutBus.getLink(pathA
						.get(pathA.size() - 1)), firstBLink = netWithoutBus
						.getLink(pathB.get(0));
				System.out.println(">>>>> before calcShortPath\t" + lastALink
						+ "\t" + firstBLink);
				Path path = dijkstra.calcLeastCostPath(from, to, 0);
				System.out.println(">>>>> after calcShortPath");
				if (path == null) {

					System.err.println("No connections between the 2 links:\t"
							+ lastALink + "\t" + firstBLink);

					Node lastANode = lastALink.getToNode(), firstBNode = firstBLink
							.getFromNode();

					if (lastANode.getId().toString().startsWith("tr_")) {
						lastALink.setToNode(firstBNode);
					} else if (!lastANode.getId().toString().startsWith("tr_")
							&& firstBNode.getId().toString().startsWith("tr_")) {
						firstBLink.setFromNode(lastANode);
					} else {
						Id newLinkId = new IdImpl(lastANode.getId() + "-2-"
								+ firstBNode.getId());
						Link newLink = netWithoutBus.getLink(newLinkId);
						if (newLink == null)
							newLink = netWithoutBus.createAndAddLink(newLinkId,
									lastANode, firstBNode, CoordUtils
											.calcDistance(lastANode.getCoord(),
													firstBNode.getCoord()),
									lastALink.getFreespeed(0), lastALink
											.getCapacity(0), lastALink
											.getNumberOfLanes(0));

						linkIds.addAll(pathA);
						linkIds.add(newLink.getId());
						linkIds.addAll(pathB);
						System.out.println(">>>>>Line 331 finished!!!");
					}

				} else {

					linkIds = links2Ids(path.links);
				}
				ptLinkIdPaths4rtf
						.add(new Tuple<String, List<Id>/* Path.links */>(idA
								+ ":" + idB /* idAB */, linkIds/* pathAB */));
				System.out.println(">>>>>Line 339 finished!!!");
			}
			paths4rtf.put(ptRouteId, ptLinkIdPaths4rtf);
			System.out.println(">>>>>Line 341 finished!!!");
		}

	}

	private static List<Id> links2Ids(List<Link> links) {
		List<Id> ids = new ArrayList<Id>();
		for (Link link : links) {
			ids.add(link.getId());
		}
		return ids;
	}

	private void eliminateRedundancy() {
		for (Entry<Id, List<Tuple<String, List<Id>/* Path.links */>>> path4rtfEntry : paths4rtf
				.entrySet()) {
			Id routeId = path4rtfEntry.getKey();
			List<Tuple<String, List<Id>/* Path.links */>> ptLinkIdsPaths4rtf = path4rtfEntry
					.getValue();
			for (int j = 0; j < ptLinkIdsPaths4rtf.size(); j++) {

				Tuple<String, List<Id>/* Path.links */> ptLinkIdsPath4rtf = ptLinkIdsPaths4rtf
						.get(j);
				String[] ptLinkIds4rtf = ptLinkIdsPath4rtf.getFirst()
						.split(":");
				List<Id>/* Path.links */pathA = null, pathB = null;
				List<Tuple<Id, List<Id>>> ptLinkIdPaths = paths.get(routeId);

				Tuple<Id, List<Id>/* Path.links */> ptLinkIdPathA = ptLinkIdPaths
						.get(j), ptLinkIdPathB = ptLinkIdPaths.get(j + 1);
				String ptLinkIdStrA = ptLinkIdPathA.getFirst().toString(), ptLinkIdStrB = ptLinkIdPathB
						.getFirst().toString();
				if (ptLinkIds4rtf[0].equals(ptLinkIdStrA))
					pathA = ptLinkIdPathA.getSecond();
				if (ptLinkIds4rtf[1].equals(ptLinkIdStrB))
					pathB = ptLinkIdPathB.getSecond();
				if (pathA == null && pathB == null) {
					System.err
							.println(">>>>>lineNo.380\tlinkIds don't match :\t"
									+ ptLinkIds4rtf + "\t<->\t" + ptLinkIdStrA
									+ "\t" + ptLinkIdStrB);
					System.exit(1);
				}

				List<Id>/* Path.links */rtfPath = ptLinkIdsPath4rtf.getSecond();
				List<Id>/* Path.links */tmpLinksA = new ArrayList<Id>/*
																	 * Path.links
																	 */();
				List<Id>/* Path.links */tmpLinksB = new ArrayList<Id>/*
																	 * Path.links
																	 */();
				List<Id>/* Path.links */tmpLinksRtf = new ArrayList<Id>/*
																		 * Path.links
																		 */();
				tmpLinksA.addAll(pathA);
				for (Id linkId : tmpLinksA)
					if (!rtfPath.contains(linkId)) {
						pathA.remove(linkId);
					}
				tmpLinksB.addAll(pathB);
				for (Id linkId : tmpLinksB)
					if (!rtfPath.contains(linkId)) {
						pathB.remove(linkId);
					}

				if (!isSubList(pathA, pathB, rtfPath)) {
					pathA.clear();
					pathB.clear();
					pathB.addAll(tmpLinksB);
					pathA.addAll(tmpLinksA);
				}
				/* already reduced ? */
				Id lastA = pathA.get(pathA.size() - 1), firstB = pathB.get(0);
				Node lastANode = netWithoutBus.getLink(lastA).getToNode(), firstBNode = netWithoutBus
						.getLink(firstB).getFromNode();
				if (!lastANode.equals(firstBNode))/* different nodes */{
					if (!lastANode.getCoord().equals(firstBNode.getCoord()))/*
																			 * differen
																			 * coord
																			 */{
						Path path = dijkstra.calcLeastCostPath(lastANode,
								firstBNode, 0);

						if (path != null) {
							if (!path.links.isEmpty()) {
								Coord ptLinkAToCoord = multiModalNetwork
										.getLink(ptLinkIdStrA).getToNode()
										.getCoord();
								double minDist = 10000;
								Link nearestLink = null;
								int nearestPos = -1;
								for (int i = 0; i < path.links.size(); i++) {
									Link link = path.links.get(i);
									double tmpDist = CoordUtils.calcDistance(
											ptLinkAToCoord, link.getCoord());
									if (tmpDist < minDist) {
										minDist = tmpDist;
										nearestLink = link;
										nearestPos = i;
									}
								}
								if (nearestLink != null)
									for (int i = 0; i < path.links.size(); i++) {
										Link link = path.links.get(i);
										if (i <= nearestPos)
											pathA.add(link.getId());
										else
											pathB.add(i - nearestPos - 1, link
													.getId());
									}
							} else {
								System.out
										.println(">>>>>line424\tempty path.links");
							}
						} else/* path==null */{
							Link ptLinkA = multiModalNetwork
									.getLink(ptLinkIdStrA);
							Link newLink = netWithoutBus.createAndAddLink(
									new IdImpl(ptLinkIdStrA + "-"
											+ ptLinkIdStrB), lastANode,
									firstBNode, CoordUtils.calcDistance(
											lastANode.getCoord(), firstBNode
													.getCoord()), ptLinkA
											.getFreespeed(0), ptLinkA
											.getCapacity(0), ptLinkA
											.getNumberOfLanes(0));
							pathA.add(newLink.getId());
						}
					} else/* same Coord */{
						Link ptLinkA = multiModalNetwork.getLink(ptLinkIdStrA);
						Id newLinkId = new IdImpl(ptLinkIdStrA + "-"
								+ ptLinkIdStrB);

						Link newLink = netWithoutBus.getLink(newLinkId);
						if (newLink == null)
							newLink = netWithoutBus.createAndAddLink(newLinkId,
									lastANode, firstBNode, 0, ptLinkA
											.getFreespeed(0), ptLinkA
											.getCapacity(0), ptLinkA
											.getNumberOfLanes(0));
						newLink.setAllowedModes(modes);
						pathA.add(newLink.getId());
					}
				}
			}
		}
		netWithoutBus.connect();
	}

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
					if (createCoordPair(endLink, ptLinkCoordPairs))
						endLinks.add(endLink);
					else
						endLinks.add(ptLinkCoordPairs.get(
								ptLinkCoordPairs.size() - 1).getFirst());
					hasStartLink = false;

					coordPairs.put(ptRouteId, ptLinkCoordPairs);
				}
			}
		}
		return coordPairs;
	}

	private boolean createCoordPair(Link link,
			List<Tuple<Link, Tuple<Coord, Coord>>> ptLinkCoordPairs) {
		Coord fromCoord = link.getFromNode().getCoord(), toCoord = link
				.getToNode().getCoord();
		boolean toReturn = true;
		// link.getLength() > 0 && !fromCoord.equals(toCoord);
		if (toReturn) {
			ptLinkCoordPairs.add(new Tuple<Link, Tuple<Coord, Coord>>(link,
					new Tuple<Coord, Coord>(fromCoord, toCoord)));
			if (!hasStartLink) {
				startLinks.add(link);
				hasStartLink = true;
			}
		}
		return toReturn;
	}

	private List<Id>/* Path.links */allocateRouteLink(
			Tuple<Coord, Coord> coordPair) {
		Coord coordA = coordPair.getFirst(), coordB = coordPair.getSecond();
		Node nodeA = netWithoutBus.getNearestNode(coordA), nodeB = netWithoutBus
				.getNearestNode(coordB);
		boolean AInRange = CoordUtils.calcDistance(coordA, nodeA.getCoord()) < 100/*
																				 * TEST(
																				 * old
																				 * value
																				 * =
																				 * 50
																				 * )
																				 */, BInRange = CoordUtils
				.calcDistance(coordB, nodeB.getCoord()) < 100;/*
															 * TEST(old
															 * value=50)
															 */// circle with
		// radius of 50
		// [m]
		List<Id>/* Path.links */linkIds = new ArrayList<Id>/* Path.links */();

		if (AInRange && BInRange/* both inside */) {
			if (!nodeA.equals(nodeB)) {
				Path path = dijkstra.calcLeastCostPath(nodeA, nodeB, 0);
				if (path != null) {
					if (!path.links.isEmpty()) {
						List<Link> links = path.links;
						linkIds.addAll(links2Ids(links));
					} else {
						System.out.println("empty path.links!");
					}
				} else {
					tmpPtLink.setFromNode(nodeA);

					tmpPtLink.setToNode(nodeB);

					links2add.add(tmpPtLink);
					linkIds.add(tmpPtLink.getId());
				}
			} else/* nodeA==nodeB */{
				if (startLinks.contains(tmpPtLink)) /*
													 * this tmpPtLink is a
													 * startLink or endLink
													 */
					startCase(linkIds, nodeB);
				else if (endLinks.contains(tmpPtLink))
					endCase(linkIds, nodeA);
			}
		} else if (!AInRange && !BInRange/* both outside */) {
			links2add.add(tmpPtLink);
			linkIds.add(tmpPtLink.getId());
		} else/* one outside, one inside */{
			if (AInRange) {
				// endCase(linkIds, nodeA);
				Path path = dijkstra.calcLeastCostPath(nodeA, nodeB, 0);
				if (path != null) {
					if (!path.links.isEmpty()) {
						List<Link> links = path.links;
						linkIds.addAll(links2Ids(links));
					} else {
						tmpPtLink.setFromNode(nodeA);
						links2add.add(tmpPtLink);
						linkIds.add(tmpPtLink.getId());
					}

				} else {
					tmpPtLink.setFromNode(nodeA);
					links2add.add(tmpPtLink);
					linkIds.add(tmpPtLink.getId());
				}

				if (!nodeA.equals(nodeB)) {
					Id newLinkId = new IdImpl(tmpPtLink.getId() + ">"
							+ nodeB.getId());
					Link newLink = multiModalNetwork.getLink(newLinkId);
					if (newLink == null) {
						Id newNodeId = new IdImpl(tmpPtLink.getId() + "-4-"
								+ nodeB.getId());
						Node newNode = multiModalNetwork.getNode(newNodeId);
						if (newNode == null)
							newNode = multiModalNetwork.createAndAddNode(
									newNodeId, coordB);
						newLink = multiModalNetwork.createAndAddLink(newLinkId,
								nodeB, newNode, CoordUtils.calcDistance(nodeB
										.getCoord(), coordB), tmpPtLink
										.getFreespeed(0), tmpPtLink
										.getCapacity(0), tmpPtLink
										.getNumberOfLanes(0));
					}
					links2add.add(newLink);
					linkIds.add(newLinkId);
					System.out.println("path of ptLink:\t" + tmpPtLink.getId()
							+ "==>\t" + linkIds);
				}
			} else {
				/* BInRange */
				// startCase(linkIds, nodeB);
				if (!nodeA.equals(nodeB)) {
					Id newLinkId = new IdImpl(nodeA.getId() + ">"
							+ tmpPtLink.getId());
					Link newLink = multiModalNetwork.getLink(newLinkId);
					if (newLink == null) {
						Id newNodeId = new IdImpl(tmpPtLink.getId() + "-4-"
								+ nodeA.getId());
						Node newNode = multiModalNetwork.getNode(newNodeId);
						if (newNode == null)
							newNode = multiModalNetwork.createAndAddNode(
									newNodeId, coordA);

						newLink = multiModalNetwork.createAndAddLink(newLinkId,
								newNode, nodeA, CoordUtils.calcDistance(nodeA
										.getCoord(), coordA), tmpPtLink
										.getFreespeed(0), tmpPtLink
										.getCapacity(0), tmpPtLink
										.getNumberOfLanes(0));
					}

					links2add.add(newLink);
					linkIds.add(newLinkId);
				}

				Path path = dijkstra.calcLeastCostPath(nodeA, nodeB, 0);
				if (path != null) {
					if (!path.links.isEmpty()) {
						List<Link> links = path.links;
						linkIds.addAll(links2Ids(links));
						return linkIds;
					}
				}
				{
					tmpPtLink.setToNode(nodeB);
					links2add.add(tmpPtLink);
					linkIds.add(tmpPtLink.getId());
				}
				System.out.println("path of ptLink:\t" + tmpPtLink.getId()
						+ "==>\t" + linkIds);
			}
		}
		return linkIds;
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
		for (Entry<Id, List<Tuple<Id, List<Id>/* Path.links */>>> routeLinkPathEntry : paths
				.entrySet()) {
			for (Tuple<Id, List<Id>/* Path.links */> linkPathPair : routeLinkPathEntry
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

		for (Entry<Id, List<Tuple<String, List<Id>/* Path.links */>>> routeLinkPathEntry : paths4rtf
				.entrySet()) {
			for (Tuple<String, List<Id>/* Path.links */> linkPathPair : routeLinkPathEntry
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
				.println(">>>>> >>>>> output has written HALFWAY RECTIFICATION RESULTS!!!");
		writer.writeln("----->>>>>RECTIFICATION RESULTS<<<<<-----");

		eliminateRedundancy();/* very important */
		System.out.println(">>>>> >>>>> eliminateRedundancy ENDS!!!");

		writer.writeln("ptRouteId\t:\tptlinkId\t:\tlinks");
		for (Entry<Id, List<Tuple<Id, List<Id>/* Path.links */>>> routeLinkPathEntry : paths
				.entrySet()) {
			for (Tuple<Id, List<Id>/* Path.links */> linkPathPair : routeLinkPathEntry
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
		Map<Id, List<Id>/* Path.links */> ptLinkIdPaths = convertResult();
		/*-------------------STOPS---------------------------*/
		for (TransitStopFacility stop : schedule.getFacilities().values()) {
			Id stopLinkId = stop.getLinkId();
			List<Id>/* Path.links */path = ptLinkIdPaths.get(stopLinkId);
			if (path != null) {
				if (path.size() > 0)
					/*------with carNet-----*/
					stop.setLink(netWithoutBus.getLink(path
							.get(path.size() - 1)));
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

	private Map<Id, List<Id>/* Path.links */> convertResult() {
		Map<Id, List<Id>/* Path.links */> ptLinkIdPathMap = new HashMap<Id, List<Id>/*
																					 * Path.
																					 * links
																					 */>();
		for (List<Tuple<Id, List<Id>/* Path.links */>> ptLinkIdPathPairs : paths
				.values())
			for (Tuple<Id, List<Id>/* Path.links */> idPathPair : ptLinkIdPathPairs) {
				Id id = idPathPair.getFirst();
				List<Id> path = ptLinkIdPathMap.get(id);
				if (path == null)
					path = idPathPair.getSecond();
				else if (path.size() < idPathPair.getSecond().size())
					path = idPathPair.getSecond();

				for (Id linkId : path) {
					netWithoutBus.getLink(linkId).setAllowedModes(modes);
				}

				ptLinkIdPathMap.put(id, path /* path.links */);
			}

		return ptLinkIdPathMap;
	}

	/**
	 * @return Map<ptRouteId,Route of a TransitRoute>
	 */
	private Map<Id, NetworkRouteWRefs> convertResult2() {
		Map<Id, NetworkRouteWRefs> routes = new HashMap<Id, NetworkRouteWRefs>();
		for (Entry<Id, List<Tuple<Id, List<Id>/* Path.links */>>> routeId_linkPathPair : paths
				.entrySet()) {
			List<Tuple<Id, List<Id>/* Path.links */>> linkId_Paths = routeId_linkPathPair
					.getValue();
			Link startLink = null, endLink = null;
			LinkedList<Id>/* Path.links */routeLinks = new LinkedList<Id>/*
																		 * Path.
																		 * links
																		 */();
			int size = linkId_Paths.size();
			for (int i = 0; i < size; i++) {
				Tuple<Id, List<Id>/* Path.links */> linkId_PathPair = linkId_Paths
						.get(i);
				List<Id>/* Path.links */linkIds = linkId_PathPair.getSecond();
				routeLinks.addAll(linkIds);

			}
			/*-----with carNetwork-----*/
			endLink = netWithoutBus.getLink(routeLinks.removeLast());
			startLink = netWithoutBus.getLink(routeLinks.remove(0));
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
			links.add(netWithoutBus.getLink(id));
		}
		return links;
	}

	private void generateNewPlans(PopulationImpl pop, String newPopFile) {
		Map<Id, List<Id>/* Path.links */> linkIdPahs = convertResult();

		for (Person person : pop.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<PlanElement> pes = plan.getPlanElements();

				for (int i = 0; i < pes.size(); i += 2) {
					ActivityImpl act = (ActivityImpl) pes.get(i);
					Id linkId = act.getLinkId();
					if (act.getType().equals("pt interaction")
							&& linkId.toString().startsWith("tr_")) {
						Link actLink = netWithoutBus.getLink(linkId);
						if (actLink == null)
							act.setLink(netWithoutBus
									.getNearestLink(multiModalNetwork.getLink(
											linkId).getCoord()));
					}
				}
			}
		}
		new PopulationWriter(pop).writeFile(newPopFile);
	}

	private void generateNewNetwork(String newNetFilename) {
		/*-----------carNetwork with only Bus----------*/
		new NetworkWriter(netWithoutBus).writeFile(newNetFilename);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// String multiModalNetworkFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.multimodal.mini.xml";
		// String transitScheduleFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/transitSchedule.networkOevModellBln.xml";
		// String netWithoutBusFile =
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
		String netWithoutBusFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newMultiModalNetBiggerWithoutBusLinkCleanedTest.xml.gz";
		String outputFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/busLineAllocationBigger.txt";
		String popFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/plan.routedOevModell.xml.gz";

		String newNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newMultiModalNetBiggerTest.xml.gz";
		String newTransitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newScheduleBigger.xml.gz";
		String newPopFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newPopBiggerTest.xml.gz";

		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);

		NetworkLayer multiModalNetwork = scenario.getNetwork();
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

		NetworkLayer netWithoutBus = new NetworkLayer();
		new MatsimNetworkReader(netWithoutBus).readFile(netWithoutBusFile);

		BusLineAllocator busLineAllocator = new BusLineAllocator(netWithoutBus,
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
