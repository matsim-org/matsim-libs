/* *********************************************************************** *
 * project: org.matsim.*
 * BusCorridorScheduleVehiclesGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.busCorridor.version4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
 * @author Ihab
 *
 */
public class ScheduleVehiclesGenerator {
			
	private double stopTime;
	private String networkFile;
	private String scheduleFile;
	private String vehicleFile;
	private Id transitLineId;
	private Id routeId1;
	private Id routeId2;
	private Id vehTypeId;
	private int numberOfBusses;
	private double startTime;
	private double endTime;
	private int busSeats;
	private int standingRoom;
	
	List<Id> vehicleIDs = new ArrayList<Id>();
	
	TransitScheduleFactory sf = new TransitScheduleFactoryImpl();
	private TransitSchedule schedule = sf.createTransitSchedule();
	Vehicles veh = VehicleUtils.createVehiclesContainer();

	public void createSchedule() throws IOException {
			
		Scenario scen = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		Config config = scen.getConfig();
		config.network().setInputFile(this.networkFile);
		ScenarioUtils.loadScenario(scen);		
		Network network = scen.getNetwork();
			
		scen.getConfig().scenario().setUseTransit(true);
		scen.getConfig().scenario().setUseVehicles(true);
			
		Map<Id,List<Id>> routeID2linkIDs = getIDs(network);
		
		Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities = getStopLinkIDs(routeID2linkIDs, network);
			
		Map<Id, NetworkRoute> routeId2networkRoute = getRouteId2NetworkRoute(routeID2linkIDs);
		Map<Id, List<TransitRouteStop>> routeId2TransitRouteStops = getRouteId2TransitRouteStops(routeId2transitStopFacilities, network, this.stopTime);
			
		Map<Id, TransitRoute> routeId2transitRoute = getRouteId2TransitRoute(routeId2networkRoute, routeId2TransitRouteStops);
		setTransitLine(routeId2transitRoute);
		setDepartureIDs(routeId2transitRoute);
		}
		
	private Map<Id,List<Id>> getIDs(Network network) {
		Map<Id, List<Id>> routeID2linkIDs = new HashMap<Id, List<Id>>();
		List<Id> linkIDsRoute1 = new ArrayList<Id>();
		List<Id> linkIDsRoute2 = new ArrayList<Id>();
			
		for (Link link : network.getLinks().values()){
			if (Integer.parseInt(link.getFromNode().getId().toString()) < Integer.parseInt(link.getToNode().getId().toString())){			
				// one direction
				linkIDsRoute1.add(link.getId());
			}
			else {
				// other direction
				linkIDsRoute2.add(link.getId());
			}
		}
			
		linkIDsRoute1.remove(linkIDsRoute1.size()-1);
		List<Id> linkIDsRoute2rightOrder = turnArround(linkIDsRoute2);
		routeID2linkIDs.put(routeId1, linkIDsRoute1);
		routeID2linkIDs.put(routeId2, linkIDsRoute2rightOrder);
		return routeID2linkIDs;
	}

	private Map<Id,List<TransitStopFacility>> getStopLinkIDs(Map<Id, List<Id>> routeID2linkIDs, Network network) {
		Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities = new HashMap<Id, List<TransitStopFacility>>();
			
		for (Id routeID : routeID2linkIDs.keySet()){
			List<TransitStopFacility> stopFacilitiesRoute = new ArrayList<TransitStopFacility>();

			for (Id linkID : routeID2linkIDs.get(routeID)){
				TransitStopFacility transitStopFacility = sf.createTransitStopFacility(linkID, network.getLinks().get(linkID).getToNode().getCoord(), true);
				transitStopFacility.setLinkId(linkID);
				stopFacilitiesRoute.add(transitStopFacility);
				schedule.addStopFacility(transitStopFacility);
			}	
			routeId2transitStopFacilities.put(routeID, stopFacilitiesRoute);
		}
		return routeId2transitStopFacilities;
	}
	
