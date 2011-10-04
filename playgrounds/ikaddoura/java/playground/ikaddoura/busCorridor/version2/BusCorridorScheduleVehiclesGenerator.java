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
package playground.ikaddoura.busCorridor.version2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
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
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesImpl;

/**
 * @author Ihab
 *
 */
public class BusCorridorScheduleVehiclesGenerator {
			
	private double stopTime;
	private double travelTimeBus;
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
	
	List<Id> nodeIDsRoute1 = new ArrayList<Id>();
	List<Id> nodeIDsRoute2 = new ArrayList<Id>();
	List<Id> stopFacilityIDsRoute1 = new ArrayList<Id>();
	List<Id> stopFacilityIDsRoute2 = new ArrayList<Id>();
	List<Id> linkIDsRoute1 = new ArrayList<Id>();
	List<Id> linkIDsRoute2 = new ArrayList<Id>();
	
	List<Id> vehicleIDs = new ArrayList<Id>();
	
	Map<Id, List<Id>> routeId2linkIDs = new HashMap<Id, List<Id>>();
	Map<Id, List<TransitRouteStop>> routeId2TransitRouteStops = new HashMap<Id, List<TransitRouteStop>>();
	Map<Id, NetworkRoute> routeId2networkRoute = new HashMap<Id, NetworkRoute>();
	Map<Id, TransitRoute> routeId2transitRoute = new HashMap<Id, TransitRoute>();
	Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities = new HashMap<Id, List<TransitStopFacility>>();
	
	TransitScheduleFactory sf = new TransitScheduleFactoryImpl();
	private TransitSchedule schedule = sf.createTransitSchedule();

	Vehicles veh = new VehiclesImpl();

		public void createSchedule() throws IOException {
			
			Scenario scen = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
			Config config = scen.getConfig();
			config.network().setInputFile(this.networkFile);
			ScenarioUtils.loadScenario(scen);		
			Network network = scen.getNetwork();
			
			scen.getConfig().scenario().setUseTransit(true);
			scen.getConfig().scenario().setUseVehicles(true);
										
			getIDs();
			
			routeId2transitStopFacilities.put(this.routeId1, getTransitStopFacility(stopFacilityIDsRoute1, linkIDsRoute1, nodeIDsRoute1, network));
			routeId2transitStopFacilities.put(this.routeId2, getTransitStopFacility(stopFacilityIDsRoute2, linkIDsRoute2, nodeIDsRoute2, network));
			
			this.routeId2TransitRouteStops = getRouteId2TransitRouteStops(this.stopTime, this.travelTimeBus);
			this.routeId2networkRoute = getRouteId2NetworkRoute();
			this.routeId2transitRoute = getRouteId2TransitRoute();
		
			setDepartureIDs();
			setTransitLine();
		}
		
