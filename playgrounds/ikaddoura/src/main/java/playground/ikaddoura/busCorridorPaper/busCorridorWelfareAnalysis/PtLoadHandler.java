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
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author Ihab
 *
 */
public class PtLoadHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, TransitDriverStartsEventHandler {
	
	private final Map <Id, Id> busId2currentFacilityId = new HashMap<Id, Id>();
	private final Map <Id, Id> busId2currentRoute = new HashMap<Id, Id>();

	private final TransitSchedule schedule;
	private SortedMap<Integer, AnalysisPeriod> analysisPeriods = new TreeMap<Integer, AnalysisPeriod>();
	
	public PtLoadHandler(TransitSchedule schedule) {
		
		this.schedule = schedule;
		
		AnalysisPeriod period1 = new AnalysisPeriod(4.*3600, 6.*3600);
		analysisPeriods.put(1, period1);
		AnalysisPeriod period2 = new AnalysisPeriod(6.*3600, 8.*3600);
		analysisPeriods.put(2, period2);
		AnalysisPeriod period3 = new AnalysisPeriod(8.*3600, 10.*3600);
		analysisPeriods.put(3, period3);
		AnalysisPeriod period4 = new AnalysisPeriod(10.*3600, 12.*3600);
		analysisPeriods.put(4, period4);
		AnalysisPeriod period5 = new AnalysisPeriod(12.*3600, 14.*3600);
		analysisPeriods.put(5, period5);
		AnalysisPeriod period6 = new AnalysisPeriod(14.*3600, 16.*3600);
		analysisPeriods.put(6, period6);
		AnalysisPeriod period7 = new AnalysisPeriod(16.*3600, 18.*3600);
		analysisPeriods.put(7, period7);
		AnalysisPeriod period8 = new AnalysisPeriod(18.*3600, 20.*3600);
		analysisPeriods.put(8, period8);
		AnalysisPeriod period9 = new AnalysisPeriod(20.*3600, 22.*3600);
		analysisPeriods.put(9, period9);
		AnalysisPeriod period10 = new AnalysisPeriod(22.*3600, 24.*3600);
		analysisPeriods.put(10, period10);
		
		for (AnalysisPeriod period : analysisPeriods.values()){
			for (TransitLine line : this.schedule.getTransitLines().values()){
				SortedMap <Id, RouteInfo> routeId2RouteInfo = new TreeMap<Id, RouteInfo>();
				
				for (TransitRoute route : line.getRoutes().values()){
					
					RouteInfo routeInfo = new RouteInfo(route.getId());
					Map<Id, FacilityLoadInfo> id2FacilityLoadInfo = new HashMap<Id, FacilityLoadInfo>();
	
					List<Id> stopIDs = new ArrayList<Id>();
					for (TransitRouteStop stop : route.getStops()){
						id2FacilityLoadInfo.put(stop.getStopFacility().getId(), new FacilityLoadInfo(stop.getStopFacility().getId()));
						stopIDs.add(stop.getStopFacility().getId());
					}
					
					routeInfo.setTransitStopId2FacilityLoadInfo(id2FacilityLoadInfo);
					routeInfo.setStopIDs(stopIDs);
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
			
			for (Integer periodNr : this.analysisPeriods.keySet()){
				AnalysisPeriod period = this.analysisPeriods.get(periodNr);
				if (daytime < period.getEnd() && daytime >= period.getStart()) {
					int entering = period.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).getPersonEntering();
					period.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).setPersonEntering(entering + 1);
					int enteringAllRoutesAllStops = period.getEntering();
					period.setEntering(enteringAllRoutesAllStops + 1);
					
					int passengersThisBus;
					if (period.getBusId2Passengers().get(vehId) == null) {
						// passengers from period before!
						passengersThisBus = this.analysisPeriods.get(periodNr-1).getBusId2Passengers().get(vehId);

					} else {
						// passengers from this period!
						passengersThisBus = this.analysisPeriods.get(periodNr).getBusId2Passengers().get(vehId);
					}
					period.getBusId2Passengers().put(vehId, passengersThisBus + 1);
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
			
			for (Integer periodNr : this.analysisPeriods.keySet()){
				AnalysisPeriod period = this.analysisPeriods.get(periodNr);
				if (daytime < period.getEnd() && daytime >= period.getStart()) {
					int leaving = period.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).getPersonLeaving();
					period.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).setPersonLeaving(leaving + 1);
					int leavingAllRoutesAllStops = period.getLeaving();
					period.setLeaving(leavingAllRoutesAllStops + 1);
					
					int passengersThisBus;
					if (period.getBusId2Passengers().get(vehId) == null) {
						// passengers from period before!
						passengersThisBus = this.analysisPeriods.get(periodNr-1).getBusId2Passengers().get(vehId);
					} else {
						// passengers from this period!
						passengersThisBus = this.analysisPeriods.get(periodNr).getBusId2Passengers().get(vehId);
					}
					period.getBusId2Passengers().put(vehId, passengersThisBus - 1);
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
		
		for (AnalysisPeriod period : this.analysisPeriods.values()){
			if (event.getTime() < period.getEnd() && event.getTime() >= period.getStart()) {
				period.getBusId2Passengers().put(busId, 0);		
			}
		}
	}

	public SortedMap<Integer, AnalysisPeriod> getAnalysisPeriods() {
		return analysisPeriods;
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {

//		je Period je Route je Bus für diese stopId die Anzahl der Passagiere berechnen!
		
		Id vehId = event.getVehicleId();
		Id stopId = this.busId2currentFacilityId.get(vehId);
		Id routeId = this.busId2currentRoute.get(vehId);
		double daytime = event.getTime();
		
		for (Integer periodNr : this.analysisPeriods.keySet()){
			AnalysisPeriod period = this.analysisPeriods.get(periodNr);
			if (daytime < period.getEnd() && daytime >= period.getStart()) {
				
				int passengersWhenThisBusIsDeparting;
				if (period.getBusId2Passengers().get(vehId) == null){
					// passengers from period before!
					if (this.analysisPeriods.get(periodNr-1).getBusId2Passengers().get(vehId) == null){
						passengersWhenThisBusIsDeparting = 0;
					} else {
						passengersWhenThisBusIsDeparting = this.analysisPeriods.get(periodNr-1).getBusId2Passengers().get(vehId);
					}
				} else {
					// passengers from this period!
					passengersWhenThisBusIsDeparting = period.getBusId2Passengers().get(vehId);
				}
				int passengersDepartingBefore = period.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).getPassengersWhenLeavingFacility();
				int passengersNew = passengersWhenThisBusIsDeparting + passengersDepartingBefore;
				period.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo().get(stopId).setPassengersWhenLeavingFacility(passengersNew);
								
			}
		}
	}
}
