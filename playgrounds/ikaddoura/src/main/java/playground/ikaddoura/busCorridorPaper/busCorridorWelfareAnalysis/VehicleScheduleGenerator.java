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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
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
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
 * @author Ihab
 *
 */
public class VehicleScheduleGenerator {
	private final static Logger log = Logger.getLogger(VehicleScheduleGenerator.class);
	
	private double stopTime;
	private Network network;
	private String scheduleFile;
	private String vehicleFile;
	private Id<TransitLine> transitLineId;
	private Id<TransitRoute> routeId1;
	private Id<TransitRoute> routeId2;
	private Id<VehicleType> vehTypeId;
	
	private int numberOfBuses;
	private double startTime;
	private double endTime;
	private double pausenzeit;
	private int busSeats;
	private int standingRoom;
	private double length;
	private double egressSeconds;
	private double accessSeconds;
	private double scheduleSpeed;
	private DoorOperationMode doorOperationMode;
	
	private double headway;
		
	List<Id<Vehicle>> vehicleIDs = new ArrayList<Id<Vehicle>>();
	
	TransitScheduleFactory sf = new TransitScheduleFactoryImpl();
	private final TransitSchedule schedule = sf.createTransitSchedule();
	
	Vehicles veh = VehicleUtils.createVehiclesContainer();

	public void createSchedule() throws IOException {
		
		Map<Id<TransitRoute>,List<Id<Link>>> routeID2linkIDs = getIDs();
		Map<Id<TransitRoute>, List<TransitStopFacility>> routeId2transitStopFacilities = getStopLinkIDs(routeID2linkIDs);
		Map<Id<TransitRoute>, NetworkRoute> routeId2networkRoute = getRouteId2NetworkRoute(routeID2linkIDs);
		Map<Id<TransitRoute>, List<TransitRouteStop>> routeId2TransitRouteStops = getRouteId2TransitRouteStops(routeId2transitStopFacilities);
		Map<Id<TransitRoute>, TransitRoute> routeId2transitRoute = getRouteId2TransitRoute(routeId2networkRoute, routeId2TransitRouteStops);
		setTransitLine(routeId2transitRoute);
		setDepartureIDs(routeId2transitRoute);
		
		}
		
	private Map<Id<TransitRoute>,List<Id<Link>>> getIDs() {
		List <Link> busLinks = new ArrayList<Link>();
		Map<Id<TransitRoute>, List<Id<Link>>> routeID2linkIDs = new HashMap<>();
		List<Id<Link>> linkIDsRoute1 = new LinkedList<Id<Link>>();
		List<Id<Link>> linkIDsRoute2 = new LinkedList<Id<Link>>();
		
		// take busLinks and put them in a Map
		for (Link link : this.network.getLinks().values()){
			if (link.getAllowedModes().contains("bus") && !link.getAllowedModes().contains("car")){
//			if (link.getAllowedModes().contains("bus")){
				busLinks.add(link);
			}
		}
		
		if (busLinks.isEmpty()) throw new RuntimeException("No bus links found. Link IDs have to contain [bus] in order to create the schedule. Aborting...");
		
		// one direction
		int fromNodeIdRoute1 = 0;
		int toNodeIdRoute1 = 0;
		for (int ii = 0; ii <= busLinks.size(); ii++){
			fromNodeIdRoute1 = ii;
			toNodeIdRoute1 = ii + 1;
			for (Link link : busLinks){
				if (Integer.parseInt(link.getFromNode().getId().toString()) == fromNodeIdRoute1 && Integer.parseInt(link.getToNode().getId().toString()) == toNodeIdRoute1){			
					linkIDsRoute1.add(link.getId());
				}
				else {
					// nothing
				}
			}
		}
		// other direction
		int fromNodeIdRoute2 = 0;
		int toNodeIdRoute2 = 0;
		for (int ii = 0; ii <= busLinks.size(); ii++){
			fromNodeIdRoute2 = ii;
			toNodeIdRoute2 = ii - 1;
			for (Link link : busLinks){
				if (Integer.parseInt(link.getFromNode().getId().toString())==fromNodeIdRoute2 && Integer.parseInt(link.getToNode().getId().toString())==toNodeIdRoute2){			
					linkIDsRoute2.add(link.getId());
				}
				else {
					// nothing
				}
			}
		}

		List<Id<Link>> linkIDsRoute2rightOrder = turnArround(linkIDsRoute2);

		linkIDsRoute1.add(0, linkIDsRoute2rightOrder.get(linkIDsRoute2rightOrder.size()-1));
		linkIDsRoute2rightOrder.add(0, linkIDsRoute1.get(linkIDsRoute1.size()-1));
		routeID2linkIDs.put(routeId1, linkIDsRoute1);
		routeID2linkIDs.put(routeId2, linkIDsRoute2rightOrder);
		return routeID2linkIDs;
	}

