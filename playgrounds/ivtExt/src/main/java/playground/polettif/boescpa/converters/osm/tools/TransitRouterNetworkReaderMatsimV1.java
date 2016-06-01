/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkReaderMatsimV1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.polettif.boescpa.converters.osm.tools;

import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.Attributes;

/**
 * A reader for transit router network-files of MATSim according to <code>transitRouterNetwork_v1.dtd</code>.
 *
 * @author cdobler
 */
// copy from christoph-playground
public class TransitRouterNetworkReaderMatsimV1 extends MatsimXmlParser {

	private final static String NETWORK = "transitRouterNetwork";
	private final static String LINKS = "links";
	private final static String NODE = "node";
	private final static String NODES = "nodes";
	private final static String LINK = "link";

	private final Scenario scenario;
	private final TransitRouterNetwork network;
	private final TransitSchedule transitSchedule;

	private final Counter nodesCounter = new Counter("# read nodes: ");
	private final Counter linksCounter = new Counter("# read links: ");
	
	public TransitRouterNetworkReaderMatsimV1(final Scenario scenario, final TransitRouterNetwork network) {
		super();
		this.scenario = scenario;
		this.network = network;
		this.transitSchedule = scenario.getTransitSchedule();
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		
		if (NODE.equals(name)) {
			startNode(atts);
		} else if (LINK.equals(name)) {
			startLink(atts);
		} else if (NETWORK.equals(name)) {
			startNetwork(atts);
		} else if (LINKS.equals(name)) {
			startLinks(atts);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {

		// If the entire network is read, we have to call finishInit().
		if (NETWORK.equals(name)) {
			network.finishInit();
		} else if (NODE.equals(name)) {
			nodesCounter.incCounter();
		} else if (NODES.equals(name)) {
			nodesCounter.printCounter();
		} else if (LINK.equals(name)) {
			linksCounter.incCounter();
		} else if (LINKS.equals(name)) {
			linksCounter.printCounter();
		}
	}

	private void startNetwork(final Attributes atts) {
		nodesCounter.reset();
		linksCounter.reset();
	}

	private void startLinks(final Attributes atts) {
		// nothing to do here
	}

	@SuppressWarnings("unchecked")
	private void startNode(final Attributes atts) {	
		
		Id<Node> nodeId = Id.create(atts.getValue("id"), Node.class);
		Id<TransitStopFacility> stopId = Id.create(atts.getValue("stopfacility"), TransitStopFacility.class);
		Id<TransitRoute> routeId = Id.create(atts.getValue("route"), TransitRoute.class);
		Id<TransitLine> lineId = Id.create(atts.getValue("line"), TransitLine.class);
		
		TransitLine line = this.transitSchedule.getTransitLines().get(lineId);
		TransitRoute route = line.getRoutes().get(routeId);
		TransitStopFacility stopFacility = this.transitSchedule.getFacilities().get(stopId);
		TransitRouteStop stop = route.getStop(stopFacility);
		
		TransitRouterNetworkNode node = new TransitRouterNetworkNode(nodeId, stop, route, line);
		((Map<Id<Node>, TransitRouterNetworkNode>) network.getNodes()).put(nodeId, node);
	}
	
	@SuppressWarnings("unchecked")
	private void startLink(final Attributes atts) {
		
//		Id linkId = Id.create(atts.getValue("id"));
//		Id fromId = Id.create(atts.getValue("from"));
//		Id toId = Id.create(atts.getValue("to"));
		Id<Link> linkId = Id.create(atts.getValue("id"), Link.class);
		Id<Node> fromId = Id.create(atts.getValue("from"), Node.class);
		Id<Node> toId = Id.create(atts.getValue("to"), Node.class);
		
		String string = null;
		Id<TransitRoute> routeId = null;
		Id<TransitLine> lineId = null;
		
		string = atts.getValue("route");
//		if (string != null) routeId = Id.create(string);
		if (string != null) routeId = Id.create(string, TransitRoute.class);
		
		string = atts.getValue("line");
//		if (string != null) lineId = Id.create(string);
		if (string != null) lineId = Id.create(string, TransitLine.class);
		
		TransitLine line = null;
		TransitRoute route = null;
		if (lineId != null) {
			line = this.transitSchedule.getTransitLines().get(lineId);
			route = null;
			if (line != null) route = line.getRoutes().get(routeId);			
		}
		
		TransitRouterNetworkNode fromNode = this.network.getNodes().get(fromId);
		TransitRouterNetworkNode toNode = this.network.getNodes().get(toId);
		if (fromNode == null) {
			throw new RuntimeException("FromNode " + fromId + " was not found!");			
		} else if (toNode == null) {
			throw new RuntimeException("ToNode " + fromId + " was not found!");
		}
		
		double length = Double.parseDouble(atts.getValue("length"));
		TransitRouterNetworkLink link = new TransitRouterNetworkLink(linkId, fromNode, toNode, route, line, length);
		
		((Map<Id<Link>, TransitRouterNetworkLink>) network.getLinks()).put(linkId, link);
		((Map<Id<Link>, TransitRouterNetworkLink>) fromNode.getOutLinks()).put(link.getId(), link);
		((Map<Id<Link>, TransitRouterNetworkLink>) toNode.getInLinks()).put(link.getId(), link);
	}
}