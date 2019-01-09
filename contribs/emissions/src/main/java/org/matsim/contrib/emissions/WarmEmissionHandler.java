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
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule.WarmEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;


/**
 * @author benjamin
 *
 */
public class WarmEmissionHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {
	private static final Logger logger = Logger.getLogger(WarmEmissionHandler.class);

	private final Network network;
	private final Vehicles emissionVehicles;
	private final WarmEmissionAnalysisModule warmEmissionAnalysisModule;

	private int linkLeaveCnt = 0;
	private int linkLeaveFirstActWarnCnt = 0;
	private int linkLeaveSomeActWarnCnt = 0;
	private int zeroLinkLengthWarnCnt = 0;
	private int nonCarWarn = 0;
	private int noVehWarnCnt = 0;

	private final Map<Id<Vehicle>, Tuple<Id<Link>, Double>> linkenter = new HashMap<>();
	private final Map<Id<Vehicle>, Tuple<Id<Link>, Double>> vehicleLeavesTraffic = new HashMap<>();
	private final Map<Id<Vehicle>, Tuple<Id<Link>, Double>> vehicleEntersTraffic = new HashMap<>();

	public WarmEmissionHandler(
			Vehicles emissionVehicles,
			final Network network,
			WarmEmissionAnalysisModuleParameter parameterObject,
			EventsManager emissionEventsManager, Double emissionEfficiencyFactor) {

		this.emissionVehicles = emissionVehicles;
		this.network = network;
		this.warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(parameterObject, emissionEventsManager, emissionEfficiencyFactor);
		// add event handlers here and restrict the access outside the emission Module.  Amit Apr'17.
		emissionEventsManager.addHandler(this);
	}

	@Override
	public void reset(int iteration) {
		linkLeaveCnt = 0;
		linkLeaveFirstActWarnCnt = 0;

		linkenter.clear();
		vehicleLeavesTraffic.clear();
		vehicleEntersTraffic.clear();

		warmEmissionAnalysisModule.reset();
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if(!event.getNetworkMode().equals("car")){
			if( nonCarWarn <=1) {
				logger.warn("non-car modes are supported, however, not properly tested yet.");
				logger.warn(Gbl.ONLYONCE);
				nonCarWarn++;
			}
		}
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<Id<Link>, Double>(event.getLinkId(), event.getTime());
		this.vehicleLeavesTraffic.put(event.getVehicleId(), linkId2Time);

		// yyyyyy This event should also trigger an emissions calculation, from link entry up to here.  Probably not done since this particular
		// event did not exist when the emissions contrib was programmed.  Would be easy to do: calculate the emission and remove
		// the vehicle from the linkenter data structure so that no second emission event is computed for travel from parking to
		// link leave.  (This could also be done, but the excellent should not be in the way of the good.)  kai, may'16
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if(!event.getNetworkMode().equals("car")){
			if( nonCarWarn <=1) {
				logger.warn("non-car modes are supported, however, not properly tested yet.");
				logger.warn(Gbl.ONLYONCE);
				nonCarWarn++;
			}
		}
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<Id<Link>, Double>(event.getLinkId(), event.getTime());
		this.vehicleEntersTraffic.put(event.getVehicleId(), linkId2Time);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<Id<Link>, Double>(event.getLinkId(), event.getTime());
		this.linkenter.put(event.getVehicleId(), linkId2Time);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		Id<Link> linkId = event.getLinkId();
		Double leaveTime = event.getTime();
		Link link = (Link) this.network.getLinks().get(linkId);
		Double linkLength = link.getLength();

		if (linkLength == 0.) {
			if (zeroLinkLengthWarnCnt == 0 ){
				logger.warn("Length of the link "+ linkId + " is zero. No emissions will be estimated for this link. Make sure, this is intentional.");
				logger.warn(Gbl.ONLYONCE);
				zeroLinkLengthWarnCnt++;
			}
			return;
		}

		// excluding links with zero lengths from leaveCnt. Amit July'17
		linkLeaveCnt++;

		if(!this.linkenter.containsKey(vehicleId)){
			int maxLinkLeaveFirstActWarnCnt = 3;
			if(linkLeaveFirstActWarnCnt < maxLinkLeaveFirstActWarnCnt){
				logger.info("Vehicle " + vehicleId + " is ending its first activity of the day and leaving link " + linkId + " without having entered.");
				logger.info("This is because of the MATSim logic that there is no link enter event for the link of the first activity");
				logger.info("Thus, no emissions are calculated for this link leave event.");
				if (linkLeaveFirstActWarnCnt == maxLinkLeaveFirstActWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			linkLeaveFirstActWarnCnt++;
		} else if (!this.linkenter.get(vehicleId).getFirst().equals(linkId)){
			int maxLinkLeaveSomeActWarnCnt = 3;
			if(linkLeaveSomeActWarnCnt < maxLinkLeaveSomeActWarnCnt){
				logger.warn("Vehicle " + vehicleId + " is ending an activity other than the first and leaving link " + linkId + " without having entered.");
				logger.warn("This indicates that there is some inconsistency in vehicle use; please check your inital plans file for consistency.");
				logger.warn("Thus, no emissions are calculated neither for this link leave event nor for the last link that was entered.");
				if (linkLeaveSomeActWarnCnt == maxLinkLeaveSomeActWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			linkLeaveSomeActWarnCnt++;
		} else {
			double enterTime = this.linkenter.get(vehicleId).getSecond();
			double travelTime;
			if(!this.vehicleLeavesTraffic.containsKey(vehicleId) || !this.vehicleEntersTraffic.containsKey(vehicleId)){
				travelTime = leaveTime - enterTime;
			}
			else if(!this.vehicleLeavesTraffic.get(vehicleId).getFirst().equals(event.getLinkId())
					|| !this.vehicleEntersTraffic.get(vehicleId).getFirst().equals(event.getLinkId())){

				travelTime = leaveTime - enterTime;
			} else {
				double arrivalTime = this.vehicleLeavesTraffic.get(vehicleId).getSecond();		
				double departureTime = this.vehicleEntersTraffic.get(vehicleId).getSecond();	
				travelTime = leaveTime - enterTime - departureTime + arrivalTime;	
			}

			if (!this.emissionVehicles.getVehicles().containsKey(vehicleId)) {
				if (this.warmEmissionAnalysisModule.getEcg().getNonScenarioVehicles().equals(NonScenarioVehicles.abort)) {
					throw new RuntimeException(
							"No vehicle defined for id " + vehicleId + ". " +
							"Please make sure that requirements for emission vehicles in " + EmissionsConfigGroup.GROUP_NAME + " config group are met."
									+ " Or set the parameter + 'nonScenarioVehicles' to 'ignore' in order to skip such vehicles."
									+ " Aborting...");
				} else if (this.warmEmissionAnalysisModule.getEcg().getNonScenarioVehicles().equals(NonScenarioVehicles.ignore)) {
					if (noVehWarnCnt < 10) {
						logger.warn(
								"No vehicle defined for id " + vehicleId + ". The vehicle will be ignored.");
						noVehWarnCnt++;
						if (noVehWarnCnt == 10) logger.warn(Gbl.FUTURE_SUPPRESSED);
					}
				} else {
					throw new RuntimeException("Not yet implemented. Aborting...");
				}
								
			} else {
				Vehicle vehicle = this.emissionVehicles.getVehicles().get(vehicleId);

			Map<String, Double> warmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(
					vehicle,
					link,
					travelTime);

				warmEmissionAnalysisModule.throwWarmEmissionEvent(leaveTime, linkId, vehicleId, warmEmissions);
			}
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
}