	private Map<Id<TransitRoute>,List<TransitStopFacility>> getStopLinkIDs(Map<Id<TransitRoute>, List<Id<Link>>> routeID2linkIDs) {
		Map<Id<TransitRoute>, List<TransitStopFacility>> routeId2transitStopFacilities = new HashMap<Id<TransitRoute>, List<TransitStopFacility>>();
			
		for (Id<TransitRoute> routeID : routeID2linkIDs.keySet()){
			List<TransitStopFacility> stopFacilitiesRoute = new ArrayList<TransitStopFacility>();

			for (Id<Link> linkID : routeID2linkIDs.get(routeID)){
				Id<TransitStopFacility> stopId = Id.create(linkID, TransitStopFacility.class);
				if (schedule.getFacilities().containsKey(stopId)){
					TransitStopFacility transitStopFacility = schedule.getFacilities().get(stopId);
					stopFacilitiesRoute.add(transitStopFacility);
				}
				else {
					TransitStopFacility transitStopFacility = sf.createTransitStopFacility(stopId, this.network.getLinks().get(linkID).getToNode().getCoord(), false);
					transitStopFacility.setLinkId(linkID);
					stopFacilitiesRoute.add(transitStopFacility);
					schedule.addStopFacility(transitStopFacility);
				}
			}	
			routeId2transitStopFacilities.put(routeID, stopFacilitiesRoute);
		}
		return routeId2transitStopFacilities;
	}
	
	private Map<Id<TransitRoute>, NetworkRoute> getRouteId2NetworkRoute(Map<Id<TransitRoute>, List<Id<Link>>> routeID2linkIDs) {
		Map<Id<TransitRoute>, NetworkRoute> routeId2NetworkRoute = new HashMap<Id<TransitRoute>, NetworkRoute>();
		for (Id<TransitRoute> routeId : routeID2linkIDs.keySet()){
			NetworkRoute netRoute = new LinkNetworkRouteImpl(routeID2linkIDs.get(routeId).get(0), routeID2linkIDs.get(routeId).get(routeID2linkIDs.get(routeId).size()-1));	// Start-Link, End-Link	
			netRoute.setLinkIds(routeID2linkIDs.get(routeId).get(0), getMiddleRouteLinkIDs(routeID2linkIDs.get(routeId)), routeID2linkIDs.get(routeId).get(routeID2linkIDs.get(routeId).size()-1)); // Start-link, link-Ids als List, End-link
			routeId2NetworkRoute.put(routeId, netRoute);
		}
		return routeId2NetworkRoute;
	}

