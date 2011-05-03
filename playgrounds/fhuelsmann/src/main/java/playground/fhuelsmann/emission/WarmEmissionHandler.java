package playground.fhuelsmann.emission;
/* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.LinkImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehicleType;

import playground.fhuelsmann.emission.objects.HbefaObject;

public class WarmEmissionHandler implements LinkEnterEventHandler,LinkLeaveEventHandler, AgentArrivalEventHandler,AgentDepartureEventHandler {

	private Network network = null;
	private Vehicles vehicles = null;
	private Households households = null;
	private HbefaObject[][] hbefaTable = null;
	private HbefaObject[][] hbefaHdvTable =null;
	private AnalysisModule linkAndAgentAccountAnalysisModule = null;

	public WarmEmissionHandler(Households households, Vehicles vehicles,final Network network, HbefaObject[][] hbefaTable, HbefaObject[][] hbefaHdvTable, AnalysisModule linkAndAgentAccountAnalysisModule) {
		this.households = households;
		this.vehicles = vehicles;
		this.network = network;
		this.hbefaTable = hbefaTable;
		this.hbefaHdvTable = hbefaHdvTable;
		this.linkAndAgentAccountAnalysisModule = linkAndAgentAccountAnalysisModule;
	}

	Map<Id, Id> personId2VehicleId = getVehicleIdFromHouseholds(households);
	Map<Id, Id> vehicleId2VehicleType = getVehicleTypeFromVehicleId(vehicles);
	
	private final Map<Id, Double> linkenter = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentarrival = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentdeparture = new TreeMap<Id, Double>();

	public void reset(int iteration) {

	}

	public void handleEvent(LinkEnterEvent event) {
		//		String id = event.getPersonId().toString();
		//		Id onelink = event.getLinkId();
		//		if (onelink.equals(new IdImpl("590000822"))){
		//			System.out.println(onelink);
		//			if(id.contains("testVehicle")){
		this.linkenter.put(event.getPersonId(), event.getTime());
		//			}
	}
	//	}

	public void handleEvent(AgentArrivalEvent event) {
		//		String id = event.getPersonId().toString();
		//		Id onelink = event.getLinkId();
		//		if (onelink == new IdImpl("590000822")){
		//		if(id.contains("testVehicle")){
		this.agentarrival.put(event.getPersonId(), event.getTime());
		//		}
	}
	//	}

	public void handleEvent(AgentDepartureEvent event) {
		//		String id = event.getPersonId().toString();
		//		Id onelink = event.getLinkId();
		//			if (onelink == new IdImpl("590000822")){
		//				if(id.contains("testVehicle")){
		this.agentdeparture.put(event.getPersonId(), event.getTime());
		//				}
	}
	//	}

	public void handleEvent(LinkLeaveEvent event) {	

		//		String id = event.getPersonId().toString();
		//		Id onelink = event.getLinkId();
		//			if (onelink.equals(new IdImpl("590000822"))){
		//				if(id.contains("testVehicle")){
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		Double distance = link.getLength();
		Double freeVelocity = link.getFreespeed();

		String roadTypeString = link.getType();
		Integer roadType = null;
		try{
			roadType = Integer.parseInt(roadTypeString);
		}
		catch (NumberFormatException e){
			System.err.println("Error: roadtype missing");
		}
		
//		Household persId = this.households.getHouseholds().get(personId);
//			Id vehId = persId.getVehicleIds());
//		Vehicle veh = this.vehicles.getVehicles().get(vehId);
//		VehicleType vehType = veh.getType();
		
	

		if (this.linkenter.containsKey(event.getPersonId())) {						
			// link with activity
			if (this.agentarrival.containsKey(personId)) {
				double enterTime = this.linkenter.get(personId);
				double arrivalTime = this.agentarrival.get(personId);
				double departureTime = this.agentdeparture.get(personId);

				double travelTime = event.getTime() - enterTime - departureTime + arrivalTime;
				double averageSpeed=(distance/1000)/(travelTime/3600);

				this.agentarrival.remove(personId);

				linkAndAgentAccountAnalysisModule.calculateEmissionsPerLink(travelTime, linkId, personId, averageSpeed,roadType, freeVelocity, distance, hbefaTable,hbefaHdvTable);	
				linkAndAgentAccountAnalysisModule.calculateEmissionsPerPerson(travelTime, personId, averageSpeed,roadType, freeVelocity, distance, hbefaTable,hbefaHdvTable);	
			}
			// if (this.agentarrival.containsKey(personId)) is not the case (link without activity)
			else {
				double enterTime = this.linkenter.get(personId);
				double travelTime = event.getTime() - enterTime;
				double averageSpeed=(distance/1000)/(travelTime/3600);

				linkAndAgentAccountAnalysisModule.calculateEmissionsPerLink(travelTime, linkId, personId, averageSpeed,roadType, freeVelocity, distance, hbefaTable,hbefaHdvTable);	
				linkAndAgentAccountAnalysisModule.calculateEmissionsPerPerson(travelTime, personId, averageSpeed,roadType, freeVelocity, distance, hbefaTable,hbefaHdvTable);
			}
		}
		//		}
	}
	
	private Map<Id, Id> getVehicleTypeFromVehicleId (Vehicles vehicle) {
		Map<Id,Id> vehicleId2VehicleType = new TreeMap<Id, Id>();
		
		//iterating over every vehicle veh in order to get vehicleIds and vehcile type 
		for (Vehicle veh : vehicle.getVehicles().values()){
			Id vehicleId = veh.getId();
			Id vehicleType = veh.getType().getId();
			vehicleId2VehicleType.put(vehicleId, vehicleType);	
//			System.out.print("\n ++++++++++++++++++++++++++++++++++++"+vehicleId +"  "+ vehicleType);
		}
		
		return vehicleId2VehicleType;
	}
	
	private Map<Id, Id> getVehicleIdFromHouseholds(Households households) {
		Map<Id,Id> personId2VehicleId = new TreeMap<Id, Id>();
		
		//iterating over every household hh in order to get personIds and personal income 
	
		for (Household hh : households.getHouseholds().values()) {
			Id personId = hh.getMemberIds().get(0);
			if (hh.getVehicleIds() != null && !hh.getVehicleIds().isEmpty()){
				Id vehicleId = hh.getVehicleIds().get(0);
				personId2VehicleId.put(personId, vehicleId);
				System.out.print("\n ****************************"+personId +"  "+ vehicleId);}
		
		}
		return personId2VehicleId;
	}
}