		private void getIDs() {
			
			nodeIDsRoute1.add(new IdImpl("2"));
			nodeIDsRoute1.add(new IdImpl("3"));
			nodeIDsRoute1.add(new IdImpl("4"));
			nodeIDsRoute1.add(new IdImpl("5"));
			nodeIDsRoute1.add(new IdImpl("6"));
			nodeIDsRoute1.add(new IdImpl("7"));
			nodeIDsRoute1.add(new IdImpl("8"));
			nodeIDsRoute1.add(new IdImpl("9"));
			nodeIDsRoute1.add(new IdImpl("10"));
			
			stopFacilityIDsRoute1.add(new IdImpl("1to2"));
			stopFacilityIDsRoute1.add(new IdImpl("2to3"));
			stopFacilityIDsRoute1.add(new IdImpl("3to4"));
			stopFacilityIDsRoute1.add(new IdImpl("4to5"));
			stopFacilityIDsRoute1.add(new IdImpl("5to6"));
			stopFacilityIDsRoute1.add(new IdImpl("6to7"));
			stopFacilityIDsRoute1.add(new IdImpl("7to8"));
			stopFacilityIDsRoute1.add(new IdImpl("8to9"));
			stopFacilityIDsRoute1.add(new IdImpl("9to10"));
			
			linkIDsRoute1.add(new IdImpl("1to2"));
			linkIDsRoute1.add(new IdImpl("2to3"));
			linkIDsRoute1.add(new IdImpl("3to4"));
			linkIDsRoute1.add(new IdImpl("4to5"));
			linkIDsRoute1.add(new IdImpl("5to6"));
			linkIDsRoute1.add(new IdImpl("6to7"));
			linkIDsRoute1.add(new IdImpl("7to8"));
			linkIDsRoute1.add(new IdImpl("8to9"));
			linkIDsRoute1.add(new IdImpl("9to10"));
			
			nodeIDsRoute2.add(new IdImpl("10"));
			nodeIDsRoute2.add(new IdImpl("9"));
			nodeIDsRoute2.add(new IdImpl("8"));
			nodeIDsRoute2.add(new IdImpl("7"));
			nodeIDsRoute2.add(new IdImpl("6"));
			nodeIDsRoute2.add(new IdImpl("5"));
			nodeIDsRoute2.add(new IdImpl("4"));
			nodeIDsRoute2.add(new IdImpl("3"));
			nodeIDsRoute2.add(new IdImpl("2"));

			stopFacilityIDsRoute2.add(new IdImpl("11to10"));
			stopFacilityIDsRoute2.add(new IdImpl("10to9"));
			stopFacilityIDsRoute2.add(new IdImpl("9to8"));
			stopFacilityIDsRoute2.add(new IdImpl("8to7"));
			stopFacilityIDsRoute2.add(new IdImpl("7to6"));
			stopFacilityIDsRoute2.add(new IdImpl("6to5"));
			stopFacilityIDsRoute2.add(new IdImpl("5to4"));
			stopFacilityIDsRoute2.add(new IdImpl("4to3"));
			stopFacilityIDsRoute2.add(new IdImpl("3to2"));
			
			linkIDsRoute2.add(new IdImpl("11to10"));
			linkIDsRoute2.add(new IdImpl("10to9"));
			linkIDsRoute2.add(new IdImpl("9to8"));
			linkIDsRoute2.add(new IdImpl("8to7"));
			linkIDsRoute2.add(new IdImpl("7to6"));
			linkIDsRoute2.add(new IdImpl("6to5"));
			linkIDsRoute2.add(new IdImpl("5to4"));
			linkIDsRoute2.add(new IdImpl("4to3"));
			linkIDsRoute2.add(new IdImpl("3to2"));
			
			routeId2linkIDs.put(this.routeId1,this.linkIDsRoute1);
			routeId2linkIDs.put(this.routeId2,this.linkIDsRoute2);
		}

		private void setTransitLine() {
			TransitLine transitLine = sf.createTransitLine(this.transitLineId);
			schedule.addTransitLine(transitLine);
			transitLine.addRoute(routeId2transitRoute.get(this.routeId1));
			transitLine.addRoute(routeId2transitRoute.get(this.routeId2));
		}

