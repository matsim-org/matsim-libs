/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.wagonSim.shunting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author balmermi
 * @since 2013-07-08
 */
public class ShuntingTableToMATSimScheduleEnricher {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	private final Scenario scenario;
	private final ObjectAttributes vehicleAttributes;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ShuntingTableToMATSimScheduleEnricher(Scenario scenario, ObjectAttributes vehicleAttributes) {
		this.scenario = scenario;
		this.vehicleAttributes = vehicleAttributes;
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public final void enrich(Map<Id<TransitLine>,Map<Id<Node>, Boolean>> shuntingTable, double minDwellTime) {
		
		NetworkFactory factory = scenario.getNetwork().getFactory();
		Set<String> transitModes = Collections.singleton(TransportMode.pt);
		
		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {

			Map<Id<Node>, Boolean> shuntingInfoMap = shuntingTable.get(transitLine.getId());
			List<TransitRoute> newTransitRoutes = new ArrayList<TransitRoute>();
			
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				
				List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
				routeLinkIds.add(transitRoute.getRoute().getStartLinkId());
				routeLinkIds.addAll(transitRoute.getRoute().getLinkIds());
				routeLinkIds.add(transitRoute.getRoute().getEndLinkId());
				
				List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
				transitRouteStops.add(transitRoute.getStops().get(0));

				for (int i=1; i<transitRoute.getStops().size()-1; i++) {
					TransitRouteStop stop = transitRoute.getStops().get(i);
					
					boolean replaceShuntingWithDwelling = false;
					
					double dwellTime = stop.getDepartureOffset()-stop.getArrivalOffset();
					if (dwellTime < 1.0) { dwellTime = 1.0; }

					if (shuntingInfoMap != null) {
						Boolean shuntingAllowed = shuntingInfoMap.get(Id.create(stop.getStopFacility().getId(), Node.class));
						if (shuntingAllowed != null) {
							if (!shuntingAllowed) {
								replaceShuntingWithDwelling = true;
							}
						}
						else if (dwellTime < minDwellTime) {
							replaceShuntingWithDwelling = true;
						}
					}
					else if (dwellTime < minDwellTime) {
						replaceShuntingWithDwelling = true;
					}
					
					if (replaceShuntingWithDwelling) {
						Link dwellLink = scenario.getNetwork().getLinks().get(Id.create(stop.getStopFacility().getId(), Link.class));
						if (dwellLink == null) {
							Node node = scenario.getNetwork().getLinks().get(stop.getStopFacility().getLinkId()).getToNode();
							dwellLink = factory.createLink(Id.create(stop.getStopFacility().getId(), Link.class), node, node);
							dwellLink.setLength(WagonSimConstants.DEFAULT_LENGTH_LOOPLINK);
							dwellLink.setFreespeed(WagonSimConstants.DEFAULT_FREESPEED);
							dwellLink.setCapacity(WagonSimConstants.DEFAULT_CAPACITY);
							dwellLink.setNumberOfLanes(WagonSimConstants.DEFAULT_NUMLANES);
							dwellLink.setAllowedModes(transitModes);
							scenario.getNetwork().addLink(dwellLink);
						}

						boolean found = false;
						for (int j=0; j<routeLinkIds.size(); j++) {
							Id<Link> linkId = routeLinkIds.get(j);
							if (linkId.equals(stop.getStopFacility().getLinkId())) {
								routeLinkIds.add(j+1,dwellLink.getId());
								found = true;
								break;
							}
						}
						if (!found) { throw new RuntimeException("Transit route stops and network route does not fit together. bailing out."); }
						
						for (Departure departure : transitRoute.getDepartures().values()) {
							double speed = dwellLink.getLength() / dwellTime;
							this.vehicleAttributes.putAttribute(departure.getVehicleId().toString(),dwellLink.getId().toString(),speed);
						}
					}
					else {
						transitRouteStops.add(stop);
					}
				}
				transitRouteStops.add(transitRoute.getStops().get(transitRoute.getStops().size()-1));
				
				NetworkRoute networkRoute = RouteUtils.createNetworkRoute(routeLinkIds,scenario.getNetwork());
				TransitRoute newTransitRoute = scenario.getTransitSchedule().getFactory().createTransitRoute(Id.create(transitLine.getId(), TransitRoute.class), networkRoute,transitRouteStops,TransportMode.pt);
				for (Departure departure : transitRoute.getDepartures().values()) { newTransitRoute.addDeparture(departure); }
				newTransitRoutes.add(newTransitRoute);
			}

			Collection<TransitRoute> transitRoutes = transitLine.getRoutes().values();
			for (TransitRoute transitRoute : transitRoutes) { transitLine.removeRoute(transitRoute); }
			for (TransitRoute transitRoute : newTransitRoutes) { transitLine.addRoute(transitRoute); }
		}
	}
}
