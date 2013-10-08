/* *********************************************************************** *
 * project: org.matsim.*
 * WarmEmissionHandler.java
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
package playground.vsp.emissions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import playground.vsp.emissions.WarmEmissionAnalysisModule.WarmEmissionAnalysisModuleParameter;
import playground.vsp.emissions.types.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class WarmEmissionHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler {
	private static final Logger logger = Logger.getLogger(WarmEmissionHandler.class);

	Network network;
	Vehicles emissionVehicles;
	WarmEmissionAnalysisModule warmEmissionAnalysisModule;
	
	int linkLeaveCnt = 0;
	int linkLeaveWarnCnt = 0;
	final int maxLinkLeaveWarnCnt = 3;

	Map<Id, Tuple<Id, Double>> linkenter = new HashMap<Id, Tuple<Id, Double>>();
	Map<Id, Tuple<Id, Double>> agentarrival = new HashMap<Id, Tuple<Id, Double>>();
	Map<Id, Tuple<Id, Double>> agentdeparture = new HashMap<Id, Tuple<Id, Double>>();

	public WarmEmissionHandler(
			Vehicles emissionVehicles,
			final Network network, 
			WarmEmissionAnalysisModuleParameter parameterObject,
			EventsManager emissionEventsManager, Double emissionEfficiencyFactor) {
		
		this.emissionVehicles = emissionVehicles;
		this.network = network;
		this.warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(parameterObject, emissionEventsManager, emissionEfficiencyFactor);	
	}
	
	@Override
	public void reset(int iteration) {
		linkLeaveCnt = 0;
		linkLeaveWarnCnt = 0;
		
		linkenter.clear();
		agentarrival.clear();
		agentdeparture.clear();
		
		warmEmissionAnalysisModule.reset();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(!event.getLegMode().equals("car")){ // link travel time calculation not neccessary for other modes
			return;
		}
		Tuple<Id, Double> linkId2Time = new Tuple<Id, Double>(event.getLinkId(), event.getTime());
		this.agentarrival.put(event.getPersonId(), linkId2Time);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(!event.getLegMode().equals("car")){ // link travel time calculation not neccessary for other modes
			return;
		}
		Tuple<Id, Double> linkId2Time = new Tuple<Id, Double>(event.getLinkId(), event.getTime());
		this.agentdeparture.put(event.getPersonId(), linkId2Time);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Tuple<Id, Double> linkId2Time = new Tuple<Id, Double>(event.getLinkId(), event.getTime());
		this.linkenter.put(event.getPersonId(), linkId2Time);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		linkLeaveCnt++;
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();
		Double leaveTime = event.getTime();
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		Double linkLength = link.getLength();
		Double freeVelocity = link.getFreespeed();
		String roadTypeString = link.getType();
		Integer roadType;

		try{
			roadType = Integer.parseInt(roadTypeString);
		}
		catch(NumberFormatException e){
			logger.error("Roadtype missing in network information!");
			throw new RuntimeException(e);
		}

		if(!this.linkenter.containsKey(event.getPersonId())){
			if(linkLeaveWarnCnt < maxLinkLeaveWarnCnt){
				logger.warn("Person " + personId + " is leaving link " + linkId + " without having entered. " +
				"Thus, no emissions are calculated for this link leave event.");
				if (linkLeaveWarnCnt == maxLinkLeaveWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			linkLeaveWarnCnt++;
			return;
		}
		
		double enterTime = this.linkenter.get(personId).getSecond();
		double travelTime;
		if(!this.agentarrival.containsKey(personId) || !this.agentdeparture.containsKey(personId)){
			travelTime = leaveTime - enterTime;
		}
		else if(!this.agentarrival.get(personId).getFirst().equals(event.getLinkId())
				|| !this.agentdeparture.get(personId).getFirst().equals(event.getLinkId())){

			travelTime = leaveTime - enterTime;
		} else {
		double arrivalTime = this.agentarrival.get(personId).getSecond();		
		double departureTime = this.agentdeparture.get(personId).getSecond();	
		travelTime = leaveTime - enterTime - departureTime + arrivalTime;	
		}
		
		Id vehicleId = personId;
		String vehicleInformation;
		if(!this.emissionVehicles.getVehicles().containsKey(vehicleId)){
			throw new RuntimeException("No vehicle defined for person " + personId + ". " +
					"Please make sure that requirements for emission vehicles in " + 
					VspExperimentalConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}
		Vehicle vehicle = this.emissionVehicles.getVehicles().get(vehicleId);
		VehicleType vehicleType = vehicle.getType();
		vehicleInformation = vehicleType.getId().toString();

//// ===
//// TODO: remove this after debugging
//		double linkLength_km = linkLength / 1000;
//		double travelTime_h = travelTime / 3600;
//		double freeFlowSpeed_kmh_double = (freeVelocity * 3.6);
//		double averageSpeed_kmh_double = (linkLength_km / travelTime_h);
//		int freeFlowSpeed_kmh_int = (int) Math.round(freeFlowSpeed_kmh_double);
//		int averageSpeed_kmh_int = (int) Math.round(averageSpeed_kmh_double);
//
//		if (averageSpeed_kmh_int > freeFlowSpeed_kmh_int){
//			logger.info("enterTime | personId | linkId  | linkLength_km | averageSpeed_kmh_double ; averageSpeed_kmh_int | freeFlowSpeed_kmh_double ; freeFlowSpeed_kmh_int");
//			logger.info(enterTime + " | " + personId + " | " + linkId + " | " + linkLength_km + " | " + averageSpeed_kmh_double + "; "  + averageSpeed_kmh_int + " | " + freeFlowSpeed_kmh_double + "; " + freeFlowSpeed_kmh_int);
//			throw new RuntimeException("Average speed is higher than free flow speed; this would produce negative warm emissions. Aborting...");
//		}
		
		Map<WarmPollutant, Double> warmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(
				personId,
				roadType,
				freeVelocity,
				linkLength,
				travelTime,
				vehicleInformation);
		
		warmEmissionAnalysisModule.throwWarmEmissionEvent(leaveTime, linkId, vehicleId, warmEmissions);
	}

	public int getLinkLeaveCnt() {
		return linkLeaveCnt;
	}
	public int getLinkLeaveWarnCnt() {
		return linkLeaveWarnCnt;
	}

	public WarmEmissionAnalysisModule getWarmEmissionAnalysisModule(){
		return warmEmissionAnalysisModule;
	}
}