		public void createVehicles() {
			// Vehicle-Typ: Bus
			VehicleType type = veh.getFactory().createVehicleType(this.vehTypeId);
			VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
			cap.setSeats(this.busSeats);
			cap.setStandingRoom(this.standingRoom);
			type.setCapacity(cap);
			veh.getVehicleTypes().put(this.vehTypeId, type); 
			
			// Vehicle-IDs, Anzahl abhängig von Busanzahl, Startzeit, Endzeit, Routenfahrzeit
			for (int vehicleNr=1 ; vehicleNr<=numberOfBusses ; vehicleNr++){
				vehicleIDs.add(new IdImpl("bus_"+vehicleNr));
			}

			// Vehicles
			for (Id vehicleId : vehicleIDs){
				Vehicle vehicle = veh.getFactory().createVehicle(vehicleId, veh.getVehicleTypes().get(vehTypeId));
				veh.getVehicles().put(vehicleId, vehicle);
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

		private void setDepartureIDs() {	
			double routeNr = 0; 
			double serviceTime = this.endTime - this.startTime; //sec
			int lastStop = routeId2transitRoute.get(routeId1).getStops().size()-1;
			double routeTravelTime = this.routeId2transitRoute.get(routeId1).getStops().get(lastStop).getArrivalOffset();
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
		
		
		private Map<Id, TransitRoute> getRouteId2TransitRoute() {
			
			Map<Id, TransitRoute> routeId2transitRoute = new HashMap<Id, TransitRoute>();			
			for (Id routeId : this.routeId2networkRoute.keySet()){
				TransitRoute transitRoute = sf.createTransitRoute(routeId, this.routeId2networkRoute.get(routeId), routeId2TransitRouteStops.get(routeId), "bus");
				routeId2transitRoute.put(routeId, transitRoute);
			}
			return routeId2transitRoute;
		}

		private Map<Id, NetworkRoute> getRouteId2NetworkRoute() {
			Map<Id, NetworkRoute> routeId2NetworkRoute = new HashMap<Id, NetworkRoute>();
			for (Id routeId : routeId2linkIDs.keySet()){
				NetworkRoute netRoute = new LinkNetworkRouteImpl(routeId2linkIDs.get(routeId).get(0), routeId2linkIDs.get(routeId).get(routeId2linkIDs.get(routeId).size()-1));	// Start-Link, End-Link	
				netRoute.setLinkIds(routeId2linkIDs.get(routeId).get(0), getMiddleRouteLinkIDs(routeId2linkIDs.get(routeId)), routeId2linkIDs.get(routeId).get(routeId2linkIDs.get(routeId).size()-1)); // Start-link, link-Ids als List, End-link
				routeId2NetworkRoute.put(routeId, netRoute);
			}
			return routeId2NetworkRoute;
		}

		private Map<Id, List<TransitRouteStop>> getRouteId2TransitRouteStops(
				double stopTime, double travelTimeBus) {

			Map<Id, List<TransitRouteStop>> routeId2transitRouteStops = new HashMap<Id, List<TransitRouteStop>>();
			for (Id routeId : routeId2transitStopFacilities.keySet()){
				double arrivalTime = 0;
				double departureTime = 0;
				List<TransitRouteStop> transitStopFacilities = new ArrayList<TransitRouteStop>();
				for (TransitStopFacility transitStopFacility : routeId2transitStopFacilities.get(routeId)){
					TransitRouteStop transitRouteStop = sf.createTransitRouteStop(transitStopFacility, arrivalTime, departureTime);
					arrivalTime = departureTime + travelTimeBus;
					departureTime = arrivalTime + stopTime;
					transitRouteStop.setAwaitDepartureTime(true);
					transitStopFacilities.add(transitRouteStop);
				}
			routeId2transitRouteStops.put(routeId, transitStopFacilities);
			}
			return routeId2transitRouteStops;
		}

		private List<TransitStopFacility> getTransitStopFacility(List<Id> stopFacilityIDsRoute, List<Id> linkIDsRoute, List<Id> nodeIDs, Network network) {
			List<TransitStopFacility> transitStopFacilities = new ArrayList<TransitStopFacility>();
			for (Id stopFacId : stopFacilityIDsRoute){
				int index = stopFacilityIDsRoute.indexOf(stopFacId);
				TransitStopFacility transStopFacil = sf.createTransitStopFacility(stopFacId, network.getNodes().get(nodeIDs.get(index)).getCoord(), true);
				transStopFacil.setLinkId(linkIDsRoute.get(index));
				schedule.addStopFacility(transStopFacil);
				transitStopFacilities.add(transStopFacil);
			}
			return transitStopFacilities;
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

		public void setTravelTimeBus(double travelTimeBus) {
			this.travelTimeBus = travelTimeBus;
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
