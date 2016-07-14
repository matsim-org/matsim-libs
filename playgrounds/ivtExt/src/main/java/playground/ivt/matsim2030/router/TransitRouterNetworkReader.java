/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ivt.matsim2030.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.*;
import org.xml.sax.Attributes;

import java.util.Map;
import java.util.Stack;

/**
 * This is copied from Christoph's playground, to avoid depending on it
 * and have some control over it, in particular put the dtd in jar
 * @author thibautd
 */
public class TransitRouterNetworkReader extends MatsimXmlParser {

	private final static String NETWORK = "transitRouterNetwork";
	private final static String LINKS = "links";
	private final static String NODE = "node";
	private final static String NODES = "nodes";
	private final static String LINK = "link";

	private final TransitRouterNetwork network;
	private final TransitSchedule transitSchedule;

	private final Counter nodesCounter = new Counter("# read nodes: ");
	private final Counter linksCounter = new Counter("# read links: ");
	
	public TransitRouterNetworkReader(
			final TransitSchedule schedule,
			final TransitRouterNetwork network) {
		this.network = network;
		this.transitSchedule = schedule;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		
		if (NODE.equals(name)) {
			startNode(atts);
		} else if (LINK.equals(name)) {
			startLink(atts);
		} else if (NETWORK.equals(name)) {
			startNetwork();
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

	private void startNetwork() {
		nodesCounter.reset();
		linksCounter.reset();
	}

	@SuppressWarnings("unchecked")
	private void startNode(final Attributes atts) {	
		
		final Id<Node> nodeId = Id.create(atts.getValue("id"), Node.class);
		final Id<TransitStopFacility> stopId = Id.create(atts.getValue("stopfacility"), TransitStopFacility.class);
		final Id<TransitRoute> routeId = Id.create(atts.getValue("route"), TransitRoute.class);
		final Id<TransitLine> lineId = Id.create(atts.getValue("line"), TransitLine.class);

		final TransitLine line = this.transitSchedule.getTransitLines().get(lineId);
		if ( line == null ) {
			throw new RuntimeException( "no transit line "+lineId+" found in schedule "+transitSchedule );
		}

		final TransitRoute route = line.getRoutes().get(routeId);
		if ( route == null ) {
			throw new RuntimeException( "no route "+routeId+" found in line "+line );
		}

		final TransitStopFacility stopFacility = this.transitSchedule.getFacilities().get(stopId);
		if ( stopFacility == null ) {
			throw new RuntimeException( "no stop facility "+stopId+" found in schedule "+transitSchedule );
		}

		final TransitRouteStop stop = route.getStop(stopFacility);
		if ( stop == null ) {
			throw new RuntimeException( "no stop for facility "+stopFacility+" found in route "+route );
		}
		
		final TransitRouterNetworkNode node = new TransitRouterNetworkNode(nodeId, stop, route, line);
		// XXX Should be done with the addNode method... But it throws an UnsupportedOpperationException.
		((Map<Id<Node>, TransitRouterNetworkNode>) network.getNodes()).put(nodeId, node);
	}
	
	@SuppressWarnings("unchecked")
	private void startLink(final Attributes atts) {
		
		final Id<Link> linkId = Id.create(atts.getValue("id"), Link.class);
		final Id<Node> fromId = Id.create(atts.getValue("from"), Node.class);
		final Id<Node> toId = Id.create(atts.getValue("to"), Node.class);

		final String routeAtt = atts.getValue("route");
		final Id<TransitRoute> routeId =
			routeAtt != null ?
					Id.create(routeAtt, TransitRoute.class) :
				null;
		
		final String lineAtt = atts.getValue("line");
		final Id<TransitLine> lineId =
			lineAtt != null ?
				Id.create(lineAtt, TransitLine.class) :
				null;
		
		final TransitLine line =
			lineId != null ?
				this.transitSchedule.getTransitLines().get(lineId) :
				null;
		final TransitRoute route =
			line != null && routeId != null ?
				line.getRoutes().get(routeId) :
				null;
		
		final TransitRouterNetworkNode fromNode = this.network.getNodes().get(fromId);
		final TransitRouterNetworkNode toNode = this.network.getNodes().get(toId);

		if (fromNode == null) {
			throw new RuntimeException("FromNode " + fromId + " was not found!");
		}
		if (toNode == null) {
			throw new RuntimeException("ToNode " + fromId + " was not found!");
		}
		
		final double length = Double.parseDouble(atts.getValue("length"));
		final TransitRouterNetworkLink link = new TransitRouterNetworkLink(linkId, fromNode, toNode, route, line, length);
		
		((Map<Id<Link>, TransitRouterNetworkLink>) network.getLinks()).put(linkId, link);
		((Map<Id<Link>, TransitRouterNetworkLink>) fromNode.getOutLinks()).put(link.getId(), link);
		((Map<Id<Link>, TransitRouterNetworkLink>) toNode.getInLinks()).put(link.getId(), link);
	}
}
