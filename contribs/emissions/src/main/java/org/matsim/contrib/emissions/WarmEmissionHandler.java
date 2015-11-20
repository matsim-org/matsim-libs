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
package org.matsim.contrib.emissions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule.WarmEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
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
public class WarmEmissionHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, 
Wait2LinkEventHandler, VehicleLeavesTrafficEventHandler {
	private static final Logger logger = Logger.getLogger(WarmEmissionHandler.class);

	private final Network network;
	private final Vehicles emissionVehicles;
	private final WarmEmissionAnalysisModule warmEmissionAnalysisModule;
	
	private int linkLeaveCnt = 0;
	private int linkLeaveFirstActWarnCnt = 0;
    private int linkLeaveSomeActWarnCnt = 0;

    private final Map<Id<Person>, Tuple<Id<Link>, Double>> linkenter = new HashMap<>();
	private final Map<Id<Person>, Tuple<Id<Link>, Double>> agentarrival = new HashMap<>();
	private final Map<Id<Person>, Tuple<Id<Link>, Double>> agentdeparture = new HashMap<>();

    private Vehicle2DriverEventHandler delegate;
    
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
		linkLeaveFirstActWarnCnt = 0;
		
		linkenter.clear();
		agentarrival.clear();
		agentdeparture.clear();
		
		warmEmissionAnalysisModule.reset();
		
		delegate.reset(iteration);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(!event.getLegMode().equals("car")){ // link travel time calculation not neccessary for other modes
			return;
		}
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<Id<Link>, Double>(event.getLinkId(), event.getTime());
		this.agentarrival.put(event.getPersonId(), linkId2Time);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(!event.getLegMode().equals("car")){ // link travel time calculation not neccessary for other modes
			return;
		}
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<Id<Link>, Double>(event.getLinkId(), event.getTime());
		this.agentdeparture.put(event.getPersonId(), linkId2Time);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<Id<Link>, Double>(event.getLinkId(), event.getTime());
		this.linkenter.put(delegate.getDriverOfVehicle(event.getVehicleId()), linkId2Time);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		linkLeaveCnt++;
		Id<Person> personId= delegate.getDriverOfVehicle(event.getVehicleId());
		Id<Link> linkId = event.getLinkId();
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

		if(!this.linkenter.containsKey(personId)){
            int maxLinkLeaveFirstActWarnCnt = 3;
            if(linkLeaveFirstActWarnCnt < maxLinkLeaveFirstActWarnCnt){
				logger.info("Person " + personId + " is ending its first activity of the day and leaving link " + linkId + " without having entered.");
				logger.info("This is because of the MATSim logic that there is no link enter event for the link of the first activity");
				logger.info("Thus, no emissions are calculated for this link leave event.");
				if (linkLeaveFirstActWarnCnt == maxLinkLeaveFirstActWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			linkLeaveFirstActWarnCnt++;
        } else if (!this.linkenter.get(personId).getFirst().equals(linkId)){
            int maxLinkLeaveSomeActWarnCnt = 3;
            if(linkLeaveSomeActWarnCnt < maxLinkLeaveSomeActWarnCnt){
				logger.warn("Person " + personId + " is ending an activity other than the first and leaving link " + linkId + " without having entered.");
				logger.warn("This indicates that there is some inconsistency in vehicle use; please check your inital plans file for consistency.");
				logger.warn("Thus, no emissions are calculated neither for this link leave event nor for the last link that was entered.");
				if (linkLeaveSomeActWarnCnt == maxLinkLeaveSomeActWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			linkLeaveSomeActWarnCnt++;
        } else {
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

			Id<Vehicle> vehicleId = Id.create(personId, Vehicle.class);
			String vehicleInformation;
			if(!this.emissionVehicles.getVehicles().containsKey(vehicleId)){
				throw new RuntimeException("No vehicle defined for person " + personId + ". " +
						"Please make sure that requirements for emission vehicles in " + 
						EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
			}
			Vehicle vehicle = this.emissionVehicles.getVehicles().get(vehicleId);
			VehicleType vehicleType = vehicle.getType();
			vehicleInformation = vehicleType.getId().toString();

			Map<WarmPollutant, Double> warmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(
					personId,
					roadType,
					freeVelocity,
					linkLength,
					travelTime,
					vehicleInformation);

			warmEmissionAnalysisModule.throwWarmEmissionEvent(leaveTime, linkId, vehicleId, warmEmissions);
		}
	}

	public int getLinkLeaveCnt() {
		return linkLeaveCnt;
	}
	public int getLinkLeaveWarnCnt() {
		return linkLeaveFirstActWarnCnt;
	}

	public WarmEmissionAnalysisModule getWarmEmissionAnalysisModule(){
		return warmEmissionAnalysisModule;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}
}