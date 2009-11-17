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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
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

	private NetworkImpl network;
	private Map<Id, Tuple<Coord, Coord>> coordPairs;// <ptLinkId,<fromNodeCoord,toNodeCoord>>
	private Map<Id, Path> resultPaths = new HashMap<Id, Path>();// linkId linkId
	// linkId (never
	// pt linkId)
	private String outputFile;
	private Dijkstra dijkstra;
	private Id tmpPtLinkId = null;

	/**
	 * @param network
	 *            a nomral MATsim <code>NetworkImpl</code>, in which there
	 *            aren't "pt" links
	 * @param stopCoords
	 *            a Collection of bus lines
	 * @param outputFile
	 *            file path of the output file
	 */
	public BusLineAllocator(NetworkImpl network,
			Map<Id, Tuple<Coord, Coord>> coordPairs, String outputFile) {
		this.network = network;
		this.coordPairs = coordPairs;
		this.outputFile = outputFile;
		this.dijkstra = new Dijkstra(network, new TravelCostFunctionDistance(),
				new TravelTimeFunctionFree());
	}

	protected void allocateAllRouteLinks() {
		for (Entry<Id, Tuple<Coord, Coord>> coordPair : coordPairs.entrySet()) {
			tmpPtLinkId = coordPair.getKey();
			allocateRouteLink(coordPair.getValue());
		}
	}

	private static Map<Id, Tuple<Coord, Coord>> createCoordPairs(
			NetworkLayer ptNet, TransitSchedule schedule) {
		Map<Id, Tuple<Coord, Coord>> coordPairs = new HashMap<Id, Tuple<Coord, Coord>>();
		for (TransitLine ptLine : schedule.getTransitLines().values()) {
			for (TransitRoute ptRoute : ptLine.getRoutes().values()) {
				NetworkRouteWRefs route = ptRoute.getRoute();
				Link startLink = route.getStartLink();

				if (startLink.getLength() > 0) {
					coordPairs.put(startLink.getId(), new Tuple<Coord, Coord>(
							startLink.getFromNode().getCoord(), startLink
									.getToNode().getCoord()));
					System.out.println("ptLink :\t" + startLink.getId()
							+ "\tis the first link of a route.");
				} else
					System.err.println("ptLink : " + startLink.getId()
							+ "\thas a length of\t" + startLink.getLength()
							+ "\t[m]");

				for (Link link : route.getLinks()) {
					if (link.getLength() > 0)
						coordPairs.put(link.getId(), new Tuple<Coord, Coord>(
								link.getFromNode().getCoord(), link.getToNode()
										.getCoord()));
					else
						System.err.println("ptLink : " + link.getId()
								+ "\thas a length of\t" + link.getLength()
								+ "\t[m]");
				}

				Link endLink = route.getEndLink();
				if (endLink.getLength() > 0)
					coordPairs.put(endLink.getId(), new Tuple<Coord, Coord>(
							endLink.getFromNode().getCoord(), endLink
									.getToNode().getCoord()));
				else
					System.err.println("ptLink : " + endLink.getId()
							+ "\thas a length of\t" + endLink.getLength()
							+ "\t[m]");
			}
		}
		return coordPairs;
	}

	// private void allocateLine(TransitLine line) {
	// for (TransitRoute transitRoute : line.getRoutes().values()) {
	// NetworkRouteWRefs route = transitRoute.getRoute();
	// allocateRouteLink(route.getStartLink());
	// for(Link link:route.getLinks())
	// allocateRouteLink(route.getEndLink());
	// }
	//
	// }
	private void allocateRouteLink(Tuple<Coord, Coord> coordPair) {
		Node firstNode = network.getNearestNode(coordPair.getFirst());
		Node secondNode = network.getNearestNode(coordPair.getSecond());
		if (firstNode.equals(secondNode))
			System.out.println("ptlinkId : " + tmpPtLinkId
					+ "\thas the same \"from-node\" and \"to-node\" : "
					+ firstNode.getId()
					+ "\t. Maybe it is a startLink of a route");
		resultPaths.put(tmpPtLinkId, dijkstra.calcLeastCostPath(firstNode,
				secondNode, 0));
	}

	// route.getStartLink().getFromNode().getCoord()

	private void output() {
		SimpleWriter writer = new SimpleWriter(outputFile);
		writer.writeln("ptlinkId\t:\tlinks");
		for (Entry<Id, Path> pathEntry : resultPaths.entrySet()) {
			StringBuffer line = new StringBuffer(pathEntry.getKey() + "\t:\t");
			for (Link link : pathEntry.getValue().links)
				line.append(link.getId() + "\t");
			writer.writeln(line.toString());
		}
		writer.close();
	}

	public void run() {
		allocateAllRouteLinks();
		output();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String multiModalNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.multimodal.mini.xml", transitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/scheduleTest.xml", carNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.car.mini.xml", outputFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/busLineAllocation.txt";

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

		BusLineAllocator BusLineAllocator = new BusLineAllocator(carNetwork,
				createCoordPairs(multiModalNetwork, schedule), outputFile);
		BusLineAllocator.run();
	}
}
