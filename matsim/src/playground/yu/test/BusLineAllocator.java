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

	private NetworkLayer carNetwork;
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
	// private NetworkLayer multiModalNetwork;
	private boolean hasStartLink = false, isStartLink = false;;
	private static Set<TransportMode> modes = new HashSet<TransportMode>();

	/**
	 * @param carNetwork
	 *            a nomral MATsim <code>NetworkImpl</code>, in which there
	 *            aren't "pt" links
	 * @param stopCoords
	 *            a Collection of
	 *            <ptRouteId,<ptLinkId<fromNodeCoord,toNodeCoord>>>
	 * @param outputFile
	 *            file path of the output file
	 */
	public BusLineAllocator(NetworkLayer carNetwork,
			NetworkLayer multiModalNetwork, TransitSchedule schedule,
			String outputFile) {
		this.carNetwork = carNetwork;
		// this.multiModalNetwork = multiModalNetwork;
		this.schedule = schedule;
		this.coordPairs = createCoordPairs(schedule);
		// this.coordPairs4rtf = createCoordPairs4rtf(schedule);
		this.outputFile = outputFile;
		this.dijkstra = new Dijkstra(carNetwork,
				new TravelCostFunctionDistance(), new TravelTimeFunctionFree());
		modes.add(TransportMode.car);
		modes.add(TransportMode.pt);
	}

	protected void allocateAllRouteLinks() {
		for (Entry<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> routeLinkCoordPair : coordPairs
				.entrySet()) {
			tmpPtRouteId = routeLinkCoordPair.getKey();
			List<Tuple<Id, List<Id>/* Path.links */>> ptLinkIdPaths = paths
					.get(tmpPtRouteId);
			if (ptLinkIdPaths == null)
				ptLinkIdPaths = new ArrayList<Tuple<Id, List<Id>/* Path. links */>>();

			isStartLink = true;
			for (Tuple<Link, Tuple<Coord, Coord>> linkCoordPair : routeLinkCoordPair
					.getValue()) {
				tmpPtLink = linkCoordPair.getFirst();
				List<Id>/* Path.links */pathLinks = allocateRouteLink(linkCoordPair
						.getSecond()/* ptCoordPair */);

				if (pathLinks != null)/*
									 * TODO think? How to handle the median
									 * link, which has the same fromNode(carNet)
									 * and toNode(carNet)?
									 */{

					if (pathLinks.size() == 0) {
						System.err.println(">>>>>pathLinks!=null, but empty!!");
						System.out.println(">>>>>lineNo.180\t"
								+ tmpPtLink.getId() + "\t" + pathLinks);
						System.exit(1);
					}
					if (tmpPtLink.getId().toString().equals("tr_4306")) {
						System.out
								.println(">>>>>lineNo.187\ttr_4306\tpathLinks\t"
										+ pathLinks
										+ "\trouteId\t"
										+ tmpPtRouteId);
					}
					ptLinkIdPaths.add(new Tuple<Id, List<Id>/* Path.links */>(
							tmpPtLink.getId(), pathLinks));
				} else {
					System.err.println(">>>>>pathLinks==null");
					System.exit(1);
				}
				tmpPtLink = null;
				isStartLink = false;
			}

			paths.put(tmpPtRouteId, ptLinkIdPaths);
		}

		// add links
		for (Link link : links2add) {

			Node ptFrom = link.getFromNode();
			Id ptFromId = ptFrom.getId();
			Node carFrom = carNetwork.getNode(ptFromId);

			if (carFrom == null)/* this node with this Id dosn't exist. */
				carFrom = carNetwork.createAndAddNode(ptFromId, ptFrom
						.getCoord());
			else if (carFrom.getCoord().equals(ptFrom.getCoord()))/*
																 * exists and
																 * has the same
																 * Coord
																 */{
			} else {
				System.err
						.println(">>>>>lineNo.219\tERROR : node double name :\t"
								+ ptFrom.getId());
				System.exit(1);
			}

			Node ptTo = link.getToNode();
			Id ptToId = ptTo.getId();
			Node carTo = carNetwork.getNode(ptToId);
			if (carTo == null)
				carTo = carNetwork.createAndAddNode(ptToId, ptTo.getCoord());
			else if (carTo.getCoord().equals(ptTo.getCoord())) {
			} else {
				System.err
						.println(">>>>>LineNo.232\tERROR : node double name :\t"
								+ ptTo.getId());
				System.exit(1);
			}

			Link createdLink = carNetwork.createAndAddLink(link.getId(),
					carFrom, carTo, link.getLength(), link.getFreespeed(0),
					link.getCapacity(0), link.getNumberOfLanes(0));
			createdLink.setAllowedModes(modes);
		}
		carNetwork.connect();
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
				Tuple<Id, List<Id>/* Path.links */> ptLinkIdPathA = ptLinkIdPaths
						.get(i), ptLinkIdPathB = ptLinkIdPaths.get(i + 1);
				List<Id> pathA = ptLinkIdPathA.getSecond(), pathB = ptLinkIdPathB
						.getSecond();

				int positionA = pathA.size();
				Node fromNode = null, toNode = null;
				for (int j = 0; j < pathA.size(); j++) {
					Id id = pathA.get(j);
					if (!id.toString().startsWith("tr_")) {
						fromNode = carNetwork.getLink(id).getFromNode();
						positionA = j;
						break;
					}
				}

				int positionB = 0;
				for (int j = pathB.size() - 1; j >= 0; j--) {
					Id id = pathB.get(j);
					if (!id.toString().startsWith("tr_")) {
						toNode = carNetwork.getLink(id).getToNode();
						positionB = j;
						break;
					}
				}

				Id idA = ptLinkIdPathA.getFirst(), idB = ptLinkIdPathB
						.getFirst();
				List<Id> linkIds = new ArrayList<Id>();
				if (fromNode != null && toNode != null) {
					Path path =
					// new Dijkstra(carNetwork,
					// new TravelCostFunctionDistance(),
					// new TravelTimeFunctionFree())
					dijkstra.calcLeastCostPath(fromNode/* fromNode */,
							toNode/* toNode */, 0);

					if (path == null) {
						System.out.println("ptLinkIdPaths.size()"
								+ ptLinkIdPaths.size());
						StringBuffer linksA = new StringBuffer();
						for (Id linkId : pathA)
							linksA.append("link:["
									+ linkId
									+ "]from["
									+ carNetwork.getLink(linkId).getFromNode()
											.getId()
									+ "]to["
									+ carNetwork.getLink(linkId).getToNode()
											.getId() + "],length("
									+ carNetwork.getLink(linkId).getLength()
									+ "[m]).");
						System.out.println("linksA\tId :\t" + idA + "\tlinks\t"
								+ linksA);

						StringBuffer linksB = new StringBuffer();
						for (Id linkId : pathB)
							linksB.append("link:["
									+ linkId
									+ "]from["
									+ carNetwork.getLink(linkId).getFromNode()
											.getId()
									+ "]to["
									+ carNetwork.getLink(linkId).getToNode()
											.getId() + "],length("
									+ carNetwork.getLink(linkId).getLength()
									+ "[m]).");
						System.out.println("linksB\tId :\t" + idB + "\tlinks\t"
								+ linksB);
					}
					for (int j = 0; j < positionA; j++)
						linkIds.add(pathA.get(j));
					linkIds.addAll(links2Ids(path.links));
					for (int j = positionB + 1; j < pathB.size(); j++)
						linkIds.add(pathB.get(j));

				} else {
					linkIds.addAll(pathA);
					linkIds.addAll(pathB);
				}
				ptLinkIdPaths4rtf
						.add(new Tuple<String, List<Id>/* Path.links */>(idA
								+ ":" + idB /* idAB */, linkIds/* pathAB */));
			}
			paths4rtf.put(ptRouteId, ptLinkIdPaths4rtf);
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

						if (linkId.toString().startsWith("tr_")) {
							System.out.println("rtfPath\t" + rtfPath);
							System.out.println(">>>>>linNo.402\tpathA\t"
									+ pathA);
							System.exit(1);
						}
						pathA.remove(linkId);
					}

				tmpLinksB.addAll(pathB);
				for (Id linkId : tmpLinksB)
					if (!rtfPath.contains(linkId)) {

						if (linkId.toString().startsWith("tr_")) {
							System.out.println("rtfPath\t" + rtfPath);
							System.out.println(">>>>>lineNo.415\tpathB\t"
									+ pathB);
							System.exit(1);
						}
						pathB.remove(linkId);
					}
				if (pathA.isEmpty() || pathB.isEmpty()) {
					pathA.clear();
					pathB.clear();
					pathB.addAll(tmpLinksB);
					pathA.addAll(tmpLinksA);
				}

				tmpLinksRtf.addAll(pathA);
				for (Id linkId : tmpLinksRtf)
					if (pathB.contains(linkId)) {
						if (linkId.toString().startsWith("tr_")) {
							System.out.println("pathA\t" + pathA);
							System.out.println(">>>>>lineNo.433\tpathB\t"
									+ pathB);
							System.exit(1);
						}
						pathA.remove(linkId);
					}
				if (pathA.isEmpty()) {
					pathA.clear();
					pathA.addAll(tmpLinksRtf);
				}
			}
		}
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
		boolean toReturn = link.getLength() > 0 && !fromCoord.equals(toCoord);
		if (toReturn) {
			ptLinkCoordPairs.add(new Tuple<Link, Tuple<Coord, Coord>>(link,
					new Tuple<Coord, Coord>(fromCoord, toCoord)));
			// if (link.getId().toString().equals("tr_255")) {
			// System.out.println(">>>>>tr_255\tfromCoord\t" + fromCoord
			// + "\ttoCoord\t" + toCoord);
			// System.exit(3);
			// }
			if (!hasStartLink) {
				startLinks.add(link);
				System.out.println("ptLink :\t" + link.getId()
						+ "\tis the first link of a route.");
				hasStartLink = true;
			}
		} else/* exclude "null"-Link */{
			System.err.println("ptLink : " + link.getId()
					+ "\thas a length of\t" + link.getLength()
					+ "\t[m], and its fromCoord and toCoord is same.");
			links2add.add(link);
		}
		return toReturn;
	}

	private List<Id>/* Path.links */allocateRouteLink(
			Tuple<Coord, Coord> ptCoordPair) {
		Coord firstPtCoord = ptCoordPair.getFirst(), secondPtCoord = ptCoordPair
				.getSecond();
		Node firstNode = carNetwork.getNearestNode(firstPtCoord), secondNode = carNetwork
				.getNearestNode(secondPtCoord);
		boolean firstInRange = CoordUtils.calcDistance(firstPtCoord, firstNode
				.getCoord()) < 50, secondInRange = CoordUtils.calcDistance(
				secondPtCoord, secondNode.getCoord()) < 50;// circle with radius
		// of 50 [m]
		List<Id>/* Path.links */linkIds = new ArrayList<Id>/* Path.links */();
		if (isStartLink) {
			if (startLinks.contains(tmpPtLink)) /*
												 * this tmpPtLink is a startLink
												 * or endLink
												 */
				startCase(linkIds, secondNode);
		} else {
			if (firstInRange && secondInRange/* both inside */) {
				if (!firstNode.equals(secondNode))
					linkIds = links2Ids(dijkstra.calcLeastCostPath(firstNode,
							secondNode, 0).links);
				else/* firstNode==secondNode */{
					if (endLinks.contains(tmpPtLink))
						endCase(linkIds, firstNode);
					else/* this tmpPtLink is only a median link */{
						tmpPtLink.setFromNode(firstNode);
						tmpPtLink.setToNode(secondNode);
						// Node fromNode = tmpPtLink.getFromNode();
						// fromNode.getOutLinks().clear();
						// Node toNode = tmpPtLink.getToNode();
						// toNode.getInLinks().clear();
						links2add.add(tmpPtLink);
						linkIds.add(tmpPtLink.getId());
						System.out
								.println("ptlink :\t"
										+ tmpPtLink.getId()
										+ "\tmay correspond to the same \"(car)from-node\" and \"(car)to-node\" :\t"
										+ firstNode.getId()
										+ "\t, and it isn't a startLink or an endLink of ptRoute!!!!!");
					}
				}
			} else if (!firstInRange && !secondInRange/* both outside */) {
				Node fromNode = tmpPtLink.getFromNode(), toNode = tmpPtLink
						.getToNode();
				fromNode.getOutLinks().clear();
				toNode.getInLinks().clear();
				links2add.add(tmpPtLink);
				linkIds.add(tmpPtLink.getId());
			} else/* one outside, one inside */{
				if (firstInRange)
					endCase(linkIds, secondNode);
				else
					/* secondInRange */
					startCase(linkIds, secondNode);

			}
		}
		return linkIds;
	}

	private void startCase(List<Id> linkIds, Node node) {
		tmpPtLink.setToNode(node);
		// Node fromNode = tmpPtLink.getFromNode();
		// fromNode.getOutLinks().clear();
		links2add.add(tmpPtLink);
		linkIds.add(tmpPtLink.getId());
	}

	private void endCase(List<Id> linkList, Node node) {
		tmpPtLink.setFromNode(node);
		// Node toNode = tmpPtLink.getToNode();
		// toNode.getInLinks().clear();
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
		writer.writeln("----->>>>>RECTIFICATION RESULTS<<<<<-----");
		eliminateRedundancy();/* very important */
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
		writer
				.writeln("----->>>>>PLEASE DON'T FORGET THE STARTLINE OF A BUSLINE<<<<<-----");
		writer.close();
	}

	public void run() {
		allocateAllRouteLinks();
		rectifyAllocations();
		output();
	}

	// private void generateNewFiles() {
	// generateNewNetwork(null);
	// generateNewPlans();
	// generateNewSchedule();
	// }

	private void generateNewSchedule(String newTransitScheduleFilename) {
		Map<Id, List<Id>/* Path.links */> ptLinkIdPaths = convertResult();
		Set<TransitStopFacility> stops = new HashSet<TransitStopFacility>();
		stops.addAll(schedule.getFacilities().values());

		for (TransitStopFacility stop : stops) {
			Id stopLinkId = stop.getLinkId();
			List<Id>/* Path.links */linkIds = ptLinkIdPaths.get(stopLinkId);
			if (linkIds != null) {
				if (linkIds.size() > 0)
					stop.setLink(carNetwork.getLink(linkIds
							.get(linkIds.size() - 1)));
			}
		}

		Map<Id, NetworkRouteWRefs> ptRouteIdRoutes = convertResult2();
		for (TransitLine ptLine : schedule.getTransitLines().values())
			for (Entry<Id, TransitRoute> ptRouteIdRoutePair : ptLine
					.getRoutes().entrySet())
				ptRouteIdRoutePair.getValue().setRoute(
						ptRouteIdRoutes.get(ptRouteIdRoutePair.getKey()));

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
			for (Tuple<Id, List<Id>/* Path.links */> ptLinkIdPathPair : ptLinkIdPathPairs) {
				Id ptlinkId = ptLinkIdPathPair.getFirst();
				List<Id> linkIds = ptLinkIdPathMap.get(ptlinkId);
				if (linkIds == null)
					linkIds = ptLinkIdPathPair.getSecond();
				else {
					if (linkIds.size() < ptLinkIdPathPair.getSecond().size()) {
						linkIds = ptLinkIdPathPair.getSecond();
					}
				}
				ptLinkIdPathMap.put(ptlinkId, linkIds /* path.links */);

				if (ptLinkIdPathPair.getFirst().equals(new IdImpl("tr_4306")))
					System.out.println(">>>>>lineNo.702\tseconde:\t"
							+ ptLinkIdPathPair.getSecond() + ",\tbut "
							+ linkIds + "\twas put.");
			}

//		List<Tuple<Id, List<Id>>> ptlinkidPathPairs = paths.get(new IdImpl(
//				"BVB----184.27.BVB----184.H"));
//		for (Tuple<Id, List<Id>> ptlinkidPathPair : ptlinkidPathPairs)
//			if (ptlinkidPathPair.getFirst().equals(new IdImpl("tr_4306"))) {
//				System.out
//						.println(">>>>>lineNo.706\tpaths:\ttr_4306\tlinkIds\t"
//								+ ptlinkidPathPair.getSecond());
//				System.out.println(">>>>>lineNo.708\tMap:\ttr_4306\tlinkIds\t"
//						+ ptLinkIdPathMap.get(new IdImpl("tr_4306")));
//				if (ptlinkidPathPair.getSecond().size() == 0
//						|| ptLinkIdPathMap.get(new IdImpl("tr_4306")).size() == 0)
//					System.out.println(">>>>>lineNo.712\tsize==0");
//				// System.exit(7);
//			}

		return ptLinkIdPathMap;
	}

	/**
	 * @return Map<ptRouteId,Route of a TransitRoute>
	 */
	private Map<Id, NetworkRouteWRefs> convertResult2() {
		Map<Id, NetworkRouteWRefs> ptRouteIdRoutes = new HashMap<Id, NetworkRouteWRefs>();
		for (Entry<Id, List<Tuple<Id, List<Id>/* Path.links */>>> ptRouteIdLinkPathPair : paths
				.entrySet()) {
			Id ptRouteId = ptRouteIdLinkPathPair.getKey();
			List<Tuple<Id, List<Id>/* Path.links */>> ptLinksIdPaths = ptRouteIdLinkPathPair
					.getValue();
			Link startLink = null, endLink = null;
			LinkedList<Id>/* Path.links */routeLinks = new LinkedList<Id>/*
																		 * Path.
																		 * links
																		 */();
			int size = ptLinksIdPaths.size();
			for (int i = 0; i < size; i++) {
				Tuple<Id, List<Id>/* Path.links */> ptLinkIdPathPair = ptLinksIdPaths
						.get(i);
				// if (i != 0)
				// routeLinks.addAll(ptLinkIdPathPair.getSecond());
				// else {
				// Link ptLink = multiModalNetwork.getLink(ptLinkIdPathPair
				// .getFirst());
				// if (startLinks.contains(ptLink))
				// startLink = ptLink;
				// else {
				List<Id>/* Path.links */linkIds = ptLinkIdPathPair.getSecond();
				routeLinks.addAll(linkIds);
				// startLink = carNetwork.getLink(linkIds.remove(0));
				// }

			}
			endLink = carNetwork.getLink(routeLinks.removeLast());
			startLink = carNetwork.getLink(routeLinks.remove(0));
			NetworkRouteWRefs route = new LinkNetworkRouteImpl(startLink,
					endLink);
			route.setLinks(startLink, ids2links(routeLinks), endLink);
			ptRouteIdRoutes.put(ptRouteId, route);
		}
		return ptRouteIdRoutes;
	}

	private List<Link> ids2links(List<Id> ids) {
		List<Link> links = new ArrayList<Link>();
		for (Id id : ids) {
			links.add(carNetwork.getLink(id));
		}
		return links;
	}

	private void generateNewPlans(PopulationImpl pop, String newPopFile) {
		Map<Id, List<Id>/* Path.links */> ptLinkIdCarLinks = convertResult();

		for (Person person : pop.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<PlanElement> pes = plan.getPlanElements();
				for (int i = 0; i < pes.size(); i += 2) {
					ActivityImpl act = (ActivityImpl) pes.get(i);
					Id linkId = act.getLinkId();
					if (act.getType().equals("pt interaction")
							&& linkId.toString().startsWith("tr_")) {
						List<Id>/* Path.links */links = ptLinkIdCarLinks
								.get(linkId);
						if (links != null) {
							if (links.size() == 0) {
								System.err
										.println(">>>>>lineNo.786\tlinks size==0, ptlinkId:\t"
												+ linkId
												+ "\tperson:\t"
												+ person.getId());
								System.exit(1);
							}
							act.setLink(carNetwork.getLink(links.get(links
									.size() - 1)));
						}
					}
				}
			}
		}
		new PopulationWriter(pop).writeFile(newPopFile);
	}

	private void generateNewNetwork(String newNetFilename) {
		new NetworkWriter(carNetwork).writeFile(newNetFilename);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String multiModalNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.multimodal.mini.xml";
		String transitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/transitSchedule.networkOevModellBln.xml";
		String carNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.car.mini.xml";
		String outputFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/busLineAllocation.txt";
		String popFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/plan.routedOevModell.BVB344.moreLegPlan_Agent.xml";

		String newNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newNetTest.xml";
		String newTransitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newScheduleTest.xml";
		String newPopFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newPopTest.xml";

//		String multiModalNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/network.multimodal.xml.gz";
//		String transitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/transitSchedule.networkOevModellBln.xml.gz";
//		String carNetworkFile = "../berlin-bvg09/net/miv_small/m44_344_small_ba.xml.gz";
//		String outputFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/busLineAllocationBigger.txt";
//		String popFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/plan.routedOevModell.xml.gz";
//
//		String newNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newNetBiggerTest.xml";
//		String newTransitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newScheduleBigger.xml";
//		String newPopFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newPopBiggerTest.xml";

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

		NetworkLayer carNetwork = new NetworkLayer();
		new MatsimNetworkReader(carNetwork).readFile(carNetworkFile);

		BusLineAllocator busLineAllocator = new BusLineAllocator(carNetwork,
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
