/* *********************************************************************** *
 * project: org.matsim.*
 * InVehWaitHandler.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author Ihab
 *
 */
public class PtLoadHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, TransitDriverStartsEventHandler {
	
	private final Map <Id, Id> busId2currentFacilityId = new HashMap<Id, Id>();
	private final Map <Id, Id> busId2currentRoute = new HashMap<Id, Id>();
	private int passengers = 0;
	
	private final SortedMap <Id, RouteInfo> routeId2RouteInfo = new TreeMap<Id, RouteInfo>();
	
	private final TransitSchedule schedule;
	
	public PtLoadHandler(TransitSchedule schedule) {
		
		this.schedule = schedule;
		
		for (TransitLine line : this.schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				
				RouteInfo routeInfo = new RouteInfo(route.getId());
				SortedMap<Id, FacilityLoadInfo> id2FacilityLoadInfo = new TreeMap<Id, FacilityLoadInfo>();

				for (TransitRouteStop stop : route.getStops()){
					id2FacilityLoadInfo.put(stop.getStopFacility().getId(), new FacilityLoadInfo(stop.getStopFacility().getId()));
				}
				routeInfo.setTransitStopId2FacilityLoadInfo(id2FacilityLoadInfo);
				this.routeId2RouteInfo.put(route.getId(), routeInfo);
			}
		}
	}

	@Override
	public void reset(int iteration) {
		busId2currentFacilityId.clear();
		routeId2RouteInfo.clear();
		busId2currentRoute.clear();
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id personId = event.getPersonId();
		Id vehId = event.getVehicleId();
		
		if (personId.toString().contains("person") && vehId.toString().contains("bus")){
			
			Id stopId = this.busId2currentFacilityId.get(vehId);
			double daytime = event.getTime();
			Id routeId = this.busId2currentRoute.get(vehId);
			
			this.routeId2RouteInfo.get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).getPersonEntering().add(daytime);
			this.passengers++;
					
		} else {
			// no person enters a bus
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id busId = event.getVehicleId();
		Id facilityId = event.getFacilityId();
//		System.out.println("Bus " + busId + " arrives at " + facilityId + ".");
		this.busId2currentFacilityId.put(busId, facilityId);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id personId = event.getPersonId();
		Id vehId = event.getVehicleId();
		
		if (personId.toString().contains("person") && vehId.toString().contains("bus")){
			
			Id stopId = this.busId2currentFacilityId.get(vehId);
			double daytime = event.getTime();
			Id routeId = this.busId2currentRoute.get(vehId);
			
			this.routeId2RouteInfo.get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).getPersonLeaving().add(daytime);
			this.passengers--;
			
		} else {
			// no person leaves a bus
		}
		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		Id busId = event.getVehicleId();
		Id routeId = event.getTransitRouteId();
		this.busId2currentRoute.put(busId, routeId);
	}

	public SortedMap<Id, RouteInfo> getRouteId2RouteInfo() {
		return routeId2RouteInfo;
	}
	
}
