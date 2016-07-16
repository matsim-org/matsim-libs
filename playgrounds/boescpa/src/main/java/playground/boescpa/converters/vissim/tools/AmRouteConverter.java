/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim.tools;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.MatsimXmlParser;

/**
 * Provides a visum-anmroutes specific implementation of RouteConverter.
 *
 * @author boescpa
 */
public class AmRouteConverter extends AbstractRouteConverter {

	private int routesWithoutLinks = 0;
	private Network network = null;

	/**
	 * Parses the provided Visum-Anmroutes-File (xml-format) and transform the routes into a trips.
	 *
	 * @param path2AnmroutesFile	Path to a Visum-Anmroutes-File
	 * @param path2VissimNetworkAnm	Path to a Visum-Anm-Network-File
	 * @param notUsed
	 * @return
	 */
	@Override
	protected List<Trip> routes2Trips(String path2AnmroutesFile, String path2VissimNetworkAnm, String notUsed) {
		final List<Trip> trips = new ArrayList<Trip>();

		// Read anm-network
		final AmNetworkMapper amNetworkMapper = new AmNetworkMapper();
		this.network = amNetworkMapper.providePreparedNetwork(path2VissimNetworkAnm, "");

		// Read anmroutes-file:
		final List<SimpleAnmroutesParser.AnmRoute> routes = new ArrayList<SimpleAnmroutesParser.AnmRoute>();
		MatsimXmlParser xmlParser = new SimpleAnmroutesParser(new SimpleAnmroutesParser.AnmRouteHandler() {
			@Override
			public void handleRoute(SimpleAnmroutesParser.AnmRoute anmRoute) {
				routes.add(anmRoute);
			}
		});
		xmlParser.parse(path2AnmroutesFile);

		// create trips:
		for (SimpleAnmroutesParser.AnmRoute anmRoute : routes) {
			Trip trip = new Trip(Id.create(anmRoute.id, Trip.class), 0.0);
			boolean addTrip = true;
			for (int i = 0; i < anmRoute.nodes.size()-1; i++) { // the -1 from the size because we have one less links than nodes on a route.
				Id<Link> linkId = findLinkId(anmRoute.nodes.get(i), anmRoute.nodes.get(i + 1));
				if (linkId != null) {
					trip.links.add(linkId);
				} else {
					addTrip = false;
					break;
				}
			}
			if (addTrip) {
				trips.add(trip);
			}
		}

		System.out.print(routesWithoutLinks + " routes found (and dropped) with at least one link missing in network.\n");
		return trips;
	}

	private Id<Link> findLinkId(Id<Node> fromNode, Id<Node> toNode) {
		for (Link link : this.network.getLinks().values()) {
			if (link.getFromNode().getId().toString().equals(fromNode.toString())
					&& link.getToNode().getId().toString().equals(toNode.toString())) {
				return link.getId();
			}
		}
		routesWithoutLinks++;
		return null;
	}
}