	private Map<Id<TransitRoute>, List<TransitRouteStop>> getRouteId2TransitRouteStops(Map<Id<TransitRoute>, List<TransitStopFacility>> routeId2transitStopFacilities) {

		Map<Id<TransitRoute>, List<TransitRouteStop>> routeId2transitRouteStops = new HashMap<Id<TransitRoute>, List<TransitRouteStop>>();
		
		for (Id<TransitRoute> routeId : routeId2transitStopFacilities.keySet()){
			double arrivalTime = 0;
			double departureTime = 0;
			List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
			List<TransitStopFacility> transitStopFacilities = routeId2transitStopFacilities.get(routeId);

			int ii = 0;
			double travelTimeBus = 0;
			for (TransitStopFacility transitStopFacility : transitStopFacilities){
				
				TransitRouteStop transitRouteStop = sf.createTransitRouteStop(transitStopFacility, arrivalTime, departureTime);
				transitRouteStop.setAwaitDepartureTime(true);
				transitRouteStops.add(transitRouteStop);
				
				if (ii==transitStopFacilities.size()-1){
				} else {
					travelTimeBus = this.network.getLinks().get(transitStopFacilities.get(ii).getId()).getLength() / this.scheduleSpeed;
//					travelTimeBus = this.network.getLinks().get(transitStopFacilities.get(ii).getId()).getLength() / this.network.getLinks().get(transitStopFacilities.get(ii).getId()).getFreespeed();
				}
				
				arrivalTime = departureTime + travelTimeBus;
				departureTime = arrivalTime + this.stopTime;	
				ii++;
			}
		routeId2transitRouteStops.put(routeId, transitRouteStops);
		}
		return routeId2transitRouteStops;
	}

	
	private Map<Id<TransitRoute>, TransitRoute> getRouteId2TransitRoute(Map<Id<TransitRoute>, NetworkRoute> routeId2networkRoute, Map<Id<TransitRoute>, List<TransitRouteStop>> routeId2TransitRouteStops) {
		
		Map<Id<TransitRoute>, TransitRoute> routeId2transitRoute = new HashMap<>();			
		for (Id<TransitRoute> routeId : routeId2networkRoute.keySet()){
			TransitRoute transitRoute = sf.createTransitRoute(routeId, routeId2networkRoute.get(routeId), routeId2TransitRouteStops.get(routeId), "bus");
			routeId2transitRoute.put(routeId, transitRoute);
		}
		return routeId2transitRoute;
	}
	
	private void setTransitLine(Map<Id<TransitRoute>, TransitRoute> routeId2transitRoute) {
		TransitLine transitLine = sf.createTransitLine(this.transitLineId);

		if (this.numberOfBuses == 0) {
			throw new RuntimeException("At least 1 Bus expected. Aborting...");
		}
		else {
			schedule.addTransitLine(transitLine);
		}
		
		transitLine.addRoute(routeId2transitRoute.get(this.routeId1));
		transitLine.addRoute(routeId2transitRoute.get(this.routeId2));
	}
	
	private void setDepartureIDs(Map<Id<TransitRoute>, TransitRoute> routeId2transitRoute) {	
		
		int lastStop = routeId2transitRoute.get(routeId1).getStops().size()-1;
		double routeTravelTime = routeId2transitRoute.get(routeId1).getStops().get(lastStop).getArrivalOffset();
		log.info("RouteTravelTime: "+ Time.writeTime(routeTravelTime, Time.TIMEFORMAT_HHMMSS));
		double umlaufzeit = (routeTravelTime + this.pausenzeit) * 2.0;
		log.info("Umlaufzeit: "+ Time.writeTime(umlaufzeit, Time.TIMEFORMAT_HHMMSS));
		this.headway = umlaufzeit / this.numberOfBuses;
		log.info("Takt: "+ Time.writeTime(this.headway, Time.TIMEFORMAT_HHMMSS));
		
		int routeNr = 0;
		for (Id<TransitRoute> routeId : routeId2transitRoute.keySet()){
			double firstDepartureTime = 0.0;
			if (routeNr == 1){
				firstDepartureTime = this.startTime;
				log.info(routeId.toString() + ": Route 0 --> First Departure Time: "+ Time.writeTime(firstDepartureTime, Time.TIMEFORMAT_HHMMSS));
			}
			else if (routeNr == 0){
				firstDepartureTime = this.startTime + umlaufzeit/2;
				log.info(routeId.toString() + ": Route 1 --> First Departure Time: "+ Time.writeTime(firstDepartureTime, Time.TIMEFORMAT_HHMMSS));

			}
			int vehicleIndex = 0;
			int depNr = 0;
			for (double departureTime = firstDepartureTime; departureTime < this.endTime ; ){
				Departure departure = sf.createDeparture(Id.create(depNr, Departure.class), departureTime);
				departure.setVehicleId(vehicleIDs.get(vehicleIndex));
				routeId2transitRoute.get(routeId).addDeparture(departure);
				departureTime = departureTime + this.headway;
				depNr++;
				if (vehicleIndex == this.numberOfBuses - 1){
					vehicleIndex = 0;
				}
				else {
					vehicleIndex++;
				}
			}				
		routeNr++;
		}			
	}
	
