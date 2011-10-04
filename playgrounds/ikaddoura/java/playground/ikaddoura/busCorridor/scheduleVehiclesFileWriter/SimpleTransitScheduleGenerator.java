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
package playground.ikaddoura.busCorridor.scheduleVehiclesFileWriter;

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
public class SimpleTransitScheduleGenerator {

		public static void main(String[] args) throws IOException {

			SimpleTransitScheduleGenerator generator = new SimpleTransitScheduleGenerator();
			generator.createSchedule();
			
		}
			
		public void createSchedule() throws IOException {
			
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
			
			Map<Id, List<Id>> linkListMap = new HashMap<Id, List<Id>>(); 
			Map<Id, List<TransitRouteStop>> stopListMap = new HashMap<Id, List<TransitRouteStop>>();
			Map<Id, NetworkRoute> netRouteMap = new HashMap<Id, NetworkRoute>();
			Map<Id, TransitRoute> transRouteMap = new HashMap<Id, TransitRoute>();
			List<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>();	

			Id transitLineId = new IdImpl("Bus Line");
			Id routeId = new IdImpl("West-Ost");
			
			Id departureId1 = new IdImpl("dep_1");
			
			Id vehTypeId = new IdImpl("Bus");
			Id vehicleId = new IdImpl("bus_1");
			
			Id stopFacility1 = new IdImpl("1to2");
			Id nodeId1 = new IdImpl("2");
			Id linkId1 = new IdImpl("1to2");
			Id stopFacility2 = new IdImpl("3to4");
			Id nodeId3 = new IdImpl("4");
			Id linkId3 = new IdImpl("3to4");
			
			Id linkId2 = new IdImpl("2to3");
			
			List<Id> linkList = new ArrayList<Id>();
			linkList.add(linkId2);			
			
			TransitStopFacility transStopFacil1 = sf.createTransitStopFacility(stopFacility1, network.getNodes().get(nodeId1).getCoord(), false);
			transStopFacil1.setLinkId(linkId1);
			schedule.addStopFacility(transStopFacil1);
			TransitStopFacility transStopFacil2 = sf.createTransitStopFacility(stopFacility2, network.getNodes().get(nodeId3).getCoord(), false);
			transStopFacil2.setLinkId(linkId3);
			schedule.addStopFacility(transStopFacil2);
			
			TransitRouteStop transStop1 = sf.createTransitRouteStop(transStopFacil1, 0, 0);
			stopList.add(transStop1);
			TransitRouteStop transStop2 = sf.createTransitRouteStop(transStopFacil2, 600, 0);
			stopList.add(transStop2);
			
			stopListMap.put(routeId, stopList);
			
			linkListMap.put(routeId, linkList);
					
			NetworkRoute netRoute = new LinkNetworkRouteImpl(linkId1, linkId3);		
			netRoute.setLinkIds(linkId1, linkListMap.get(routeId), linkId3);
			netRouteMap.put(transitLineId, netRoute);		
			
//			final Id routeId, final NetworkRoute route, final List<TransitRouteStop> stops, final String mode
			TransitRoute transRoute = sf.createTransitRoute(routeId, netRouteMap.get(transitLineId), stopListMap.get(routeId), "bus");
			transRouteMap.put(transitLineId, transRoute);
				
//			final Id departureId, final double time
			Departure departure1 = sf.createDeparture(departureId1, 8*3600);
			departure1.setVehicleId(vehicleId);
			transRouteMap.get(transitLineId).addDeparture(departure1);
						
			TransitLine transLine = sf.createTransitLine(transitLineId);
			schedule.addTransitLine(transLine);
			transLine.addRoute(transRouteMap.get(transitLineId));	
			
			VehicleType type = veh.getFactory().createVehicleType(vehTypeId);
			VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
			cap.setSeats(15);
			cap.setStandingRoom(20);
			type.setCapacity(cap);
			veh.getVehicleTypes().put(vehTypeId, type); 
			veh.getVehicles().put(vehicleId, veh.getFactory().createVehicle(vehicleId, veh.getVehicleTypes().get(vehTypeId)));
			
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(schedule);
		scheduleWriter.write("../../shared-svn/studies/ihab/busCorridor/input/transitscheduleTESTX.xml");

		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(veh);
		vehicleWriter.writeFile("../../shared-svn/studies/ihab/busCorridor/input/transitVehiclesTESTX.xml");

		}
}
