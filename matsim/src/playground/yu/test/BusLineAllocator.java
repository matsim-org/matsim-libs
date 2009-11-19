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

	private static NetworkImpl carNetwork;
	private Map<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> coordPairs;// <ptRouteId,<ptLink<fromNodeCoord,toNodeCoord>>>
	private Map<Id, List<Tuple<String, Tuple<Coord, Coord>>>> coordPairs4rectification;// <ptRouteId,<ptLinkId:next_ptLinkId<fromNodeCoord,toNodeCoord>>>
	private Map<Id, List<Tuple<Id, Path>>> resultPaths = new HashMap<Id, List<Tuple<Id, Path>>>();// <ptRouteId,List<ptLinkId,Path(linkId
	// linkId linkId (never pt linkId))>>
	private Map<Id, List<Tuple<String, Path>>> resultPaths4rectification = new HashMap<Id, List<Tuple<String, Path>>>();// <ptRouteId,List<ptLinkId:next_ptLinkId,Path(linkId
	// linkId linkId (never pt linkId))>>
	private String outputFile;
	private Dijkstra dijkstra;
	private Link tmpPtLink = null;
	private Id tmpPtRouteId = null;
	private static Set<Link> startLinks = new HashSet<Link>();
	private static Set<Link> nullLinks = new HashSet<Link>();
	private Map<Link, Node> startLinksNewToNodes = new HashMap<Link, Node>();
	private TransitSchedule schedule;
	private NetworkLayer multiModalNetwork;

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
		this.coordPairs4rectification = createCoordPairs4rectification(schedule);
		this.outputFile = outputFile;
		this.dijkstra = new Dijkstra(carNetwork,
				new TravelCostFunctionDistance(), new TravelTimeFunctionFree());
	}

	protected void allocateAllRouteLinks() {
		for (Entry<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> routeLinkCoordPair : coordPairs
				.entrySet()) {
			tmpPtRouteId = routeLinkCoordPair.getKey();
			List<Tuple<Id, Path>> resultPtLinkIdPath = resultPaths
					.get(tmpPtRouteId);
			if (resultPtLinkIdPath == null)
				resultPtLinkIdPath = new ArrayList<Tuple<Id, Path>>();

			for (Tuple<Link, Tuple<Coord, Coord>> linkCoordPair : routeLinkCoordPair
					.getValue()) {
				tmpPtLink = linkCoordPair.getFirst();
				Path path = allocateRouteLink(linkCoordPair.getSecond());
				if (path.links.size() <= 0 && startLinks.contains(tmpPtLink))
					path.links.add(tmpPtLink);
				resultPtLinkIdPath.add(new Tuple<Id, Path>(tmpPtLink.getId(),
						path));
			}
			resultPaths.put(tmpPtRouteId, resultPtLinkIdPath);
		}
	}

	private void rectifyAllocations() {
		for (Entry<Id, List<Tuple<String, Tuple<Coord, Coord>>>> routeLinkCoordPair : coordPairs4rectification
				.entrySet()) {
			Id localTmpPtRouteId = routeLinkCoordPair.getKey();
			List<Tuple<String, Path>> resultPtLinkIdPath = resultPaths4rectification
					.get(localTmpPtRouteId);
			if (resultPtLinkIdPath == null)
				resultPtLinkIdPath = new ArrayList<Tuple<String, Path>>();

			for (Tuple<String, Tuple<Coord, Coord>> linkCoordPair : routeLinkCoordPair
					.getValue()) {
				String localTmpPtLinkId = linkCoordPair.getFirst();
				resultPtLinkIdPath.add(new Tuple<String, Path>(
						localTmpPtLinkId, allocateRouteLink(linkCoordPair
								.getSecond())));
			}

			resultPaths4rectification
					.put(localTmpPtRouteId, resultPtLinkIdPath);
		}
	}

	private void eliminateRedundancy() {
		for (Entry<Id, List<Tuple<String, Path>>> resultPath4rectificationEntry : resultPaths4rectification
				.entrySet()) {
			Id routeId = resultPath4rectificationEntry.getKey();
			// List<Tuple<Id, Path>> resultPtLinkIdPathPairs = resultPaths
			// .get(routeId);
			for (Tuple<String, Path> ptLinkIdPathPair4rectification : resultPath4rectificationEntry
					.getValue()) {
				String[] ptLinkIds = ptLinkIdPathPair4rectification.getFirst()
						.split(":");
				Path pathA = null, pathB = null;
				for (Tuple<Id, Path> resultPtLinkIdPathPair : resultPaths
						.get(routeId)) {
					if (ptLinkIds[0].equals(resultPtLinkIdPathPair.getFirst()
							.toString()))
						pathA = resultPtLinkIdPathPair.getSecond();
					else if (ptLinkIds[1].equals(resultPtLinkIdPathPair
							.getFirst().toString()))
						pathB = resultPtLinkIdPathPair.getSecond();
					if (pathA != null && pathB != null)
						break;
				}
				Path rectifiedPath = ptLinkIdPathPair4rectification.getSecond();
				List<Link> tmpLinks = new ArrayList<Link>();
				tmpLinks.addAll(pathA.links);
				for (Link link : tmpLinks)
					if (!rectifiedPath.links.contains(link))
						pathA.links.remove(link);
				tmpLinks.clear();
				tmpLinks.addAll(pathB.links);
				for (Link link : tmpLinks)
					if (!rectifiedPath.links.contains(link))
						pathB.links.remove(link);
				tmpLinks.clear();
				tmpLinks.addAll(pathA.links);
				for (Link link : tmpLinks)
					if (pathB.links.contains(link))
						pathA.links.remove(link);
			}
		}
	}

	/**
	 * @return 
	 *         Map<TransitRouteId,List<Tuple<TransitLinkId,Tuple<fromCoord,toCoord
	 *         >>>>
	 */
	private static Map<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> createCoordPairs(
			TransitSchedule schedule) {
		Map<Id, List<Tuple<Link, Tuple<Coord, Coord>>>> coordPairs = new HashMap<Id, List<Tuple<Link, Tuple<Coord, Coord>>>>();
		for (TransitLine ptLine : schedule.getTransitLines().values()) {
			for (TransitRoute ptRoute : ptLine.getRoutes().values()) {
				Id ptRouteId = ptRoute.getId();
				List<Tuple<Link, Tuple<Coord, Coord>>> ptLinkCoordPairs = coordPairs
						.get(ptRouteId);
				if (ptLinkCoordPairs == null)
					ptLinkCoordPairs = new ArrayList<Tuple<Link, Tuple<Coord, Coord>>>();

				NetworkRouteWRefs route = ptRoute.getRoute();

				Link startLink = route.getStartLink();
				if (startLink.getLength() > 0) {
					ptLinkCoordPairs.add(new Tuple<Link, Tuple<Coord, Coord>>(
							startLink, new Tuple<Coord, Coord>(startLink
									.getFromNode().getCoord(), startLink
									.getToNode().getCoord())));
					Id startLinkId = startLink.getId();
					startLinks.add(startLink);
					System.out.println("ptLink :\t" + startLinkId
							+ "\tis the first link of a route.");
				} else {
					System.err.println("ptLink : " + startLink.getId()
							+ "\thas a length of\t" + startLink.getLength()
							+ "\t[m]");
					nullLinks.add(startLink);
				}

				for (Link link : route.getLinks()) {
					if (link.getLength() > 0)
						ptLinkCoordPairs
								.add(new Tuple<Link, Tuple<Coord, Coord>>(link,
										new Tuple<Coord, Coord>(link
												.getFromNode().getCoord(), link
												.getToNode().getCoord())));
					else {
						System.err.println("ptLink : " + link.getId()
								+ "\thas a length of\t" + link.getLength()
								+ "\t[m]");
						nullLinks.add(link);
					}
				}

				Link endLink = route.getEndLink();
				if (endLink.getLength() > 0)
					ptLinkCoordPairs.add(new Tuple<Link, Tuple<Coord, Coord>>(
							endLink, new Tuple<Coord, Coord>(endLink
									.getFromNode().getCoord(), endLink
									.getToNode().getCoord())));
				else {
					System.err.println("ptLink : " + endLink.getId()
							+ "\thas a length of\t" + endLink.getLength()
							+ "\t[m]");
					nullLinks.add(endLink);
				}
				coordPairs.put(ptRouteId, ptLinkCoordPairs);
			}
		}
		return coordPairs;
	}

	/**
	 * @param ptNet
	 *            a network, that has pt links
	 * @param schedule
	 * @return
	 */
	private static Map<Id, List<Tuple<String, Tuple<Coord, Coord>>>> createCoordPairs4rectification(
			TransitSchedule schedule) {
		Map<Id, List<Tuple<String, Tuple<Coord, Coord>>>> coordPairs4rectification = new HashMap<Id, List<Tuple<String, Tuple<Coord, Coord>>>>();
		for (TransitLine ptLine : schedule.getTransitLines().values()) {
			for (TransitRoute ptRoute : ptLine.getRoutes().values()) {
				Id ptRouteId = ptRoute.getId();
				List<Tuple<String, Tuple<Coord, Coord>>> ptLinkCoordPairs = coordPairs4rectification
						.get(ptRouteId);
				if (ptLinkCoordPairs == null)
					ptLinkCoordPairs = new ArrayList<Tuple<String, Tuple<Coord, Coord>>>();

				NetworkRouteWRefs route = ptRoute.getRoute();

				List<Link> tmpPtLinkList = new ArrayList<Link>();

				Link startLink = route.getStartLink();
				if (startLink.getLength() > 0)
					tmpPtLinkList.add(startLink);
				else {
					System.out.println("ptLink :\t" + startLink.getId()
							+ "\tis the first link of a route.");
					System.err.println("ptLink : " + startLink.getId()
							+ "\thas a length of\t" + startLink.getLength()
							+ "\t[m]");
				}

				for (Link link : route.getLinks())
					if (link.getLength() > 0)
						tmpPtLinkList.add(link);
					else
						System.err.println("ptLink : " + link.getId()
								+ "\thas a length of\t" + link.getLength()
								+ "\t[m]");

				Link endLink = route.getEndLink();
				if (endLink.getLength() > 0)
					tmpPtLinkList.add(endLink);
				else
					System.err.println("ptLink : " + endLink.getId()
							+ "\thas a length of\t" + endLink.getLength()
							+ "\t[m]");
				int tmpPtLinkListSize = tmpPtLinkList.size();
				if (tmpPtLinkListSize > 1)
					for (int i = 0; i < tmpPtLinkListSize - 1; i++) {
						Link currentLink = tmpPtLinkList.get(i);
						Link nextLink = tmpPtLinkList.get(i + 1);
						ptLinkCoordPairs
								.add(new Tuple<String, Tuple<Coord, Coord>>(
										currentLink.getId() + ":"
												+ nextLink.getId(),
										new Tuple<Coord, Coord>(currentLink
												.getFromNode().getCoord(),
												nextLink.getToNode().getCoord())));
					}
				coordPairs4rectification.put(ptRouteId, ptLinkCoordPairs);
			}
		}
		return coordPairs4rectification;
	}

	private Path allocateRouteLink(Tuple<Coord, Coord> coordPair) {
		Node firstNode = carNetwork.getNearestNode(coordPair.getFirst());
		Node secondNode = carNetwork.getNearestNode(coordPair.getSecond());
		if (firstNode.equals(secondNode)) {
			System.out.println("ptlinkId : " + tmpPtLink
					+ "\thas the same \"from-node\" and \"to-node\" : "
					+ firstNode.getId()
					+ "\t. Maybe it is a startLink of a route");
			if (startLinks.contains(tmpPtLink))
				startLinksNewToNodes.put(tmpPtLink, firstNode);
		}
		return dijkstra.calcLeastCostPath(firstNode, secondNode, 0);
	}

	private void output() {
		SimpleWriter writer = new SimpleWriter(outputFile);
		writer.writeln("ptRouteId\t:\tptlinkId\t:\tlinks");
		for (Entry<Id, List<Tuple<Id, Path>>> routeLinkPathEntry : resultPaths
				.entrySet()) {
			for (Tuple<Id, Path> linkPathPair : routeLinkPathEntry.getValue()) {
				StringBuffer line = new StringBuffer(routeLinkPathEntry
						.getKey()
						+ "\t:\t" + linkPathPair.getFirst() + "\t:\t");
				for (Link link : linkPathPair.getSecond().links)
					line.append(link.getId() + "\t");
				writer.writeln(line.toString());
			}
		}
		writer.writeln("----->>>>>HALFWAY RECTIFICATION RESULTS<<<<<-----");
		for (Entry<Id, List<Tuple<String, Path>>> routeLinkPathEntry : resultPaths4rectification
				.entrySet()) {
			for (Tuple<String, Path> linkPathPair : routeLinkPathEntry
					.getValue()) {
				StringBuffer line = new StringBuffer(routeLinkPathEntry
						.getKey()
						+ "\t:\t" + linkPathPair.getFirst() + "\t:\t");
				for (Link link : linkPathPair.getSecond().links)
					line.append(link.getId() + "\t");
				writer.writeln(line.toString());
			}
		}
		writer.writeln("----->>>>>RECTIFICATION RESULTS<<<<<-----");
		eliminateRedundancy();
		writer.writeln("ptRouteId\t:\tptlinkId\t:\tlinks");
		for (Entry<Id, List<Tuple<Id, Path>>> routeLinkPathEntry : resultPaths
				.entrySet()) {
			for (Tuple<Id, Path> linkPathPair : routeLinkPathEntry.getValue()) {
				StringBuffer line = new StringBuffer(routeLinkPathEntry
						.getKey()
						+ "\t:\t" + linkPathPair.getFirst() + "\t:\t");
				for (Link link : linkPathPair.getSecond().links)
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
		Map<Id, List<Link>> ptLinkIdLinks = convertResult();
		Set<TransitStopFacility> stops = new HashSet<TransitStopFacility>();
		stops.addAll(schedule.getFacilities().values());

		for (TransitStopFacility stop : stops) {
			List<Link> links = ptLinkIdLinks.get(stop.getLinkId());
			if (links != null)
				if (links.size() > 0)
					stop.setLink(links.get(links.size() - 1));
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

	private Map<Id, List<Link>> convertResult() {
		Map<Id, List<Link>> ptLinkIdPathMap = new HashMap<Id, List<Link>>();
		for (List<Tuple<Id, Path>> ptLinkIdPathlist : resultPaths.values())
			for (Tuple<Id, Path> ptLinkIdPathPair : ptLinkIdPathlist) {
				Id ptLinkId = ptLinkIdPathPair.getFirst();
				Link ptLink = multiModalNetwork.getLink(ptLinkId);
				if (!startLinksNewToNodes.keySet().contains(ptLink))
					ptLinkIdPathMap.put(ptLinkId,
							ptLinkIdPathPair.getSecond().links);
				else {
					LinkedList<Link> links = new LinkedList<Link>();
					links.add(ptLink);
					ptLinkIdPathMap.put(ptLinkId, links);
				}
			}
		return ptLinkIdPathMap;
	}

	/**
	 * @return Map<ptRouteId,Route of a TransitRoute>
	 */
	private Map<Id, NetworkRouteWRefs> convertResult2() {
		Map<Id, NetworkRouteWRefs> ptRouteIdRoutes = new HashMap<Id, NetworkRouteWRefs>();
		for (Entry<Id, List<Tuple<Id, Path>>> ptRouteIdLinkPathPair : resultPaths
				.entrySet()) {
			Id ptRouteId = ptRouteIdLinkPathPair.getKey();
			List<Tuple<Id, Path>> ptLinksIdPaths = ptRouteIdLinkPathPair
					.getValue();
			Link startLink = null, endLink = null;
			LinkedList<Link> routeLinks = new LinkedList<Link>();
			int size = ptLinksIdPaths.size();
			for (int i = 0; i < size; i++) {
				Tuple<Id, Path> ptLinkIdPathPair = ptLinksIdPaths.get(i);
				if (i != 0)
					routeLinks.addAll(ptLinkIdPathPair.getSecond().links);
				else {
					Link ptLink = multiModalNetwork.getLink(ptLinkIdPathPair
							.getFirst());
					if (startLinksNewToNodes.keySet().contains(ptLink))
						startLink = ptLink;
					else {
						List<Link> linkList = ptLinkIdPathPair.getSecond().links;
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
		Map<Id, List<Link>> ptLinkIdCarLinks = convertResult();
		for (Person person : pop.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<PlanElement> pes = plan.getPlanElements();
				for (int i = 0; i < pes.size(); i += 2) {
					ActivityImpl act = (ActivityImpl) pes.get(i);
					Id linkId = act.getLinkId();
					if (act.getType().equals("pt interaction")
							&& linkId.toString().startsWith("tr_")) {
						List<Link> links = ptLinkIdCarLinks.get(linkId);
						act.setLink(links.get(links.size() - 1));
					}
				}
			}
		}
		new PopulationWriter(pop, newPopFile).write();
	}

	private void generateNewNetwork(String newNetFilename) {
		Set<TransportMode> modes = new HashSet<TransportMode>();
		modes.add(TransportMode.car);
		modes.add(TransportMode.pt);
		for (Link link : startLinksNewToNodes.keySet()) {
			link.setToNode(startLinksNewToNodes.get(link));
			link.getFromNode().getOutLinks().clear();
			link.setAllowedModes(modes);
			carNetwork.addLink(link);
			carNetwork.addNode(link.getFromNode());
		}
		for (Link link : nullLinks) {
			link.getFromNode().getOutLinks().clear();
			link.getToNode().getInLinks().clear();
			link.setAllowedModes(modes);
			carNetwork.addLink(link);
			carNetwork.addNode(link.getFromNode());
		}
		new NetworkWriter(carNetwork, newNetFilename).write();
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
		new MatsimPopulationReader(pop, multiModalNetwork).readFile(popFile);
		busLineAllocator.generateNewPlans(pop, newPopFile);
	}
}
