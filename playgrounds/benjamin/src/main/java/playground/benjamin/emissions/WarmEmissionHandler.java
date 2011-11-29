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
	private static int linkLeaveWarnCnt = 0;
	private static int maxLinkLeaveWarnCnt = 3;

	private final Map<Id, Double> linkenter = new HashMap<Id, Double>();
	private final Map<Id, Double> agentarrival = new HashMap<Id, Double>();
	private final Map<Id, Double> agentdeparture = new HashMap<Id, Double>();

	public WarmEmissionHandler(Vehicles emissionVehicles, final Network network, WarmEmissionAnalysisModule warmEmissionAnalysisModule) {
		this.emissionVehicles = emissionVehicles;
		this.network = network;
		this.warmEmissionAnalysisModule = warmEmissionAnalysisModule;
	}
	public void reset(int iteration) {
	}

	public void handleEvent(AgentArrivalEvent event) {
		if(event.getLegMode().equals("car")){
			this.agentarrival.put(event.getPersonId(), event.getTime());
		}
		else{
			// link travel time calcualtion not neccessary for other modes
		}
	}

	public void handleEvent(AgentDepartureEvent event) {
		if(event.getLegMode().equals("car")){
			this.agentdeparture.put(event.getPersonId(), event.getTime());
		}
		else{
			// link travel time calcualtion not neccessary for other modes
		}
	}

	public void handleEvent(LinkEnterEvent event) {
		this.linkenter.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(LinkLeaveEvent event) {
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();
		Double leaveTime = event.getTime();
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		Double linkLength = link.getLength();
		Double freeVelocity = link.getFreespeed();
		String roadTypeString = link.getType();
		Integer roadType = null;

		try{
			roadType = Integer.parseInt(roadTypeString);
		}
		catch(NumberFormatException e){
			logger.error("Roadtype missing in network information!");
			throw new RuntimeException(e);
		}
		Double enterTime = 0.0;
		Double travelTime = 0.0;

		if(this.linkenter.containsKey(event.getPersonId())){
			enterTime = this.linkenter.get(personId);
			if (this.agentarrival.containsKey(personId)){
				double arrivalTime = this.agentarrival.get(personId);		
				double departureTime = this.agentdeparture.get(personId);	
				travelTime = leaveTime - enterTime - departureTime + arrivalTime;	
				this.agentarrival.remove(personId);
			}
			else{
				travelTime = leaveTime - enterTime;
			}

			Id vehicleId = personId;
			String vehicleInformation = null;
			if(this.emissionVehicles.getVehicles().containsKey(vehicleId)){
				Vehicle vehicle = this.emissionVehicles.getVehicles().get(vehicleId);
				VehicleType vehicleType = vehicle.getType();
				vehicleInformation = vehicleType.getId().toString();
				warmEmissionAnalysisModule.calculateWarmEmissionsAndThrowEvent(
						linkId,
						personId,
						roadType,
						freeVelocity,
						linkLength,
						enterTime,
						travelTime,
						vehicleInformation);
			} else throw new RuntimeException("No vehicle defined for person " + personId + ". " +
				      						  "Please make sure that requirements for emission vehicles in " + 
				      						  VspExperimentalConfigGroup.GROUP_NAME + " config group are met. Aborting...");
			
		} else{
			if(linkLeaveWarnCnt < maxLinkLeaveWarnCnt){
				linkLeaveWarnCnt++;
				logger.warn("Person " + personId + " is leaving link " + linkId + " without having entered. " +
				"Thus, no emissions are calculated for this link leave event.");
				if (linkLeaveWarnCnt == maxLinkLeaveWarnCnt)
					logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
		}
	}

}