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
package playground.fhuelsmann.emission;

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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;


public class WarmEmissionHandler implements LinkEnterEventHandler,LinkLeaveEventHandler, AgentArrivalEventHandler,AgentDepartureEventHandler {
	private static final Logger logger = Logger.getLogger(WarmEmissionHandler.class);

	private final Network network;
	private final Vehicles vehicles;
	private final WarmEmissionAnalysisModule warmEmissionAnalysisModule;
	private static int linkLeaveWarnCnt = 0;
	private static int maxLinkLeaveWarnCnt = 10;

	private final Map<Id, Double> linkenter = new HashMap<Id, Double>();
	private final Map<Id, Double> agentarrival = new HashMap<Id, Double>();
	private final Map<Id, Double> agentdeparture = new HashMap<Id, Double>();

	public WarmEmissionHandler(Vehicles vehicles, final Network network, WarmEmissionAnalysisModule warmEmissionAnalysisModule) {
		this.vehicles = vehicles;
		this.network = network;
		this.warmEmissionAnalysisModule = warmEmissionAnalysisModule;
	}
	public void reset(int iteration) {
	}

	public void handleEvent(AgentArrivalEvent event) {
		this.agentarrival.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.agentdeparture.put(event.getPersonId(), event.getTime());
		//				Id personId= event.getPersonId();
		//				Id linkId = event.getLinkId();
		//				if (event.getLegMode().equals("pt")|| event.getLegMode().equals("walk")|| event.getLegMode().equals("bike"))	{
		//				//	System.out.println("+++++++personId "+personId+" leg "+ event.getLegMode());
		//					linkAndAgentAccountAnalysisModule.calculatePerPersonPtBikeWalk(personId, linkId);
		//					linkAndAgentAccountAnalysisModule.calculatePerLinkPtBikeWalk(linkId, personId);}
	}

	public void handleEvent(LinkEnterEvent event) {
		this.linkenter.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(LinkLeaveEvent event) {
		
		// TODO: is legMode = car?
		
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
			logger.warn("Error: roadtype missing! Exception " + e);
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
			String fuelSizeAge = null;
			if(this.vehicles.getVehicles().containsKey(vehicleId)){
				Vehicle vehicle = this.vehicles.getVehicles().get(vehicleId);
				VehicleType vehicleType = vehicle.getType();
				fuelSizeAge = vehicleType.getDescription();
			}
			else{
				// do nothing
			}
			warmEmissionAnalysisModule.calculateWarmEmissionsAndThrowEvent(
					linkId,
					personId,
					roadType,
					freeVelocity,
					linkLength,
					enterTime,
					travelTime,
					fuelSizeAge);
		}
		else{
			if(linkLeaveWarnCnt < maxLinkLeaveWarnCnt){
				linkLeaveWarnCnt++;
				logger.warn("Person " + personId + " is leaving link " + linkId + " without having entered. Thus, no emissions are calculated for this link.");
				if (linkLeaveWarnCnt == maxLinkLeaveWarnCnt)
					logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
		}
	}
}