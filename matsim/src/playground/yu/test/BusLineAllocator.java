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
import java.util.List;
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
	private Map<Id, List<Tuple<Id, Tuple<Coord, Coord>>>> coordPairs;// <ptRouteId,<ptLinkId<fromNodeCoord,toNodeCoord>>>
	/*
	 * <ptRouteId,List<ptLinkId,Path(linkId linkId linkId (never pt linkId))>>
	 */
	private Map<Id, List<Tuple<Id, Path>>> resultPaths = new HashMap<Id, List<Tuple<Id, Path>>>();
	private String outputFile;
	private Dijkstra dijkstra;
	private Id tmpPtLinkId = null, tmpPtRouteId = null;

	/**
	 * @param network
	 *            a nomral MATsim <code>NetworkImpl</code>, in which there
	 *            aren't "pt" links
	 * @param stopCoords
	 *            a Collection of
	 *            <ptRouteId,<ptLinkId<fromNodeCoord,toNodeCoord>>>
	 * @param outputFile
	 *            file path of the output file
	 */
	public BusLineAllocator(NetworkImpl network,
			Map<Id, List<Tuple<Id, Tuple<Coord, Coord>>>> coordPairs,
			String outputFile) {
		this.network = network;
		this.coordPairs = coordPairs;
		this.outputFile = outputFile;
		this.dijkstra = new Dijkstra(network, new TravelCostFunctionDistance(),
				new TravelTimeFunctionFree());
	}

	protected void allocateAllRouteLinks() {
		for (Entry<Id, List<Tuple<Id, Tuple<Coord, Coord>>>> routeLinkCoordPair : coordPairs
				.entrySet()) {
			tmpPtRouteId = routeLinkCoordPair.getKey();
			for (Tuple<Id, Tuple<Coord, Coord>> linkCoordPair : routeLinkCoordPair
					.getValue()) {
				List<Tuple<Id, Path>> resultList = resultPaths
						.get(tmpPtRouteId);
				if (resultList == null)
					resultList = new ArrayList<Tuple<Id, Path>>();
				resultList.add(new Tuple<Id, Path>(linkCoordPair.getFirst(),
						allocateRouteLink(linkCoordPair.getSecond())));
			}
		}
	}

	private void rectifyAllocations() {

	}

	/**
	 * @return 
	 *         Map<TransitRouteId,List<Tuple<TransitLinkId,Tuple<fromCoord,toCoord
	 *         >>>>
	 */
	private static Map<Id, List<Tuple<Id, Tuple<Coord, Coord>>>> createCoordPairs(
			NetworkLayer ptNet, TransitSchedule schedule) {
		Map<Id, List<Tuple<Id, Tuple<Coord, Coord>>>> coordPairs = new HashMap<Id, List<Tuple<Id, Tuple<Coord, Coord>>>>();
		for (TransitLine ptLine : schedule.getTransitLines().values()) {
			for (TransitRoute ptRoute : ptLine.getRoutes().values()) {
				List<Tuple<Id, Tuple<Coord, Coord>>> ptLinkCoordPairs = coordPairs
						.get(ptRoute.getId());
				if (ptLinkCoordPairs == null)
					ptLinkCoordPairs = new ArrayList<Tuple<Id, Tuple<Coord, Coord>>>();

				NetworkRouteWRefs route = ptRoute.getRoute();
				Link startLink = route.getStartLink();

				if (startLink.getLength() > 0) {
					ptLinkCoordPairs.add(new Tuple<Id, Tuple<Coord, Coord>>(
							startLink.getId(), new Tuple<Coord, Coord>(
									startLink.getFromNode().getCoord(),
									startLink.getToNode().getCoord())));
					System.out.println("ptLink :\t" + startLink.getId()
							+ "\tis the first link of a route.");
				} else
					System.err.println("ptLink : " + startLink.getId()
							+ "\thas a length of\t" + startLink.getLength()
							+ "\t[m]");

				for (Link link : route.getLinks()) {
					if (link.getLength() > 0)
						ptLinkCoordPairs
								.add(new Tuple<Id, Tuple<Coord, Coord>>(link
										.getId(), new Tuple<Coord, Coord>(link
										.getFromNode().getCoord(), link
										.getToNode().getCoord())));
					else
						System.err.println("ptLink : " + link.getId()
								+ "\thas a length of\t" + link.getLength()
								+ "\t[m]");
				}

				Link endLink = route.getEndLink();
				if (endLink.getLength() > 0)
					ptLinkCoordPairs.add(new Tuple<Id, Tuple<Coord, Coord>>(
							endLink.getId(), new Tuple<Coord, Coord>(endLink
									.getFromNode().getCoord(), endLink
									.getToNode().getCoord())));
				else
					System.err.println("ptLink : " + endLink.getId()
							+ "\thas a length of\t" + endLink.getLength()
							+ "\t[m]");
			}
		}
		return coordPairs;
	}

	private Path allocateRouteLink(Tuple<Coord, Coord> coordPair) {
		Node firstNode = network.getNearestNode(coordPair.getFirst());
		Node secondNode = network.getNearestNode(coordPair.getSecond());
		if (firstNode.equals(secondNode))
			System.out.println("ptlinkId : " + tmpPtLinkId
					+ "\thas the same \"from-node\" and \"to-node\" : "
					+ firstNode.getId()
					+ "\t. Maybe it is a startLink of a route");
		return dijkstra.calcLeastCostPath(firstNode, secondNode, 0);
	}

	// route.getStartLink().getFromNode().getCoord()

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
		String multiModalNetworkFile = "bvg09/network.multimodal.mini.xml", transitScheduleFile = "bvg09/transitSchedule.networkOevModellBln.xml", carNetworkFile = "bvg09/network.car.mini.xml", outputFile = "bvg09/busLineAllocation.txt";

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
