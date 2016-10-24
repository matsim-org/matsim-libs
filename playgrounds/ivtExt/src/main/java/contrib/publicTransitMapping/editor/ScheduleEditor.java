/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package contrib.publicTransitMapping.editor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.IOException;

/**
 * Interface that defines the possible commands to edit
 * a schedule via csv command file.
 *
 * @author polettif
 */
public interface ScheduleEditor {

	/**
	 * Parses and executes the given csv file
	 */
	void parseCommandCsv(String csvFile) throws IOException;

	/**
	 * Executes one (csv) line
	 */
	void executeCmdLine(String[] cmd);

	/**
	 * Refreshes the whole schedule by routing all transit routes.
	 */
	void refreshSchedule();

	/**
	 * Reroutes the section after fromRouteStop via the given viaLinkId
	 * @param transitRoute  the transit route
	 * @param fromRouteStop the section of the route from this routeStop to the subsequent
	 *                      routeStop is rerouted
	 * @param viaLinkId		the section is routed via this link
	 */
	void rerouteFromStop(TransitRoute transitRoute, TransitRouteStop fromRouteStop, Id<Link> viaLinkId);

	/**
	 * Reroutes the section between two stops that passes the oldlink via the new link
	 * @param transitRoute the transit route
	 * @param oldLinkId the section between two route stops where this link appears is rerouted
	 * @param newLinkId the section is routed via this link
	 */
	void rerouteViaLink(TransitRoute transitRoute, Id<Link> oldLinkId, Id<Link> newLinkId);

	/**
	 * Changes the referenced link of a stop facility for all routes. A new child stop
	 * facility with the reference link is created if it does not exist already.
	 *
	 * @param stopFacilityId the stop facility which should be changed
	 * @param newRefLinkId the new reference link id
	 */
	void changeRefLink(Id<TransitStopFacility> stopFacilityId, Id<Link> newRefLinkId);

	/**
	 * Adds a link to the network. Uses the attributes (freespeed, nr of lanes, transportModes)
	 * of the attributeLink.
	 *
	 * @param newLinkId 		id of the new link
	 * @param fromNodeId 		from node id
	 * @param toNodeId			to node id
	 * @param attributeLinkId	the attributs of this link are copied, default values are used if <tt>null</tt>.
	 */
	void addLink(Id<Link> newLinkId, Id<Node> fromNodeId, Id<Node> toNodeId, Id<Link> attributeLinkId);

	/**
	 * Reroutes between all referenced stop links for the given transit route.
	 *
	 * @param transitRoute the transit route
	 */
	void refreshTransitRoute(TransitRoute transitRoute);
}