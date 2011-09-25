/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleGenerator.java
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
package playground.ikaddoura.busCorridor;

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
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesImpl;

/**
 * @author Ihab
 *
 */
public class BusCorridorTransitScheduleGenerator {

		public static void main(String[] args) throws IOException {

			BusCorridorTransitScheduleGenerator generator = new BusCorridorTransitScheduleGenerator();
			generator.createSchedule();
			
		}
			
		public void createSchedule() throws IOException {
			
			double stopTime = 30;
			double travelTimeBus = 3*60;
			
			Scenario scen = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
			Config config = scen.getConfig();
			config.network().setInputFile("../../shared-svn/studies/ihab/busCorridor/input/network_busline.xml");
			ScenarioUtils.loadScenario(scen);		
			Network network = scen.getNetwork();
			
			scen.getConfig().scenario().setUseTransit(true);
			scen.getConfig().scenario().setUseVehicles(true);
				
			TransitScheduleFactory sf = new TransitScheduleFactoryImpl();
			TransitSchedule schedule = sf.createTransitSchedule();
			
			Vehicles veh = new VehiclesImpl();
			
			Map<Id, List<Id>> routeId2stopFacilities = new HashMap<Id, List<Id>>();
			List<Id> stopFacilityIDsRoute1 = new ArrayList<Id>();
			List<Id> stopFacilityIDsRoute2 = new ArrayList<Id>();

			List<Id> nodeIDs = new ArrayList<Id>();
			
			Map<Id, List<Id>> routeId2linkIDs = new HashMap<Id, List<Id>>();
			List<Id> linkIDsRoute1 = new ArrayList<Id>();
			List<Id> linkIDsRoute2 = new ArrayList<Id>();
			
			Map<Id, List<TransitRouteStop>> routeId2TransitRouteStops = new HashMap<Id, List<TransitRouteStop>>();
			Map<Id, NetworkRoute> routeId2networkRoute = new HashMap<Id, NetworkRoute>();
			Map<Id, TransitRoute> routeId2transitRoute = new HashMap<Id, TransitRoute>();
			
			Map<Id, List<TransitRouteStop>> routeId2transitRouteStops = new HashMap<Id, List<TransitRouteStop>>();
			
			Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities = new HashMap<Id, List<TransitStopFacility>>();
			List<TransitStopFacility> stopFacilityListRoute1 = new ArrayList<TransitStopFacility>();	
			List<TransitStopFacility> stopFacilityListRoute2 = new ArrayList<TransitStopFacility>();	
			
			Id transitLineId = new IdImpl("Bus Line");
			Id routeId1 = new IdImpl("West-Ost");
			Id routeId2 = new IdImpl("Ost-West");

			Id departureId1 = new IdImpl("dep_1");
			
			Id vehTypeId = new IdImpl("Bus");
			Id vehicleId = new IdImpl("bus_1");
			
			nodeIDs.add(new IdImpl("2"));
			nodeIDs.add(new IdImpl("3"));
			nodeIDs.add(new IdImpl("4"));
			nodeIDs.add(new IdImpl("5"));
			nodeIDs.add(new IdImpl("6"));
			nodeIDs.add(new IdImpl("7"));
			nodeIDs.add(new IdImpl("8"));
			nodeIDs.add(new IdImpl("9"));
			nodeIDs.add(new IdImpl("10"));
			
			stopFacilityIDsRoute1.add(new IdImpl("1to2"));
			stopFacilityIDsRoute1.add(new IdImpl("2to3"));
			stopFacilityIDsRoute1.add(new IdImpl("3to4"));
			stopFacilityIDsRoute1.add(new IdImpl("4to5"));
			stopFacilityIDsRoute1.add(new IdImpl("5to6"));
			stopFacilityIDsRoute1.add(new IdImpl("6to7"));
			stopFacilityIDsRoute1.add(new IdImpl("7to8"));
			stopFacilityIDsRoute1.add(new IdImpl("8to9"));
			stopFacilityIDsRoute1.add(new IdImpl("9to10"));
			
			stopFacilityIDsRoute2.add(new IdImpl("3to2"));
			stopFacilityIDsRoute2.add(new IdImpl("4to3"));
			stopFacilityIDsRoute2.add(new IdImpl("5to4"));
			stopFacilityIDsRoute2.add(new IdImpl("6to5"));
			stopFacilityIDsRoute2.add(new IdImpl("7to6"));
			stopFacilityIDsRoute2.add(new IdImpl("8to7"));
			stopFacilityIDsRoute2.add(new IdImpl("9to8"));
			stopFacilityIDsRoute2.add(new IdImpl("10to9"));
			stopFacilityIDsRoute2.add(new IdImpl("11to10"));

			linkIDsRoute1.add(new IdImpl("1to2"));
			linkIDsRoute1.add(new IdImpl("2to3"));
			linkIDsRoute1.add(new IdImpl("3to4"));
			linkIDsRoute1.add(new IdImpl("4to5"));
			linkIDsRoute1.add(new IdImpl("5to6"));
			linkIDsRoute1.add(new IdImpl("6to7"));
			linkIDsRoute1.add(new IdImpl("7to8"));
			linkIDsRoute1.add(new IdImpl("8to9"));
			linkIDsRoute1.add(new IdImpl("9to10"));
			
			linkIDsRoute2.add(new IdImpl("11to10"));
			linkIDsRoute2.add(new IdImpl("10to9"));
			linkIDsRoute2.add(new IdImpl("9to8"));
			linkIDsRoute2.add(new IdImpl("8to7"));
			linkIDsRoute2.add(new IdImpl("7to6"));
			linkIDsRoute2.add(new IdImpl("6to5"));
			linkIDsRoute2.add(new IdImpl("5to4"));
			linkIDsRoute2.add(new IdImpl("4to3"));
			linkIDsRoute2.add(new IdImpl("3to2"));
			
			routeId2linkIDs.put(routeId1,linkIDsRoute1);
			routeId2linkIDs.put(routeId2,linkIDsRoute2);
			
			routeId2transitStopFacilities.put(routeId1, getTransitStopFacility(stopFacilityIDsRoute1, linkIDsRoute1, nodeIDs, sf, schedule, network));
			routeId2transitStopFacilities.put(routeId2, getTransitStopFacility(stopFacilityIDsRoute2, linkIDsRoute2, nodeIDs, sf, schedule, network));
			
			routeId2TransitRouteStops = getRouteId2TransitRouteStops(routeId2transitStopFacilities, stopTime, travelTimeBus, sf);
			routeId2networkRoute = getRouteId2NetworkRoute(routeId2linkIDs);
			routeId2transitRoute = getRouteId2TransitRoute(routeId2networkRoute, routeId2TransitRouteStops, sf);
		
			Departure departure1 = sf.createDeparture(departureId1, 8*3600);
			departure1.setVehicleId(vehicleId);
			routeId2transitRoute.get(routeId1).addDeparture(departure1);
			routeId2transitRoute.get(routeId2).addDeparture(departure1);
			
			TransitLine transitLine = sf.createTransitLine(transitLineId);
			schedule.addTransitLine(transitLine);
			transitLine.addRoute(routeId2transitRoute.get(routeId1));
			transitLine.addRoute(routeId2transitRoute.get(routeId2));
			
			VehicleType type = veh.getFactory().createVehicleType(vehTypeId);
			VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
			cap.setSeats(15);
			cap.setStandingRoom(20);
			type.setCapacity(cap);
			veh.getVehicleTypes().put(vehTypeId, type); 
			veh.getVehicles().put(vehicleId, veh.getFactory().createVehicle(vehicleId, veh.getVehicleTypes().get(vehTypeId)));
			
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(schedule);
		scheduleWriter.write("../../shared-svn/studies/ihab/busCorridor/input/transitscheduleTEST_2.xml");

		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(veh);
		vehicleWriter.writeFile("../../shared-svn/studies/ihab/busCorridor/input/transitVehiclesTEST_2.xml");
		}

