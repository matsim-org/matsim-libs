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

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.LinkImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import playground.fhuelsmann.emission.objects.HbefaObject;

public class WarmEmissionHandler implements LinkEnterEventHandler,LinkLeaveEventHandler, AgentArrivalEventHandler,AgentDepartureEventHandler {
	private static final Logger logger = Logger.getLogger(WarmEmissionHandler.class);

	private Network network = null;
	private Population population = null;
	private Vehicles vehicles = null;
	private HbefaObject[][] hbefaTable = null;
	private HbefaObject[][] hbefaHdvTable = null;
	private AnalysisModule linkAndAgentAccountAnalysisModule = null;
	private ArrayList<String> listOfPollutant = new ArrayList<String>();	

	public ArrayList<String> getListOfPollutant() {
		return listOfPollutant;
	}

	public void setListOfPollutant(ArrayList<String> listOfPollutant) {
		this.listOfPollutant = listOfPollutant;
	}

	public WarmEmissionHandler(Population population, Vehicles vehicles, final Network network, HbefaObject[][] hbefaTable,
			HbefaObject[][] hbefaHdvTable, AnalysisModule linkAndAgentAccountAnalysisModule) {
		this.population = population;
		this.vehicles = vehicles;
		this.network = network;
		this.hbefaTable = hbefaTable;
		this.hbefaHdvTable = hbefaHdvTable;
		this.linkAndAgentAccountAnalysisModule = linkAndAgentAccountAnalysisModule;
	}

	private final Map<Id, Double> linkenter = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentarrival = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentdeparture = new TreeMap<Id, Double>();

	public void reset(int iteration) {
	}

	public void handleEvent(LinkEnterEvent event) {
		this.linkenter.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(AgentArrivalEvent event) {
		this.agentarrival.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.agentdeparture.put(event.getPersonId(), event.getTime());
		//		Id personId= event.getPersonId();
		//		Id linkId = event.getLinkId();
		//		if (event.getLegMode().equals("pt")|| event.getLegMode().equals("walk")|| event.getLegMode().equals("bike"))	{
		//			System.out.println("+++++++personId "+personId+" leg "+ event.getLegMode());
		//			linkAndAgentAccountAnalysisModule.calculatePerPersonPtBikeWalk(personId, linkId);
		//			linkAndAgentAccountAnalysisModule.calculatePerLinkPtBikeWalk(linkId, personId);}
	}

	public void handleEvent(LinkLeaveEvent event) {	
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		Double distance = link.getLength();
		Double freeVelocity = link.getFreespeed();
		String roadTypeString = link.getType();
		Integer roadType = null;
		Vehicle veh = null;
		try{
			roadType = Integer.parseInt(roadTypeString);
		}
		catch (NumberFormatException e){
			logger.warn("Error: roadtype missing! Exception " + e);
		}
		Double enterTime = 0.0;
		Double travelTime = 0.0;
		Double averageSpeed = 0.0;

		if(this.linkenter.containsKey(event.getPersonId())){
			if (this.agentarrival.containsKey(personId)){
				double arrivalTime = this.agentarrival.get(personId);			
				double departureTime = this.agentdeparture.get(personId);			
				enterTime = this.linkenter.get(personId);
				travelTime = event.getTime() - enterTime - departureTime + arrivalTime;				
				averageSpeed=(distance / 1000) / (travelTime / 3600);				
				this.agentarrival.remove(personId);
			}
			else{
				enterTime = this.linkenter.get(personId);
				travelTime = event.getTime() - enterTime;
				averageSpeed=(distance / 1000) / (travelTime / 3600);
			}

			if (personId.toString().contains("#")){
				try{		
					Id vehId = personId;
					veh = this.vehicles.getVehicles().get(vehId);
				}
				catch(Exception e){
				}	
				// # with leg car --> they have a vehicletype
				if (veh != null ){
					VehicleType vehType = veh.getType();
					String fuelSizeAge = vehType.getDescription();
					linkAndAgentAccountAnalysisModule.calculateEmissionsPerLink(travelTime, linkId, personId, averageSpeed,roadType, fuelSizeAge, freeVelocity, distance, hbefaTable,hbefaHdvTable);											
					linkAndAgentAccountAnalysisModule.calculateEmissionsPerPerson(travelTime, personId, averageSpeed,roadType,fuelSizeAge, freeVelocity, distance, hbefaTable,hbefaHdvTable);
				}
				// # with leg car -> they have no vehicletype
				else{
					linkAndAgentAccountAnalysisModule.calculateEmissionsPerLinkForComHdvPecWithoutVeh(travelTime, linkId, personId, averageSpeed,roadType, freeVelocity, distance, hbefaTable,hbefaHdvTable);
					linkAndAgentAccountAnalysisModule.calculateEmissionsPerCommuterHdvPcWithoutVeh(travelTime, personId, averageSpeed,roadType, freeVelocity, distance, hbefaTable,hbefaHdvTable);
				}
			}
			else{
				// gv_, pv_car_
				linkAndAgentAccountAnalysisModule.calculateEmissionsPerLinkForComHdvPecWithoutVeh(travelTime, linkId, personId, averageSpeed,roadType, freeVelocity, distance, hbefaTable,hbefaHdvTable);
				linkAndAgentAccountAnalysisModule.calculateEmissionsPerCommuterHdvPcWithoutVeh(travelTime, personId, averageSpeed,roadType, freeVelocity, distance, hbefaTable,hbefaHdvTable);				
			}	
		}	
	}
}