	private Map<Id, NetworkRoute> getRouteId2NetworkRoute(Map<Id, List<Id>> routeID2linkIDs) {
		Map<Id, NetworkRoute> routeId2NetworkRoute = new HashMap<Id, NetworkRoute>();
		for (Id routeId : routeID2linkIDs.keySet()){
			NetworkRoute netRoute = new LinkNetworkRouteImpl(routeID2linkIDs.get(routeId).get(0), routeID2linkIDs.get(routeId).get(routeID2linkIDs.get(routeId).size()-1));	// Start-Link, End-Link	
			netRoute.setLinkIds(routeID2linkIDs.get(routeId).get(0), getMiddleRouteLinkIDs(routeID2linkIDs.get(routeId)), routeID2linkIDs.get(routeId).get(routeID2linkIDs.get(routeId).size()-1)); // Start-link, link-Ids als List, End-link
			routeId2NetworkRoute.put(routeId, netRoute);
		}
		return routeId2NetworkRoute;
	}

	private Map<Id, List<TransitRouteStop>> getRouteId2TransitRouteStops(Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities, Network network, double stopTime) {

		Map<Id, List<TransitRouteStop>> routeId2transitRouteStops = new HashMap<Id, List<TransitRouteStop>>();
		for (Id routeId : routeId2transitStopFacilities.keySet()){
			double arrivalTime = 0;
			double departureTime = 0;
			List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
			List<TransitStopFacility> transitStopFacilities = routeId2transitStopFacilities.get(routeId);

			int ii = 1;
			for (TransitStopFacility transitStopFacility : transitStopFacilities){
				double travelTimeBus = 0;
				TransitRouteStop transitRouteStop = sf.createTransitRouteStop(transitStopFacility, arrivalTime, departureTime);
				transitRouteStop.setAwaitDepartureTime(true);
				transitRouteStops.add(transitRouteStop);					
				
				if (ii==transitStopFacilities.size()){
				}
				
				else {
					travelTimeBus = network.getLinks().get(transitStopFacilities.get(ii).getId()).getLength() / network.getLinks().get(transitStopFacilities.get(ii).getId()).getFreespeed(); // v = s/t --> t = s/v
				}
				arrivalTime = departureTime + travelTimeBus;
				departureTime = arrivalTime + stopTime;	
				
				ii++;
			}
		routeId2transitRouteStops.put(routeId, transitRouteStops);
		}
		return routeId2transitRouteStops;
	}
	
	private Map<Id, TransitRoute> getRouteId2TransitRoute(Map<Id, NetworkRoute> routeId2networkRoute, Map<Id, List<TransitRouteStop>> routeId2TransitRouteStops) {
		
		Map<Id, TransitRoute> routeId2transitRoute = new HashMap<Id, TransitRoute>();			
		for (Id routeId : routeId2networkRoute.keySet()){
			TransitRoute transitRoute = sf.createTransitRoute(routeId, routeId2networkRoute.get(routeId), routeId2TransitRouteStops.get(routeId), "bus");
			routeId2transitRoute.put(routeId, transitRoute);
		}
		return routeId2transitRoute;
	}
	
	private void setTransitLine(Map<Id, TransitRoute> routeId2transitRoute) {
		TransitLine transitLine = sf.createTransitLine(this.transitLineId);

		if (this.numberOfBusses==0) {
			// no schedule added
		}
		else {
			schedule.addTransitLine(transitLine);
		}
		
		transitLine.addRoute(routeId2transitRoute.get(this.routeId1));
		transitLine.addRoute(routeId2transitRoute.get(this.routeId2));
	}
	
