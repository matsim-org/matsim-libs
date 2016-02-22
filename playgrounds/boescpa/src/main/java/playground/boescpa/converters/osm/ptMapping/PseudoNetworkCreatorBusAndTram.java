/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm.ptMapping;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;

/**
 * Provides the utilities to create a pseudo-network for all pt-modes not interfering with normal traffic,
 * such as for example trains or ships.
 *
 * Heavyly based on org.matsim.pt.utils.CreatePseudoNetwork.java by mrieser.
 *
 * @author boescpa
 */
public class PseudoNetworkCreatorBusAndTram {

    private final TransitSchedule schedule;
    private final Network network;
    private final String prefix;

    private final Map<TransitStopFacility, Node> nodes = new HashMap<>();
    private final Map<Tuple<Node, Node>, Link> links = new HashMap<>();
    private final Map<Tuple<Node, Node>, TransitStopFacility> stopFacilities = new HashMap<>();
    private final Map<TransitStopFacility, List<TransitStopFacility>> facilityCopies = new HashMap<>();

    private long linkIdCounter = 0;

    private final Set<String> modes = new HashSet<>();

    protected PseudoNetworkCreatorBusAndTram(final TransitSchedule schedule, final Network network, final String networkIdPrefix) {
        this.schedule = schedule;
        this.network = network;
        this.prefix = networkIdPrefix;
		modes.add("tram"); modes.add("car");
    }

    /**
     * All PT-modes that are not connected to the given network, get their own pseudo network integrated
     * in the given network. This also includes linking the stations to the new links and nodes and routing the
     * the pt-lines (can all be done at once...).
     *
     * Heavyly based on org.matsim.pt.utils.CreatePseudoNetwork.java.
     */
    protected void createLine(TransitRoute route) {
        ArrayList<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
        TransitRouteStop fromStop = null;
        for (TransitRouteStop toStop : route.getStops()) {
            Link link = getNetworkLink(fromStop, toStop);
            routeLinks.add(link.getId());
            fromStop = toStop;
        }
        if (routeLinks.size() > 0) {
            route.setRoute(RouteUtils.createNetworkRoute(routeLinks, this.network));
        } else {
            System.err.println("Transit route " + route.getId() + " has less than two stops. No route assigned.");
        }
    }

    protected Link getNetworkLink(TransitRouteStop fromStop, TransitRouteStop toStop) {
        TransitStopFacility fromFacility = (fromStop == null) ? toStop.getStopFacility() : fromStop.getStopFacility();
        TransitStopFacility toFacility = toStop.getStopFacility();

        Node fromNode = this.nodes.get(fromFacility);
        if (fromNode == null) {
			if (fromFacility.getLinkId() != null) {
				fromNode = this.network.getLinks().get(fromFacility.getLinkId()).getToNode();
			} else {
				fromNode = this.network.getFactory().createNode(Id.create(this.prefix + fromFacility.getId(), Node.class), fromFacility.getCoord());
				this.network.addNode(fromNode);
			}
            this.nodes.put(fromFacility, fromNode);
        }

        Node toNode = this.nodes.get(toFacility);
        if (toNode == null) {
			if (toFacility.getLinkId() != null) {
				toNode = this.network.getLinks().get(toFacility.getLinkId()).getFromNode();
			} else {
				toNode = this.network.getFactory().createNode(Id.create(this.prefix + toFacility.getId(), Node.class), toFacility.getCoord());
				this.network.addNode(toNode);
			}
            this.nodes.put(toFacility, toNode);
        }

        Tuple<Node, Node> connection = new Tuple<Node, Node>(fromNode, toNode);
        Link link = this.links.get(connection);
        if (link == null) {
            link = createAndAddLink(fromNode, toNode, connection);

            if (toFacility.getLinkId() == null) {
                toFacility.setLinkId(link.getId());
                this.stopFacilities.put(connection, toFacility);
            } else {
                List<TransitStopFacility> copies = this.facilityCopies.get(toFacility);
                if (copies == null) {
                    copies = new ArrayList<TransitStopFacility>();
                    this.facilityCopies.put(toFacility, copies);
                }
                Id<TransitStopFacility> newId = Id.create(toFacility.getId().toString() + "." + Integer.toString(copies.size() + 1), TransitStopFacility.class);
                TransitStopFacility newFacility = this.schedule.getFactory().createTransitStopFacility(newId, toFacility.getCoord(), toFacility.getIsBlockingLane());
                newFacility.setStopPostAreaId(toFacility.getId().toString());
                newFacility.setLinkId(link.getId());
                newFacility.setName(toFacility.getName());
                copies.add(newFacility);
                this.nodes.put(newFacility, toNode);
                this.schedule.addStopFacility(newFacility);
                toStop.setStopFacility(newFacility);
                this.stopFacilities.put(connection, newFacility);
            }
        } else {
            toStop.setStopFacility(this.stopFacilities.get(connection));
        }
        return link;
    }

    private Link createAndAddLink(Node fromNode, Node toNode, Tuple<Node, Node> connection) {
        Link link = this.network.getFactory().createLink(Id.create(this.prefix + this.linkIdCounter++, Link.class), fromNode, toNode);
        if (fromNode == toNode) {
            link.setLength(50);
        } else {
            link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
        }
        // TODO-boescpa Identify transport mean and set speeds and link characteristics accordingly...
        link.setFreespeed(30.0 / 3.6);
        link.setCapacity(500);
        link.setNumberOfLanes(1);
        this.network.addLink(link);
        link.setAllowedModes(this.modes);
        this.links.put(connection, link);
        return link;
    }
}
