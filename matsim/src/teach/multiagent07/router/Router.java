/* *********************************************************************** *
 * project: org.matsim.*
 * Router.java
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

package teach.multiagent07.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.BasicNet;
import org.matsim.basic.v01.BasicNode;
import org.matsim.basic.v01.BasicRoute;
import org.matsim.utils.identifiers.IdI;

import teach.multiagent07.interfaces.EventHandlerI;
import teach.multiagent07.net.CANetworkReader;
import teach.multiagent07.util.Event;


public class Router implements EventHandlerI{

	BasicNet targetNet;
	RouterNet net;
	Map<IdI, Integer> starttimeMap = new HashMap<IdI, Integer>();

	public Router (BasicNet net) {
		this.targetNet = net;
	}

	public void readNetwork (String filename) {
		net = new RouterNet();
		CANetworkReader reader = new CANetworkReader(net, filename);
		reader.readNetwork();
		net.connect();
		net.build();

	}

	public void handleEvent(Event event) {
		if (event.type == Event.ENTER_LINK) {
			// store time of enterlink for later use
			starttimeMap.put(event.agentId, event.time);
		} else if (event.type == Event.LEAVE_LINK) {
			// get the right link int the ROUTER net
			RouterLink link = (RouterLink) net.getLinks().get(event.link.getId());
			// get starttime for the link
			int starttime = starttimeMap.get(event.agentId);
			link.addTravelTime(starttime, event.time - starttime);
		}
	}

	public BasicRoute reRoute(IdI fromNodeId, IdI toNodeId, double starttime) {
		BasicRoute<BasicNode> route = new BasicRoute<BasicNode>();

		RouterNode fromNode = (RouterNode) net.getNodes().get(fromNodeId.toString());
		RouterNode toNode = (RouterNode) net.getNodes().get(toNodeId.toString());

		// re-route in Router net
		List<RouterNode> nodes;
		nodes = net.calcCheapestRoute(fromNode, toNode, starttime);

		//	the fromNode is part of nodesList but should not be in
		// the final route
		nodes.remove(0);

		// convert route to targetNet
		ArrayList<BasicNode> targetNodes = new ArrayList<BasicNode>();
		for (RouterNode node : nodes) {
			targetNodes.add((BasicNode)targetNet.getNodes().get(node.getId()));
		}
		route.setRoute(targetNodes);

		return route;
	}

	public void TTDump() {
		for (Object o : net.getLinks()) {
			RouterLink link = (RouterLink)o;
			link.printTravelTimeDump();
		}
	}

	public void resetTravelTimes() {
		for (Object o : net.getLinks()) {
			RouterLink link = (RouterLink)o;
			link.resetTravelTimes();
		}
	}
}
