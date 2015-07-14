/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
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

package air.scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
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

public class SfTransitBuilder {

	private static final Logger log = Logger.getLogger(SfTransitBuilder.class);

	public static final String FLIGHT_TRANSIT_SCHEDULE = "flight_transit_schedule.xml";
		
	public static final String FLIGHT_TRANSIT_VEHICLES = "flight_transit_vehicles.xml";
	
	private static final double TAXI_TOL_TIME = SfMatsimAirport.TAXI_TOL_TIME;
	
	private Scenario loadScenario(String inputNetworkFile){
		Scenario scen = ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		Config config = scen.getConfig();
		config.network().setInputFile(inputNetworkFile);
		ScenarioUtils.loadScenario(scen);		
		return scen;
	}
	
	
	public void createSchedule(String inputOagFlights, String inputNetworkFile, String outputDirectory) throws IOException {
		ScenarioImpl scen = (ScenarioImpl) this.loadScenario(inputNetworkFile);
		Network network = scen.getNetwork();
		scen.getConfig().transit().setUseTransit(true);
		scen.getConfig().scenario().setUseVehicles(true);
		
		BufferedReader br = new BufferedReader(new FileReader(new File(inputOagFlights)));
		
		TransitScheduleFactory sf = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = sf.createTransitSchedule();
		
		Vehicles veh = VehicleUtils.createVehiclesContainer();
				
		Map<Id<TransitRoute>, List<Id<Link>>> linkListMap = new HashMap<>(); 
		Map<Id, List<TransitRouteStop>> stopListMap = new HashMap<Id, List<TransitRouteStop>>();
		Map<Id, NetworkRoute> netRouteMap = new HashMap<Id, NetworkRoute>();
		Map<Id, TransitRoute> transRouteMap = new HashMap<Id, TransitRoute>();
		
		while (br.ready()) {
			
			String oneLine = br.readLine();
			String[] lineEntries = oneLine.split("\t");
			String[] airportCodes = lineEntries[0].split("_");
			String origin = airportCodes[0];
			String destination = airportCodes[1];
			String transitRoute = lineEntries[0];
			String transitLine = lineEntries[1];
			double departureTime = Double.parseDouble(lineEntries[3]);
			double duration = Double.parseDouble(lineEntries[4]);
			double distance = 1000.0 * (Double.parseDouble(lineEntries[7])); //km to m
//			double vehicleSpeed =(100*Math.round(distance/(duration-TAXI_TOL_TIME)))/100.;
			double vehicleSpeed = distance / (duration - TAXI_TOL_TIME);
			Id<TransitStopFacility> originId = Id.create(origin, TransitStopFacility.class);
			Id<TransitStopFacility> destinationId = Id.create(destination, TransitStopFacility.class);
			Id<TransitRoute> routeId = Id.create(transitRoute, TransitRoute.class);	//origin IATA code + destination IATA code
			Id<TransitLine> transitLineId = Id.create(transitLine, TransitLine.class);		//origin IATA code + destination IATA code + airline IATA code
			Id<Departure> flightNumber = Id.create(lineEntries[2], Departure.class);	//flight number
			Id<VehicleType> vehTypeId = Id.create(lineEntries[5]+"_"+lineEntries[6]+"_"+flightNumber, VehicleType.class);	//IATA aircraft code + seats avail
			int aircraftCapacity = Integer.parseInt(lineEntries[6]);
			List<Id<Link>> linkList = new ArrayList<Id<Link>>();	//evtl in Map mit Route als key verpacken
			List<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>();	//evtl in Map mit Route als key verpacken
			
			//nur ausführen, wenn stopListMap noch keinen entspechenden key enthält
			
			if (!stopListMap.containsKey(routeId)) {			
				TransitStopFacility transStopFacil = sf.createTransitStopFacility(originId, network.getNodes().get(originId).getCoord(), false);
				transStopFacil.setLinkId(Id.create(originId, Link.class));
				TransitRouteStop transStop = sf.createTransitRouteStop(transStopFacil, 0, 0);
				stopList.add(transStop);				
				TransitStopFacility transStopFacil2 = sf.createTransitStopFacility(destinationId, network.getNodes().get(destinationId).getCoord(), false);
				transStopFacil2.setLinkId(Id.create(destinationId, Link.class));
				TransitRouteStop transStop2 = sf.createTransitRouteStop(transStopFacil2, 0, 0);
				stopList.add(transStop2);	
				if (!schedule.getFacilities().containsKey(originId)) schedule.addStopFacility(transStopFacil);
				if (!schedule.getFacilities().containsKey(destinationId)) schedule.addStopFacility(transStopFacil2);
				stopListMap.put(routeId, stopList);
			}
				
			//nur ausführen, wenn linkListMap noch keinen entsprechenden key enthält
			
			if (!linkListMap.containsKey(routeId) && DgCreateSfFlightScenario.NUMBER_OF_RUNWAYS==2) {
				linkList.add(Id.create(origin+"taxiOutbound", Link.class));
				linkList.add(Id.create(origin+"runwayOutbound", Link.class));
				linkList.add(Id.create(origin+destination, Link.class));
				if (DgCreateSfFlightScenario.doCreateStars) {
					linkList.add(Id.create(destination+"star", Link.class));
				}
				linkList.add(Id.create(destination+"runwayInbound", Link.class));
				linkList.add(Id.create(destination+"taxiInbound", Link.class));
				linkListMap.put(routeId, linkList);
			}
			
			if (!linkListMap.containsKey(routeId) && DgCreateSfFlightScenario.NUMBER_OF_RUNWAYS==1) {
				linkList.add(Id.create(origin+"taxiOutbound", Link.class));
				linkList.add(Id.create(origin+"runway", Link.class));
				linkList.add(Id.create(origin+destination, Link.class));
				if (DgCreateSfFlightScenario.doCreateStars) {
					linkList.add(Id.create(destination+"star", Link.class));
				}
				linkList.add(Id.create(destination+"runway", Link.class));
				linkList.add(Id.create(destination+"taxiInbound", Link.class));
				linkListMap.put(routeId, linkList);
			}
			
			if (!netRouteMap.containsKey(transitLineId)) {
				NetworkRoute netRoute = new LinkNetworkRouteImpl(Id.create(origin, Link.class), Id.create(destination, Link.class));		
				netRoute.setLinkIds(Id.create(origin, Link.class), linkListMap.get(routeId), Id.create(destination, Link.class));
				netRouteMap.put(transitLineId, netRoute);
			}			
			
			if (!transRouteMap.containsKey(transitLineId)) {
				TransitRoute transRoute = sf.createTransitRoute(Id.create(transitRoute, TransitRoute.class), netRouteMap.get(transitLineId), stopListMap.get(routeId), "pt");
				transRouteMap.put(transitLineId, transRoute);
			}
						
			Departure departure = sf.createDeparture(flightNumber, departureTime);
			departure.setVehicleId(Id.create(flightNumber, Vehicle.class));
			transRouteMap.get(transitLineId).addDeparture(departure);
						
			if (!schedule.getTransitLines().containsKey(transitLineId)) {
				TransitLine transLine = sf.createTransitLine(transitLineId);
				transLine.addRoute(transRouteMap.get(transitLineId));	
				schedule.addTransitLine(transLine);
			}
			
			VehicleType type = veh.getVehicleTypes().get(vehTypeId);
			if (type == null) {
				type = veh.getFactory().createVehicleType(vehTypeId);
				VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
				cap.setSeats(aircraftCapacity);
				type.setCapacity(cap);
				type.setMaximumVelocity(vehicleSpeed);
				veh.addVehicleType(type); 
			}
			Vehicle vehicle = veh.getFactory().createVehicle(Id.create(flightNumber, Vehicle.class), type); 
			veh.addVehicle( vehicle);
			
		}
		
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(schedule);
		scheduleWriter.write(outputDirectory + FLIGHT_TRANSIT_SCHEDULE);
		
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(veh);
		vehicleWriter.writeFile(outputDirectory + FLIGHT_TRANSIT_VEHICLES);
		
		log.info("Created transit schedule and vehicles.");
	}
}