	private void setDepartureIDs(Map<Id, TransitRoute> routeId2transitRoute) {	
		double routeNr = 0; 
		double serviceTime = this.endTime - this.startTime; //sec
		int lastStop = routeId2transitRoute.get(routeId1).getStops().size()-1;
		double routeTravelTime = routeId2transitRoute.get(routeId1).getStops().get(lastStop).getArrivalOffset();
		int numberOfDepartures = (int) (this.numberOfBusses * (serviceTime / (routeTravelTime * 2)));
		System.out.println("Anzahl an Fahrten pro Tag: "+numberOfDepartures);

		double takt = serviceTime / numberOfDepartures; //sec
		System.out.println("Takt: "+Time.writeTime(takt, Time.TIMEFORMAT_HHMMSS));
		
		for (Id routeId : routeId2transitRoute.keySet()){
			double firstDepartureTime = this.startTime;
			double departureTime = firstDepartureTime + routeNr;
			routeNr = routeNr + (takt/2);
			int vehicleIndex = 0;
			for (int depNr=1 ; depNr<=numberOfDepartures ; depNr++){
				Departure departure = sf.createDeparture(new IdImpl(depNr), departureTime);
				departure.setVehicleId(vehicleIDs.get(vehicleIndex));
				routeId2transitRoute.get(routeId).addDeparture(departure);
				departureTime = departureTime+takt;
				if (vehicleIndex==this.numberOfBusses-1){
					vehicleIndex = 0;
				}
				else {
					vehicleIndex++;
				}
			}				
		}			
	}
	
	private List<Id> turnArround(List<Id> myList) {
		List<Id> turnedArroundList = new ArrayList<Id>();
		for (int n=(myList.size()-1); n>0; n=n-1){
			turnedArroundList.add(myList.get(n));
		}
		return turnedArroundList;
	}

	public void createVehicles() {
		// Vehicle-Typ: Bus
		VehicleType type = veh.getFactory().createVehicleType(this.vehTypeId);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(this.busSeats);
		cap.setStandingRoom(this.standingRoom);
		type.setCapacity(cap);
		veh.getVehicleTypes().put(this.vehTypeId, type); 
		
		for (int vehicleNr=1 ; vehicleNr<=numberOfBusses ; vehicleNr++){
			vehicleIDs.add(new IdImpl("bus_"+vehicleNr));
		}

		// Vehicles
		// dummyVehicle if no buses
		if (vehicleIDs.isEmpty()){
			Vehicle vehicle = veh.getFactory().createVehicle(new IdImpl("bus_xxx"), veh.getVehicleTypes().get(vehTypeId));
			veh.getVehicles().put(new IdImpl("bus_xxx"), vehicle);
		}
		
		else {
			for (Id vehicleId : vehicleIDs){
				Vehicle vehicle = veh.getFactory().createVehicle(vehicleId, veh.getVehicleTypes().get(vehTypeId));
				veh.getVehicles().put(vehicleId, vehicle);
			}
		}
	}
	
	public void writeScheduleFile() {
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(schedule);
		scheduleWriter.write(scheduleFile);
	}
	
	public void writeVehicleFile() {
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(veh);
		vehicleWriter.writeFile(vehicleFile);
	}

	private List<Id> getMiddleRouteLinkIDs(List<Id> linkIDsRoute) {
		List<Id> routeLinkIDs = new ArrayList<Id>();
		int nr = 0;
		for(Id id : linkIDsRoute){
			if (nr>=1 & nr <= (linkIDsRoute.size()-2)){ // die Links zwischen dem Start- und Endlink
				routeLinkIDs.add(id);
			}
			nr++;
		}
		return routeLinkIDs;
	}

	public void setStopTime(double stopTime) {
		this.stopTime = stopTime;
	}

	public void setNetworkFile(String networkFile) {
		this.networkFile = networkFile;
	}

	public void setVehicleFile(String vehicleFile) {
		this.vehicleFile = vehicleFile;
	}

	public void setScheduleFile(String scheduleFile) {
		this.scheduleFile = scheduleFile;
	}
	
	public void setSeats(int seats) {
		this.busSeats = seats;
	}

	public void setTransitLineId(Id transitLineId) {
		this.transitLineId = transitLineId;
	}

	public void setRouteId1(Id routeId1) {
		this.routeId1 = routeId1;
	}

	public void setRouteId2(Id routeId2) {
		this.routeId2 = routeId2;
	}

	public void setVehTypeId(Id vehTypeId) {
		this.vehTypeId = vehTypeId;
	}

	public void setStandingRoom(int standingRoom) {
		this.standingRoom = standingRoom;
	}

	public void setNumberOfBusses(int numberOfBusses) {
		this.numberOfBusses = numberOfBusses;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}		
}
