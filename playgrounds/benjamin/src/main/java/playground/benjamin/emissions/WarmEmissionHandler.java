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
package playground.benjamin.emissions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
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
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

/**
 * @author benjamin
 *
 */
public class WarmEmissionHandler implements LinkEnterEventHandler,LinkLeaveEventHandler, AgentArrivalEventHandler,AgentDepartureEventHandler {
	private static final Logger logger = Logger.getLogger(WarmEmissionHandler.class);

	private final Network network;
	private final Vehicles emissionVehicles;
	private final WarmEmissionAnalysisModule warmEmissionAnalysisModule;
	private static int linkLeaveCnt = 0;
	private static int linkLeaveWarnCnt = 0;
	private static int maxLinkLeaveWarnCnt = 3;

	private final Map<Id, Tuple<Id, Double>> linkenter = new HashMap<Id, Tuple<Id, Double>>();
	private final Map<Id, Tuple<Id, Double>> agentarrival = new HashMap<Id, Tuple<Id, Double>>();
	private final Map<Id, Tuple<Id, Double>> agentdeparture = new HashMap<Id, Tuple<Id, Double>>();

	public WarmEmissionHandler(Vehicles emissionVehicles, final Network network, WarmEmissionAnalysisModule warmEmissionAnalysisModule) {
		this.emissionVehicles = emissionVehicles;
		this.network = network;
		this.warmEmissionAnalysisModule = warmEmissionAnalysisModule;
	}
	public void reset(int iteration) {
	}

	public void handleEvent(AgentArrivalEvent event) {
		if(!event.getLegMode().equals("car")){ // link travel time calculation not neccessary for other modes
			return;
		}
		Tuple<Id, Double> linkId2Time = new Tuple<Id, Double>(event.getLinkId(), event.getTime());
		this.agentarrival.put(event.getPersonId(), linkId2Time);
	}

	public void handleEvent(AgentDepartureEvent event) {
		if(!event.getLegMode().equals("car")){ // link travel time calculation not neccessary for other modes
			return;
		}
		Tuple<Id, Double> linkId2Time = new Tuple<Id, Double>(event.getLinkId(), event.getTime());
		this.agentdeparture.put(event.getPersonId(), linkId2Time);
	}

	public void handleEvent(LinkEnterEvent event) {
		Tuple<Id, Double> linkId2Time = new Tuple<Id, Double>(event.getLinkId(), event.getTime());
		this.linkenter.put(event.getPersonId(), linkId2Time);
	}

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

		//TODO: remove this after debuging
		double linkLength_km = linkLength / 1000;
		double travelTime_h = travelTime / 3600;
		double freeFlowSpeed_kmh_double = (freeVelocity * 3.6);
		double averageSpeed_kmh_double = (linkLength_km / travelTime_h);
		int freeFlowSpeed_kmh_int = (int) Math.round(freeFlowSpeed_kmh_double);
		int averageSpeed_kmh_int = (int) Math.round(averageSpeed_kmh_double);
		
		logger.info(linkId + "| " + averageSpeed_kmh_double + "; "  + averageSpeed_kmh_int + "| " + freeFlowSpeed_kmh_double + " ;" + freeFlowSpeed_kmh_int);
		
		if (averageSpeed_kmh_int > freeFlowSpeed_kmh_int){
			logger.info("departureTime_h: " + this.agentdeparture.get(personId).getSecond() / 3600);
			logger.info("arrivalTime_h: " + this.agentarrival.get(personId).getSecond() / 3600);
			logger.info("averageSpeed_kmh: " + averageSpeed_kmh_int + "; freeFlowSpeed_kmh: " + freeFlowSpeed_kmh_int);
			throw new RuntimeException("Average speed was higher than free flow speed; this would produce negative warm emissions. Aborting...");
		}
		
		warmEmissionAnalysisModule.calculateWarmEmissionsAndThrowEvent(
				linkId,
				personId,
				roadType,
				freeVelocity,
				linkLength,
				enterTime,
				travelTime,
				vehicleInformation);
	}

	public static int getLinkLeaveCnt() {
		return linkLeaveCnt;
	}
	public static int getLinkLeaveWarnCnt() {
		return linkLeaveWarnCnt;
	}

}