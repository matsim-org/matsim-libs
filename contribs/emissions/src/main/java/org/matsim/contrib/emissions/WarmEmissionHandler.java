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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * @author benjamin
 *
 */
class WarmEmissionHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {
	private static final Logger logger = Logger.getLogger(WarmEmissionHandler.class);

	private final WarmEmissionAnalysisModule warmEmissionAnalysisModule;
	private final Scenario scenario;
	private final EmissionsConfigGroup emissionsConfigGroup;

	private int linkLeaveCnt = 0;
	private int linkLeaveFirstActWarnCnt = 0;
	private int linkLeaveSomeActWarnCnt = 0;
	private int zeroLinkLengthWarnCnt = 0;
	private int nonCarWarn = 0;
	private int noVehWarnCnt = 0;

	private final Map<Id<Vehicle>, Tuple<Id<Link>, Double>> linkEnterMap = new HashMap<>();
//	private final Map<Id<Vehicle>, Tuple<Id<Link>, Double>> vehicleLeavesTraffic = new HashMap<>(); // not needed anymore ~rjg 07.22
	private final Map<Id<Vehicle>, Tuple<Id<Link>, Double>> vehicleEntersTrafficMap = new HashMap<>();

	/*package-private*/ WarmEmissionHandler( Scenario scenario, Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable,
											 Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable,
											 Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds, Set<Pollutant> warmPollutants,
											 EventsManager eventsManager ){

		this.scenario = scenario;
		this.emissionsConfigGroup = ConfigUtils.addOrGetModule( scenario.getConfig(), EmissionsConfigGroup.class );

		this.warmEmissionAnalysisModule = new WarmEmissionAnalysisModule( avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds,
				warmPollutants, eventsManager, ConfigUtils.addOrGetModule( scenario.getConfig(), EmissionsConfigGroup.class) );

		eventsManager.addHandler( this );
	}