	private List<Id<Link>> turnArround(List<Id<Link>> myList) {
		List<Id<Link>> turnedArroundList = new ArrayList<Id<Link>>();
		for (int n = (myList.size() - 1); n >= 0; n = n - 1){
			turnedArroundList.add(myList.get(n));
		}
		return turnedArroundList;
	}

	public void createVehicles() {
		
		VehicleType type = veh.getFactory().createVehicleType(this.vehTypeId);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(this.busSeats);
		cap.setStandingRoom(this.standingRoom);
		type.setCapacity(cap);
		type.setLength(length);
		type.setAccessTime(accessSeconds);
		type.setEgressTime(egressSeconds);
		type.setDoorOperationMode(doorOperationMode);
		
		veh.getVehicleTypes().put(this.vehTypeId, type); 
		
		for (int vehicleNr=1 ; vehicleNr<=numberOfBuses ; vehicleNr++){
			vehicleIDs.add(Id.create("bus_"+vehicleNr, Vehicle.class));
		}

		if (vehicleIDs.isEmpty()){
			throw new RuntimeException("At least 1 Bus is expected. Aborting...");
		} else {
			for (Id<Vehicle> vehicleId : vehicleIDs){
				Vehicle vehicle = veh.getFactory().createVehicle(vehicleId, veh.getVehicleTypes().get(vehTypeId));
				veh.addVehicle( vehicle);
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

	private List<Id<Link>> getMiddleRouteLinkIDs(List<Id<Link>> linkIDsRoute) {
		List<Id<Link>> routeLinkIDs = new ArrayList<Id<Link>>();
		int nr = 0;
		for(Id<Link> id : linkIDsRoute){
			if (nr >= 1 & nr <= (linkIDsRoute.size() - 2)){ // links between startLink and endLink
				routeLinkIDs.add(id);
			}
			nr++;
		}
		return routeLinkIDs;
	}

	public void setStopTime(double stopTime) {
		this.stopTime = stopTime;
	}

	public void setNetwork(Network network) {
		this.network = network;
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

	public void setTransitLineId(Id<TransitLine> transitLineId) {
		this.transitLineId = transitLineId;
	}

	public void setRouteId1(Id<TransitRoute> routeId1) {
		this.routeId1 = routeId1;
	}

	public void setRouteId2(Id<TransitRoute> routeId2) {
		this.routeId2 = routeId2;
	}

	public void setVehTypeId(Id<VehicleType> vehTypeId) {
		this.vehTypeId = vehTypeId;
	}

	public void setStandingRoom(int standingRoom) {
		this.standingRoom = standingRoom;
	}

	public void setNumberOfBuses(int numberOfBuses) {
		this.numberOfBuses = numberOfBuses;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public void setEgressSeconds(double egressSeconds) {
		this.egressSeconds = egressSeconds;
	}

	public void setAccessSeconds(double accessSeconds) {
		this.accessSeconds = accessSeconds;
	}

	public void setScheduleSpeed(double scheduleSpeed) {
		this.scheduleSpeed = scheduleSpeed;
	}

	public void setPausenzeit(double pausenzeit) {
		this.pausenzeit = pausenzeit;
	}

	public void setDoorOperationMode(DoorOperationMode doorOperationMode) {
		this.doorOperationMode = doorOperationMode;
	}

	public double getHeadway() {
		return headway;
	}
}