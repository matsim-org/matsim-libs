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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import playground.yu.utils.io.SimpleWriter;

/**
 * tries to allocate bus stop coordinations to links in MATSim "car" network
 * 
 * @author yu
 * 
 */
public class BusStopAllocator {
	private NetworkImpl network;
	private Collection<TransitStopFacility> stops;
	private Map<Id, Id> resultIds = new HashMap<Id, Id>();
	private Map<Id, String> resultIsToNodes = new HashMap<Id, String>();
	private String outputFile;

	/**
	 * @param network
	 *            a MATsim <code>NetworkImpl</code>
	 * @param stopCoords
	 *            a Collection of bus stop coordinations
	 * @param outputFile
	 *            file path of the output file
	 */
	public BusStopAllocator(NetworkImpl network,
			Collection<TransitStopFacility> stops, String outputFile) {
		this.network = network;
		this.stops = stops;
		this.outputFile = outputFile;
	}

	protected void allocateAllStops1() {
		for (TransitStopFacility stop : stops)
			allocateStop1(stop);
	}

	protected void allocateAllStops2() {
		for (TransitStopFacility stop : stops)
			allocateStop2(stop);
	}

	private void allocateStop1(TransitStopFacility stop) {
		Coord stopCoord = stop.getCoord();
		Id stopId = stop.getId();
		Link link = network.getNearestLink(stopCoord);
		resultIds.put(stopId, link.getId());
		if (link != null) {
			Node node = network.getNearestNode(stopCoord);
			String isToNode = null;
			if (link.getToNode().equals(node))
				isToNode = "to";
			else if (link.getFromNode().equals(node))
				isToNode = "from";
			else
				isToNode = node.getId().toString();
			resultIsToNodes.put(stopId, isToNode);
		}
	}

	private void allocateStop2(TransitStopFacility stop) {
		Coord stopCoord = stop.getCoord();
		Id stopId = stop.getId();
		Link link = network.getNearestRightEntryLink(stopCoord);
		resultIds.put(stopId, link.getId());
		if (link != null) {
			Collection<NodeImpl> nodes = network.getNearestNodes(stopCoord, 25);
			String isToNode = null;
			if (nodes.contains(link.getToNode()))
				isToNode = "to";
			else if (nodes.contains(link.getFromNode()))
				isToNode = "from";
			else
				isToNode = network.getNearestNode(stopCoord).getId().toString();
			resultIsToNodes.put(stopId, isToNode);
		}
	}

	private void output() {
		SimpleWriter writer = new SimpleWriter(outputFile);
		writer.writeln("stopId\tlinkId\tfrom_to_nodeId");
		for (Id stopId : resultIds.keySet())
			writer.writeln(stopId.toString() + "\t"
					+ resultIds.get(stopId).toString() + "\t"
					+ resultIsToNodes.get(stopId));
		writer.close();
	}

	public void run1() {
		allocateAllStops1();
		output();
	}

	public void run2() {
		allocateAllStops2();
		output();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String multiModalNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.multimodal.mini.xml", 
		transitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/scheduleTest.xml", 
		carNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.car.mini.xml", 
		outputFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/stopAllocation2.txt";

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

		NetworkLayer carNetwork = new NetworkLayer();
		new MatsimNetworkReader(carNetwork).readFile(carNetworkFile);

		BusStopAllocator stopAllocator = new BusStopAllocator(carNetwork,
				schedule.getFacilities().values(), outputFile);
		stopAllocator.run2();
	}
}