	@Override
	public void reset(int iteration) {
		linkLeaveCnt = 0;
		linkLeaveFirstActWarnCnt = 0;

		linkEnterMap.clear();
		vehicleEntersTrafficMap.clear();

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

		// extract event details
		final double leaveTime = event.getTime();
		Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
		final Id<Vehicle> vehicleId = event.getVehicleId();

		double travelTime = leaveTime - this.linkEnterMap.get(vehicleId).getSecond() + 1.0;

		// match vehicleId to scenario and test for non-scenario vehicles
		// if vehicle type is defined calculate warm emissions
		Vehicle vehicle = VehicleUtils.findVehicle(vehicleId, scenario);

		if (vehicle != null) {
			// execute emissions calculation method
			emissionsCalculation(vehicleId, vehicle, link, leaveTime, travelTime );
			// this extra second added to travel time is because the vehicleLeavesTrafficEvent is thrown one second earlier (by design)

			this.vehicleEntersTrafficMap.remove(vehicleId);
			this.linkEnterMap.remove(vehicleId);
			// this is "so that no second emission event is computed for travel from parking to link leave" (kn)
			// because after this vehicleLeavesTrafficEvent there can/will be another enterTraffic, linkEnter
			// (and then later leaveTraffic as well)...
		} else {
			handleNullVehicle(vehicleId);
		}

}


	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (!event.getNetworkMode().equals("car")) {
			if (nonCarWarn <= 1) {
				logger.warn("non-car modes are supported, however, not properly tested yet.");
				logger.warn(Gbl.ONLYONCE);
				nonCarWarn++;
			}
		}
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<>(event.getLinkId(), event.getTime());
		this.vehicleEntersTrafficMap.put(event.getVehicleId(), linkId2Time);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<>(event.getLinkId(), event.getTime());
		this.linkEnterMap.put(event.getVehicleId(), linkId2Time);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {

		// extract event details
		Id<Vehicle> vehicleId = event.getVehicleId();
		Id<Link> linkId = event.getLinkId();
		Link link = this.scenario.getNetwork().getLinks().get(linkId);
		double linkLength = link.getLength();
		double leaveTime = event.getTime();

		warnIfZeroLinkLength(linkId, linkLength);

		// excluding links with zero lengths from leaveCnt. Amit July'17
		linkLeaveCnt++;

		if (!this.linkEnterMap.containsKey(vehicleId)) { // vehicle has NOT entered this link
			int maxLinkLeaveFirstActWarnCnt = 3;
			if (linkLeaveFirstActWarnCnt < maxLinkLeaveFirstActWarnCnt) {
				logger.info("Vehicle " + vehicleId + " is ending its first activity of the day and leaving link " + linkId + " without having entered.");
				logger.info("This is because of the MATSim logic that there is no link enter event for the link of the first activity");
				logger.info("Thus, no emissions are calculated for this link leave event.");
				if (linkLeaveFirstActWarnCnt == maxLinkLeaveFirstActWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			linkLeaveFirstActWarnCnt++;
		} else if (!this.linkEnterMap.get(vehicleId).getFirst().equals(linkId)) { // vehicle HAS entered a link but not THIS one
			int maxLinkLeaveSomeActWarnCnt = 3;
			if (linkLeaveSomeActWarnCnt < maxLinkLeaveSomeActWarnCnt) {
				logger.warn("Vehicle " + vehicleId + " is ending an activity other than the first and leaving link " + linkId + " without having entered.");
				logger.warn("This indicates that there is some inconsistency in vehicle use; please check your inital plans file for consistency.");
				logger.warn("Thus, no emissions are calculated neither for this link leave event nor for the last link that was entered.");
				if (linkLeaveSomeActWarnCnt == maxLinkLeaveSomeActWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			linkLeaveSomeActWarnCnt++;
		} else {
			// the vehicle traversed the entire link, DO calculate emissions IF this was not after an activity...
			double travelTime = leaveTime - this.linkEnterMap.get(vehicleId).getSecond();

			if (this.linkEnterMap.containsKey(vehicleId)) {
				{
					// match vehicleId to scenario and test for non-scenario vehicles
					// if vehicle type is defined calculate warm emissions
					Vehicle vehicle = VehicleUtils.findVehicle(vehicleId, scenario);

					if (vehicle != null) {
						// execute emissions calculation method
						emissionsCalculation(vehicleId, vehicle, link, leaveTime, travelTime);
						// remove vehicle from data structure (no activity on this link, thus not removing from vehicleEnters/LeavesTraffic)
						this.linkEnterMap.remove(vehicleId);
						// because after this linkLeaveEvent there can/will be another linkEnterEvent
					} else {
						handleNullVehicle(vehicleId);
					}
				}
			}


		}
	}

	private void emissionsCalculation(Id<Vehicle> vehicleId, Vehicle vehicle, Link link, double leaveTime, double travelTime) {
		// warm emissions calculation
		VehicleType vehicleType = vehicle.getType();
		Map<Pollutant, Double> warmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(vehicleType, vehicleId, link, travelTime);
		warmEmissionAnalysisModule.throwWarmEmissionEvent(leaveTime, link.getId(), vehicleId, warmEmissions);
	}

	private void warnIfZeroLinkLength(Id<Link> linkId, double linkLength) {
		if (linkLength == 0.) {
			if (zeroLinkLengthWarnCnt == 0) {
				logger.warn("Length of the link " + linkId + " is zero. No emissions will be estimated for this link. Make sure, this is intentional.");
				logger.warn(Gbl.ONLYONCE);
				zeroLinkLengthWarnCnt++;
			}
		}
	}

	private void handleNullVehicle(Id<Vehicle> vehicleId) {

		ColdEmissionHandler.handleNullVehicleECG(vehicleId, emissionsConfigGroup);

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
	}

	/*package-private*/ int getLinkLeaveCnt() {
		return linkLeaveCnt;
	}

	/*package-private*/ int getLinkLeaveWarnCnt() {
		return linkLeaveFirstActWarnCnt;
	}

	/*package-private*/ WarmEmissionAnalysisModule getWarmEmissionAnalysisModule(){
		return warmEmissionAnalysisModule;
	}
}
