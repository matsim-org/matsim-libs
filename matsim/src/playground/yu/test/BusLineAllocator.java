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

	private NetworkImpl carNetwork;
	private Map<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> coordPairs;// <ptRouteId,<ptLink<fromNodeCoord,toNodeCoord>>>
	// private Map<Id, List<Tuple<String, Tuple<Coord, Coord>>>>
	// coordPairs4rtf;//
	// <ptRouteId,<ptLinkId:next_ptLinkId<fromNodeCoord,toNodeCoord>>>
	/*
	 * <ptRouteId,List<ptLinkId,Path_Links(shouldn't be pt linkId, but there
	 * also is exception))>>
	 */
	private Map<Id, List<Tuple<Id, List<Link>/* Path.links */>>> paths = new HashMap<Id, List<Tuple<Id, List<Link>/*
																												 * Path.
																												 * links
																												 */>>>();
	/*
	 * <ptRouteId,List<ptLinkId:next_ptLinkId,Path(linkIds (shouldn't be pt
	 * linkIds))>>
	 */
	private Map<Id, List<Tuple<String, List<Link>/* Path.links */>>> paths4rtf = new HashMap<Id, List<Tuple<String, List<Link>/*
																															 * Path.
																															 * links
																															 */>>>();
	// 
	private String outputFile;
	private Dijkstra dijkstra;
	private Link tmpPtLink = null;
	private Id tmpPtRouteId = null;
	private Set<Link> startLinks = new HashSet<Link>(),
			endLinks = new HashSet<Link>(), nullLinks = new HashSet<Link>(),
			links2add = new HashSet<Link>();
	private Set<Node> nodes2add = new HashSet<Node>();
	private Map<Link, Node> startLinksNewToNodes = new HashMap<Link, Node>();
	private TransitSchedule schedule;
	private NetworkLayer multiModalNetwork;
	private boolean hasStartLink = false;
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
	public BusLineAllocator(NetworkImpl carNetwork,
			NetworkLayer multiModalNetwork, TransitSchedule schedule,
			String outputFile) {
		this.carNetwork = carNetwork;
		this.multiModalNetwork = multiModalNetwork;
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
			List<Tuple<Id, List<Link>/* Path.links */>> ptLinkIdPaths = paths
					.get(tmpPtRouteId);
			if (ptLinkIdPaths == null)
				ptLinkIdPaths = new ArrayList<Tuple<Id, List<Link>/* Path. links */>>();

			for (Tuple<Link, Tuple<Coord, Coord>> linkCoordPair : routeLinkCoordPair
					.getValue()) {
				tmpPtLink = linkCoordPair.getFirst();
				List<Link>/* Path.links */pathLinks = allocateRouteLink(linkCoordPair
						.getSecond()/* ptCoordPair */);

				if (pathLinks != null)/*
									 * TODO think? How to handle the median
									 * link, which has the same fromNode(carNet)
									 * and toNode(carNet)?
									 */
					ptLinkIdPaths.add(new Tuple<Id, List<Link>/* Path.links */>(
							tmpPtLink.getId(), pathLinks));
				tmpPtLink = null;
			}
			paths.put(tmpPtRouteId, ptLinkIdPaths);
		}
		for (Link link : links2add) {
			link.setAllowedModes(modes);
			carNetwork.addLink(link);
		}
		for (Node node : nodes2add)
			carNetwork.addNode(node);
		for (Link link : nullLinks) {
			link.getFromNode().getOutLinks().clear();
			link.getToNode().getInLinks().clear();
			link.setAllowedModes(modes);
			carNetwork.addLink(link);
			carNetwork.addNode(link.getFromNode());
			carNetwork.addNode(link.getToNode());
		}
		carNetwork.connect();
	}

	private void rectifyAllocations() {
		// 1. step add connection (link) between pathA.lastLink to
		// path.firstLink, if there is not so a connection

		// 2. step
		for (Entry<Id, List<Tuple<Id, List<Link>/* Path.links */>>> ptRouteIdPtLinkIdpaths : paths
				.entrySet()) {

			Id ptRouteId = ptRouteIdPtLinkIdpaths.getKey();
			List<Tuple<String/* ptLinkIdpair */, List<Link>/* Path.links */>> ptLinkIdPaths4rtf = paths4rtf
					.get(ptRouteId);
			if (ptLinkIdPaths4rtf == null)
				ptLinkIdPaths4rtf = new ArrayList<Tuple<String, List<Link>/*
																		 * Path.links
																		 */>>();

			List<Tuple<Id, List<Link>/* Path.links */>> ptLinkIdPaths = ptRouteIdPtLinkIdpaths
					.getValue();
			for (int i = 0; i < ptLinkIdPaths.size() - 1; i++) {
				Tuple<Id, List<Link>/* Path.links */> ptLinkIdPathA = ptLinkIdPaths
						.get(i), ptLinkIdPathB = ptLinkIdPaths.get(i + 1);
				List<Link> pathA = ptLinkIdPathA.getSecond(), pathB = ptLinkIdPathB
						.getSecond();

				Path path = dijkstra.calcLeastCostPath(pathA.get(0)
						.getFromNode()/* fromNode */, pathB.get(pathB.size() - 1)
						.getToNode()/* toNode */, 0);

				Id idA = ptLinkIdPathA.getFirst(), idB = ptLinkIdPathB
						.getFirst();

				if (path == null) {
					StringBuffer linksA = new StringBuffer();
					for (Link link : pathA)
						linksA.append(link.getId() + "[from:"
								+ link.getFromNode().getId() + "|to:"
								+ link.getToNode().getId() + "]|");
					System.out.println("linksA\tId :\t" + idA + "\tlinks\t"
							+ linksA);

					StringBuffer linksB = new StringBuffer();
					for (Link link : pathB)
						linksB.append(link.getId() + "[from:"
								+ link.getFromNode().getId() + "|to:"
								+ link.getToNode().getId() + "]|");
					System.out.println("linksB\tId :\t" + idB + "\tlinks\t"
							+ linksB);
				}

				ptLinkIdPaths4rtf
						.add(new Tuple<String, List<Link>/* Path.links */>(idA
								+ ":" + idB /* idAB */, path.links/* pathAB */));
			}
			paths4rtf.put(ptRouteId, ptLinkIdPaths4rtf);
		}
	}

	private void eliminateRedundancy() {
		for (Entry<Id, List<Tuple<String, List<Link>/* Path.links */>>> path4rtfEntry : paths4rtf
				.entrySet()) {
			Id routeId = path4rtfEntry.getKey();
			List<Tuple<String, List<Link>/* Path.links */>> ptLinkIdsPaths4rtf = path4rtfEntry
					.getValue();
			for (int j = 0; j < ptLinkIdsPaths4rtf.size(); j++) {
				Tuple<String, List<Link>/* Path.links */> ptLinkIdsPath4rtf = ptLinkIdsPaths4rtf
						.get(j);
				String[] ptLinkIds4rtf = ptLinkIdsPath4rtf.getFirst()
						.split(":");
				List<Link>/* Path.links */pathA = null, pathB = null;
				List<Tuple<Id, List<Link>>> ptLinkIdPaths = paths.get(routeId);

				Tuple<Id, List<Link>/* Path.links */> ptLinkIdPathA = ptLinkIdPaths
						.get(j), ptLinkIdPathB = ptLinkIdPaths.get(j + 1);
				String ptLinkIdStrA = ptLinkIdPathA.getFirst().toString(), ptLinkIdStrB = ptLinkIdPathB
						.getFirst().toString();
				if (ptLinkIds4rtf[0].equals(ptLinkIdStrA))
					pathA = ptLinkIdPathA.getSecond();
				if (ptLinkIds4rtf[1].equals(ptLinkIdStrB))
					pathB = ptLinkIdPathB.getSecond();
				if (pathA == null && pathB == null) {
					System.err.println("linkIds don't match :\t"
							+ ptLinkIds4rtf + "\t<->\t" + ptLinkIdStrA + "\t"
							+ ptLinkIdStrB);
					System.exit(1);
				}

				List<Link>/* Path.links */rtfPath = ptLinkIdsPath4rtf.getSecond();
				List<Link>/* Path.links */tmpLinks = new ArrayList<Link>/*
																	 * Path.links
																	 */();
				tmpLinks.addAll(pathA);
				for (Link link : tmpLinks)
					if (!rtfPath.contains(link))
						pathA.remove(link);
				tmpLinks.clear();
				tmpLinks.addAll(pathB);
				for (Link link : tmpLinks)
					if (!rtfPath.contains(link))
						pathB.remove(link);
				tmpLinks.clear();
				tmpLinks.addAll(pathA);
				for (Link link : tmpLinks)
					if (pathB.contains(link))
						pathA.remove(link);
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
					createCoordPair(endLink, ptLinkCoordPairs);
					endLinks.add(endLink);
					// else
					// endLinks.add(ptLinkCoordPairs.get(
					// ptLinkCoordPairs.size() - 1).getFirst());
					hasStartLink = false;

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
		// boolean toReturn = link.getLength() > 0 &&
		// !fromCoord.equals(toCoord);
		// if (toReturn)
		{
			ptLinkCoordPairs.add(new Tuple<Link, Tuple<Coord, Coord>>(link,
					new Tuple<Coord, Coord>(fromCoord, toCoord)));
			if (!hasStartLink) {
				startLinks.add(link);
				System.out.println("ptLink :\t" + link.getId()
						+ "\tis the first link of a route.");
				hasStartLink = true;
			}
		}
		// else/* exclude "null"-Link */{
		// System.err.println("ptLink : " + link.getId()
		// + "\thas a length of\t" + link.getLength()
		// + "\t[m], and its fromCoord and toCoord is same.");
		// nullLinks.add(link);
		// }
		// return toReturn;
	}

	// /**
	// * @param ptNet
	// * a network, that has pt links
	// * @param schedule
	// * @return
	// */
	// private Map<Id, List<Tuple<String, Tuple<Coord, Coord>>>>
	// createCoordPairs4rtf(
	// TransitSchedule schedule) {
	// Map<Id, List<Tuple<String, Tuple<Coord, Coord>>>> coordPairs4rtf = new
	// HashMap<Id, List<Tuple<String, Tuple<Coord, Coord>>>>();
	//
	// for (Entry<Id, List<Tuple<Link, Tuple<Coord, Coord>>>>
	// ptRouteIdPtLinkCoordPairs : coordPairs
	// .entrySet()) {
	// Id ptRouteId = ptRouteIdPtLinkCoordPairs.getKey();
	//
	// List<Tuple<String, Tuple<Coord, Coord>>> ptLinkIdsCoordPairs4rtf =
	// coordPairs4rtf
	// .get(ptRouteId);
	// if (ptLinkIdsCoordPairs4rtf == null)
	// ptLinkIdsCoordPairs4rtf = new ArrayList<Tuple<String, Tuple<Coord,
	// Coord>>>();
	//
	// List<Tuple<Link, Tuple<Coord, Coord>>> ptLinkCoordPairs =
	// ptRouteIdPtLinkCoordPairs
	// .getValue();
	// for (int i = 0; i < ptLinkCoordPairs.size() - 1; i++) {
	// Tuple<Link, Tuple<Coord, Coord>> ptLinkCoordPairA = ptLinkCoordPairs
	// .get(i), ptLinkCoordPairB = ptLinkCoordPairs.get(i + 1);
	//
	// ptLinkIdsCoordPairs4rtf
	// .add(new Tuple<String, Tuple<Coord, Coord>>(
	// ptLinkCoordPairA.getFirst().getId() + ":"
	// + ptLinkCoordPairB.getFirst().getId()/* newLinkIds */,
	// new Tuple<Coord, Coord>(ptLinkCoordPairA
	// .getSecond().getFirst(),
	// ptLinkCoordPairB.getSecond()
	// .getSecond()/* newCoordPair */))/* ptLinkIdsCoordPair4rtf */);
	// }
	// coordPairs4rtf.put(ptRouteId, ptLinkIdsCoordPairs4rtf);
	// }
	// return coordPairs4rtf;
	// }

	private List<Link>/* Path.links */allocateRouteLink(
			Tuple<Coord, Coord> ptCoordPair) {
		Coord firstPtCoord = ptCoordPair.getFirst(), secondPtCoord = ptCoordPair
				.getSecond();
		Node firstNode = carNetwork.getNearestNode(firstPtCoord), secondNode = carNetwork
				.getNearestNode(secondPtCoord);
		boolean firstInRange = CoordUtils.calcDistance(firstPtCoord, firstNode
				.getCoord()) < 50, secondInRange = CoordUtils.calcDistance(
				secondPtCoord, secondNode.getCoord()) < 50;// circle with radius
		// of 50 [m]
		List<Link>/* Path.links */links = new ArrayList<Link>/* Path.links */();
		if (firstInRange && secondInRange/* both inside */) {
			// if (!firstNode.equals(secondNode))
			links = dijkstra.calcLeastCostPath(firstNode, secondNode, 0).links;
			if (links == null) {
				System.out.println("firstNode:\t" + firstNode.getId()
						+ "\tsecondeNode:\t" + secondNode.getId());
				System.exit(1);
			} else if (links.size() == 0) {
				if (tmpPtLink.getLength() == 0
						|| firstPtCoord.equals(secondPtCoord))
					nullCase(links);
			}
			// else {
			// System.out.println("Path.size==0\tfirstNode:\t"
			// + firstNode.getId() + "\tsecondeNode:\t"
			// + secondNode.getId() + "\ttmpPtLink:\t"
			// + tmpPtLink.getId());
			// System.exit(1);
			// }
			// else/* firstNode==secondNode */{
			// if (startLinks.contains(tmpPtLink)) /*
			// * this tmpPtLink is a
			// * startLink or endLink
			// */
			// startCase(links, secondNode);
			// else if (endLinks.contains(tmpPtLink))
			// endCase(links, firstNode);
			// else/* this tmpPtLink is only a median link */{
			// links = null;
			// System.err
			// .println("ptlink : "
			// + tmpPtLink.getId()
			// +
			// "\tmay correspond to the same \"(car)from-node\" and \"(car)to-node\" :\t"
			// + firstNode.getId()
			// + "\t, and it isn't a startLink or an endLink of ptRoute!!!!!");
			// System.exit(1);
			// }
			// System.out
			// .println("ptlink :\t"
			// + tmpPtLink.getId()
			// +
			// "\tmay have the same \"(car)from-node\" and \"(car)to-node\" : "
			// + firstNode.getId());
			// }
		} else if (!firstInRange && !secondInRange/* both outside */)
			nullCase(links);
		else/* one outside, one inside */{
			if (firstInRange)
				endCase(links, secondNode);
			else
				/* secondInRange */
				startCase(links, secondNode);
		}
		return links;
	}

	private void nullCase(List<Link> linkList) {
		Node fromNode = tmpPtLink.getFromNode(), toNode = tmpPtLink.getToNode();
		fromNode.getOutLinks().clear();
		toNode.getInLinks().clear();
		links2add.add(tmpPtLink);
		nodes2add.add(fromNode);
		nodes2add.add(toNode);
		linkList.add(tmpPtLink);
		nullLinks.add(tmpPtLink);
	}

	private void startCase(List<Link> linkList, Node node) {
		tmpPtLink.setToNode(node);
		Node fromNode = tmpPtLink.getFromNode();
		fromNode.getOutLinks().clear();
		links2add.add(tmpPtLink);
		nodes2add.add(fromNode);
		linkList.add(tmpPtLink);
	}

	private void endCase(List<Link> linkList, Node node) {
		tmpPtLink.setFromNode(node);
		Node toNode = tmpPtLink.getToNode();
		toNode.getInLinks().clear();
		links2add.add(tmpPtLink);
		nodes2add.add(toNode);
		linkList.add(tmpPtLink);
	}

	private void output() {
		SimpleWriter writer = new SimpleWriter(outputFile);
		writer.writeln("ptRouteId\t:\tptlinkId\t:\tlinks");
		for (Entry<Id, List<Tuple<Id, List<Link>/* Path.links */>>> routeLinkPathEntry : paths
				.entrySet()) {
			for (Tuple<Id, List<Link>/* Path.links */> linkPathPair : routeLinkPathEntry
					.getValue()) {
				StringBuffer line = new StringBuffer(routeLinkPathEntry
						.getKey()
						+ "\t:\t" + linkPathPair.getFirst() + "\t:\t");
				for (Link link : linkPathPair.getSecond())
					line.append(link.getId() + "\t");
				writer.writeln(line.toString());
			}
		}
		writer.writeln("----->>>>>HALFWAY RECTIFICATION RESULTS<<<<<-----");
		for (Entry<Id, List<Tuple<String, List<Link>/* Path.links */>>> routeLinkPathEntry : paths4rtf
				.entrySet()) {
			for (Tuple<String, List<Link>/* Path.links */> linkPathPair : routeLinkPathEntry
					.getValue()) {
				StringBuffer line = new StringBuffer(routeLinkPathEntry
						.getKey()
						+ "\t:\t" + linkPathPair.getFirst() + "\t:\t");
				for (Link link : linkPathPair.getSecond())
					line.append(link.getId() + "\t");
				writer.writeln(line.toString());
			}
		}
		writer.writeln("----->>>>>RECTIFICATION RESULTS<<<<<-----");
		eliminateRedundancy();/* very important */
		writer.writeln("ptRouteId\t:\tptlinkId\t:\tlinks");
		for (Entry<Id, List<Tuple<Id, List<Link>/* Path.links */>>> routeLinkPathEntry : paths
				.entrySet()) {
			for (Tuple<Id, List<Link>/* Path.links */> linkPathPair : routeLinkPathEntry
					.getValue()) {
				StringBuffer line = new StringBuffer(routeLinkPathEntry
						.getKey()
						+ "\t:\t" + linkPathPair.getFirst() + "\t:\t");
				for (Link link : linkPathPair.getSecond())
					line.append(link.getId() + "\t");
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
		Map<Id, List<Link>/* Path.links */> ptLinkIdPaths = convertResult();
		Set<TransitStopFacility> stops = new HashSet<TransitStopFacility>();
		stops.addAll(schedule.getFacilities().values());

		for (TransitStopFacility stop : stops) {
			Id stopLinkId = stop.getLinkId();
			List<Link>/* Path.links */links = ptLinkIdPaths.get(stopLinkId);
			if (links != null) {
				if (links.size() > 0)
					stop.setLink(links.get(links.size() - 1));
			} else {
				System.err.println("WARN path.links==null, stopLink :\t"
						+ stopLinkId);
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

	private Map<Id, List<Link>/* Path.links */> convertResult() {
		Map<Id, List<Link>/* Path.links */> ptLinkIdPathMap = new HashMap<Id, List<Link>/*
																					 * Path.
																					 * links
																					 */>();
		for (List<Tuple<Id, List<Link>/* Path.links */>> ptLinkIdPathlist : paths
				.values())
			for (Tuple<Id, List<Link>/* Path.links */> ptLinkIdPathPair : ptLinkIdPathlist)
				ptLinkIdPathMap.put(ptLinkIdPathPair.getFirst(),
						ptLinkIdPathPair.getSecond()/* path.links */);
		return ptLinkIdPathMap;
	}

	/**
	 * @return Map<ptRouteId,Route of a TransitRoute>
	 */
	private Map<Id, NetworkRouteWRefs> convertResult2() {
		Map<Id, NetworkRouteWRefs> ptRouteIdRoutes = new HashMap<Id, NetworkRouteWRefs>();
		for (Entry<Id, List<Tuple<Id, List<Link>/* Path.links */>>> ptRouteIdLinkPathPair : paths
				.entrySet()) {
			Id ptRouteId = ptRouteIdLinkPathPair.getKey();
			List<Tuple<Id, List<Link>/* Path.links */>> ptLinksIdPaths = ptRouteIdLinkPathPair
					.getValue();
			Link startLink = null, endLink = null;
			LinkedList<Link>/* Path.links */routeLinks = new LinkedList<Link>/*
																			 * Path.
																			 * links
																			 */();
			int size = ptLinksIdPaths.size();
			for (int i = 0; i < size; i++) {
				Tuple<Id, List<Link>/* Path.links */> ptLinkIdPathPair = ptLinksIdPaths
						.get(i);
				if (i != 0)
					routeLinks.addAll(ptLinkIdPathPair.getSecond());
				else {
					Link ptLink = multiModalNetwork.getLink(ptLinkIdPathPair
							.getFirst());
					if (startLinksNewToNodes.keySet().contains(ptLink))
						startLink = ptLink;
					else {
						List<Link>/* Path.links */linkList = ptLinkIdPathPair
								.getSecond();
						startLink = linkList.remove(0);
						routeLinks.addAll(linkList);
					}
				}
			}
			endLink = routeLinks.removeLast();
			NetworkRouteWRefs route = new LinkNetworkRouteImpl(startLink,
					endLink);
			route.setLinks(startLink, routeLinks, endLink);
			ptRouteIdRoutes.put(ptRouteId, route);
		}
		return ptRouteIdRoutes;
	}

	private void generateNewPlans(PopulationImpl pop, String newPopFile) {
		Map<Id, List<Link>/* Path.links */> ptLinkIdCarLinks = convertResult();
		for (Person person : pop.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<PlanElement> pes = plan.getPlanElements();
				for (int i = 0; i < pes.size(); i += 2) {
					ActivityImpl act = (ActivityImpl) pes.get(i);
					Id linkId = act.getLinkId();
					if (act.getType().equals("pt interaction")
							&& linkId.toString().startsWith("tr_")) {
						List<Link>/* Path.links */links = ptLinkIdCarLinks
								.get(linkId);
						act.setLink(links.get(links.size() - 1));
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
		String newTransitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newSchedule.xml";
		String newPopFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newPopTest.xml";

		// String multiModalNetworkFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/network.multimodal.xml.gz";
		// String transitScheduleFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/transitSchedule.networkOevModellBln.xml.gz";
		// String carNetworkFile =
		// "../berlin-bvg09/net/miv_small/m44_344_small_ba.xml.gz";
		// String outputFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/busLineAllocationBigger.txt";
		// String popFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/plan.routedOevModell.xml.gz";
		//
		// String newNetworkFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newNetBiggerTest.xml";
		// String newTransitScheduleFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newScheduleBigger.xml";
		// String newPopFile =
		// "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newPopBiggerTest.xml";

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
	}
}