		private Map<Id, TransitRoute> getRouteId2TransitRoute(Map<Id, NetworkRoute> routeId2networkRoutes, Map<Id, List<TransitRouteStop>> routeId2TransitRouteStops, TransitScheduleFactory sf) {
			
			Map<Id, TransitRoute> routeId2transitRoute = new HashMap<Id, TransitRoute>();			
			for (Id routeId : routeId2networkRoutes.keySet()){
				TransitRoute transitRoute = sf.createTransitRoute(routeId, routeId2networkRoutes.get(routeId), routeId2TransitRouteStops.get(routeId), "bus");
				routeId2transitRoute.put(routeId, transitRoute);
			}
			return routeId2transitRoute;
		}

		private Map<Id, NetworkRoute> getRouteId2NetworkRoute(Map<Id, List<Id>> routeId2linkIDs) {
			Map<Id, NetworkRoute> routeId2NetworkRoute = new HashMap<Id, NetworkRoute>();
			for (Id routeId : routeId2linkIDs.keySet()){
				NetworkRoute netRoute = new LinkNetworkRouteImpl(routeId2linkIDs.get(routeId).get(0), routeId2linkIDs.get(routeId).get(routeId2linkIDs.get(routeId).size()-1));	// Start-Link, End-Link	
				netRoute.setLinkIds(routeId2linkIDs.get(routeId).get(0), getMiddleRouteLinkIDs(routeId2linkIDs.get(routeId)), routeId2linkIDs.get(routeId).get(routeId2linkIDs.get(routeId).size()-1)); // Start-link, link-Ids als List, End-link
				routeId2NetworkRoute.put(routeId, netRoute);
			}
			return routeId2NetworkRoute;
		}

		private Map<Id, List<TransitRouteStop>> getRouteId2TransitRouteStops(
				Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities,
				double stopTime, double travelTimeBus, TransitScheduleFactory sf) {

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

		private List<TransitStopFacility> getTransitStopFacility(List<Id> stopFacilityIDsRoute, List<Id> linkIDsRoute, List<Id> nodeIDs, TransitScheduleFactory sf, TransitSchedule schedule, Network network) {
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
}
