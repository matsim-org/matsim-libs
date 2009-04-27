/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePseudoNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.tryout;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.FacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;

public class CreatePseudoNetwork {

	public static final String INPUT_FACILITIES = "../thesis-data/examples/berta/facilities.xml";
	public static final String INPUT_SCHEDULE = "../thesis-data/examples/berta/schedule.xml";
	public static final String OUTPUT_SCHEDULE = "";
	public static final String OUTPUT_NETWORK = "../thesis-data/examples/berta/pseudoNetwork.xml";
	
	public void run() {
		FacilitiesImpl facilities = new FacilitiesImpl();
		NetworkLayer network = new NetworkLayer();
		long linkIdCounter = 0;
		long nodeIdCounter = 0;
		new MatsimFacilitiesReader(facilities).readFile(INPUT_FACILITIES);
		TransitSchedule schedule = new TransitSchedule();
		try {
			new TransitScheduleReaderV1(schedule, null, facilities).readFile(INPUT_SCHEDULE);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Map<Tuple<Facility, Facility>, Link> links = new HashMap<Tuple<Facility, Facility>, Link>();
		Map<Facility, Node> nodes = new HashMap<Facility, Node>();
		
		for (TransitLine tLine : schedule.getTransitLines().values()) {
			for (TransitRoute tRoute : tLine.getRoutes().values()) {
				Facility prevFacility = null;
				for (TransitRouteStop stop : tRoute.getStops()) {
					Facility facility = stop.getStopFacility();
					if (prevFacility != null) {
						Tuple<Facility, Facility> connection = new Tuple<Facility, Facility>(prevFacility, facility);
						Link link = links.get(connection);
						if (link == null) {
							Node fromNode = nodes.get(prevFacility);
							if (fromNode == null) {
								fromNode = network.createNode(new IdImpl(nodeIdCounter++), prevFacility.getCoord());
								nodes.put(prevFacility, fromNode);
							}
							Node toNode = nodes.get(facility);
							if (toNode == null) {
								toNode = network.createNode(new IdImpl(nodeIdCounter++), facility.getCoord());
								nodes.put(facility, toNode);
							}
							link = network.createLink(new IdImpl(linkIdCounter++), fromNode, toNode, CoordUtils.calcDistance(prevFacility.getCoord(), facility.getCoord()), 50.0 / 3.6, 1600, 1);
							links.put(new Tuple<Facility, Facility>(prevFacility, facility), link);
						}
						// add link to route
					}
					prevFacility = facility;
				}
			}
		}
		
		new NetworkWriter(network, OUTPUT_NETWORK).write();
		
	}
	
	public static void main(final String[] args) {
		new CreatePseudoNetwork().run();
	}
}
