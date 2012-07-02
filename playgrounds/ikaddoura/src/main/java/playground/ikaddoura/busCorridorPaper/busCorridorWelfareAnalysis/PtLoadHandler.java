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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	
	
	private final TransitSchedule schedule;
	private List<AnalysisPeriod> analysisPeriods = new ArrayList<AnalysisPeriod>();

	public PtLoadHandler(TransitSchedule schedule) {
		
		this.schedule = schedule;
		
		AnalysisPeriod period1 = new AnalysisPeriod(4.*3600, 6.*3600);
		analysisPeriods.add(period1);
		AnalysisPeriod period2 = new AnalysisPeriod(6.*3600, 8.*3600);
		analysisPeriods.add(period2);
		AnalysisPeriod period2a = new AnalysisPeriod(8.*3600, 10.*3600);
		analysisPeriods.add(period2a);
		AnalysisPeriod period3 = new AnalysisPeriod(10.*3600, 12.*3600);
		analysisPeriods.add(period3);
		AnalysisPeriod period4 = new AnalysisPeriod(12.*3600, 14.*3600);
		analysisPeriods.add(period4);
		AnalysisPeriod period5 = new AnalysisPeriod(14.*3600, 16.*3600);
		analysisPeriods.add(period5);
		AnalysisPeriod period6 = new AnalysisPeriod(16.*3600, 18.*3600);
		analysisPeriods.add(period6);
		AnalysisPeriod period7 = new AnalysisPeriod(18.*3600, 20.*3600);
		analysisPeriods.add(period7);
		AnalysisPeriod period8 = new AnalysisPeriod(20.*3600, 22.*3600);
		analysisPeriods.add(period8);
		AnalysisPeriod period9 = new AnalysisPeriod(22.*3600, 24.*3600);
		analysisPeriods.add(period9);
		
		for (AnalysisPeriod period : analysisPeriods){
			for (TransitLine line : this.schedule.getTransitLines().values()){
				SortedMap <Id, RouteInfo> routeId2RouteInfo = new TreeMap<Id, RouteInfo>();
				
				for (TransitRoute route : line.getRoutes().values()){
					
					RouteInfo routeInfo = new RouteInfo(route.getId());
					SortedMap<Id, FacilityLoadInfo> id2FacilityLoadInfo = new TreeMap<Id, FacilityLoadInfo>();
	
					for (TransitRouteStop stop : route.getStops()){
						id2FacilityLoadInfo.put(stop.getStopFacility().getId(), new FacilityLoadInfo(stop.getStopFacility().getId()));
					}
					routeInfo.setTransitStopId2FacilityLoadInfo(id2FacilityLoadInfo);
					routeId2RouteInfo.put(route.getId(), routeInfo);
					period.setRouteId2RouteInfo(routeId2RouteInfo);
				}
			}
		}
	}

	@Override
	public void reset(int iteration) {
		busId2currentFacilityId.clear();
		analysisPeriods.clear();
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
			
			for (AnalysisPeriod period : this.analysisPeriods){
				if (daytime < period.getEnd() && daytime >= period.getStart()) {
					int entering = period.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).getPersonEntering();
					period.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).setPersonEntering(entering + 1);
					int nrEnteringAllStops = period.getEntering();
					period.setEntering(nrEnteringAllStops + 1);
				}
			}
					
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
			
			for (AnalysisPeriod period : this.analysisPeriods){
				if (daytime < period.getEnd() && daytime >= period.getStart()) {
					int leaving = period.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).getPersonLeaving();
					period.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).setPersonLeaving(leaving + 1);
					int nrLeavingAllStops = period.getLeaving();
					period.setLeaving(nrLeavingAllStops + 1);
				}
			}
			
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

	public List<AnalysisPeriod> getAnalysisPeriods() {
		return analysisPeriods;
	}
		
}
