/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim.cadyts;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author johannes
 * 
 */
public class ODCalibrator implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private static final Logger logger = Logger.getLogger(ODCalibrator.class);

	private final Network network;

	private final Map<Node, Node> real2virtual;

	private final PlanToPlanStepBasedOnEvents p2p;

	private final SimResultsAdaptor adaptor;

	private final Map<Id<Person>, Node> person2Node;

	private final double scaleFactor;

	public ODCalibrator(Network network, CadytsContext cadytsContext, KeyMatrix odMatrix, ZoneCollection zones) {
		this.network = network;
		this.p2p = (PlanToPlanStepBasedOnEvents) cadytsContext.getPlansTranslator();
		this.adaptor = cadytsContext.getSimResultsAdaptor();
		this.scaleFactor = cadytsContext.getScalingFactor();

		this.person2Node = new IdentityHashMap<>();
		this.real2virtual = new IdentityHashMap<>();

		buildVirtualNetwork(odMatrix, zones, cadytsContext.getCounts());
	}

	private void buildVirtualNetwork(KeyMatrix odMartix, ZoneCollection zones, Counts counts) {
		zones.setPrimaryKey("gsvId");
		Set<String> keys = odMartix.keys();
		Map<String, Node> key2Node = new HashMap<String, Node>();

		for (String key : keys) {
			Zone zone = zones.get(key);
			Id<Node> nodeId = Id.createNodeId(String.format("virtual.%s", zone.getAttribute("nuts3_name")));
			Node node = network.getFactory().createNode(nodeId, MatsimCoordUtils.pointToCoord(zone.getGeometry().getCentroid()));
			network.addNode(node);
			key2Node.put(key, node);
		}

		for (String i : keys) {
			for (String j : keys) {
				if (i != j) {
					Double volume = odMartix.get(i, j);
					if (volume == null)
						volume = 0.0;
					
					if (Math.floor(volume / scaleFactor) > 5.0) {
						Node ni = key2Node.get(i);
						Node nj = key2Node.get(j);
						Id<Link> linkId = Id.createLinkId(String.format("virtual.%s.%s", i, j));
						Link link = network.getFactory().createLink(linkId, ni, nj);
						network.addLink(link);

						p2p.addCalibratedItem(link.getId());

						Count count = counts.createAndAddCount(link.getId(), link.getId().toString());

						volume = volume / scaleFactor;
						volume = volume / 24.0;
						for (int h = 1; h < 25; h++) {
							count.createVolume(h, volume);
						}
					}
				}
			}
		}

		
		int cnt = 0;
		for (Node node : network.getNodes().values()) {
			Zone zone = zones.get(new Coordinate(node.getCoord().getX(), node.getCoord().getY()));
			if (zone != null) {
				Node vNode = key2Node.get(zone.getAttribute("gsvId"));
				real2virtual.put(node, vNode);
			} else {
				cnt++;
			}
		}

		if (cnt > 0) {
			logger.warn(String.format("%s nodes cannot be assigned to a virtual node.", cnt));
		}
	}

	@Override
	public void reset(int iteration) {
		adaptor.resetVirtualCounts();
		person2Node.clear();

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Node startNode = person2Node.remove(event.getPersonId());
		Node endNode = network.getLinks().get(event.getLinkId()).getToNode();

		Node vStart = real2virtual.get(startNode);
		Node vEnd = real2virtual.get(endNode);

		if (vStart != null && vEnd != null) {
			Link vLink = NetworkUtils.getConnectingLink(vStart, vEnd);
			if (vLink != null) {
				p2p.handleEvent(new PersonDepartureEvent(event.getTime(), event.getPersonId(), vLink.getId(), event.getLegMode()));
				p2p.handleEvent(new LinkLeaveEvent(event.getTime(), event.getPersonId(), vLink.getId(), null));
				p2p.handleEvent(new PersonArrivalEvent(event.getTime(), event.getPersonId(), vLink.getId(), event.getLegMode()));

				adaptor.addVirtualCount(vLink);
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Node startNode = network.getLinks().get(event.getLinkId()).getFromNode();
		person2Node.put(event.getPersonId(), startNode);

	}
}
