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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private static final Logger logger = LogManager.getLogger(WarmEmissionHandler.class);

	private final WarmEmissionAnalysisModule warmEmissionAnalysisModule;
	private final Scenario scenario;
	private final EmissionsConfigGroup emissionsConfigGroup;

	private int linkLeaveCnt = 0;
	private int linkLeaveFirstActWarnCnt = 0;
	private int zeroLinkLengthWarnCnt = 0;
	private int nonCarWarn = 0;
	private int noVehWarnCnt = 0;
	private int trafficLeaveCnt = 0;
	private int sameLinkTrafficLeaveWarnCnt = 0;
	private int unusualTrafficLeaveWarnCnt = 0;

	private final Map<Id<Vehicle>, Tuple<Id<Link>, Double>> linkEnterMap = new HashMap<>();
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
		trafficLeaveCnt = 0;
		sameLinkTrafficLeaveWarnCnt = 0;
		unusualTrafficLeaveWarnCnt = 0;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if(!event.getNetworkMode().equals("car")){
			if( nonCarWarn <=1) {
				logger.warn("non-car modes are supported, however, not properly tested yet.");
				logger.warn(Gbl.ONLYONCE);
				nonCarWarn++; }
		}
		Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
		if ( linkLengthIsZero( link.getId(), link.getLength() ) ) { trafficLeaveCnt++; }

		final double leaveTime = event.getTime();
		final Id<Vehicle> vehicleId = event.getVehicleId();
		if ( this.linkEnterMap.containsKey( vehicleId ) ) {
			double travelTime = leaveTime - this.linkEnterMap.get(vehicleId).getSecond() + 1.0;
			// this extra second added to travelTime is needed because the vehicleLeavesTrafficEvent
			// is thrown one second earlier (by design) ~kn,kmt,rjg 08.22
			Vehicle vehicle = VehicleUtils.findVehicle(vehicleId, scenario);
			if (vehicle != null) {
				emissionsCalculation( vehicleId, vehicle, link, leaveTime, travelTime );
				this.vehicleEntersTrafficMap.remove( vehicleId );
				this.linkEnterMap.remove( vehicleId );
				// clearing these maps so that no second emission event is computed for travel from parking to link leave
			} else {
				handleNullVehicle(vehicleId);
			}
		} else if ( this.vehicleEntersTrafficMap.containsKey( vehicleId ) ) {
			logger.warn("At time {}, vehicle {} enters and leaves traffic without having entered link {}. Thus, no emissions are calculated for travel along this link.", event.getTime(), event.getVehicleId(), event.getLinkId());
			sameLinkTrafficLeaveWarnCnt++;
		} else {
			logger.warn("At time {}, vehicle {} left traffic without entering traffic or ANY link. Thus, no emissions are calculated for this unusual event.", event.getTime(), event.getVehicleId());
			unusualTrafficLeaveWarnCnt++;
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

		Id<Vehicle> vehicleId = event.getVehicleId();
		Id<Link> linkId = event.getLinkId();
		Link link = this.scenario.getNetwork().getLinks().get(linkId);
		double linkLength = link.getLength();
		double leaveTime = event.getTime();

		if ( !linkLengthIsZero( linkId, linkLength ) ) { linkLeaveCnt++; }
		// excluding links with zero lengths from leaveCnt. Amit July'17

		if (this.linkEnterMap.containsKey( vehicleId )) {
			Tuple<Id<Link>, Double> linkEnterMapEntry = this.linkEnterMap.get( vehicleId );
			if (linkEnterMapEntry.getFirst().equals( linkId )) {
				// the vehicle traversed the entire link, calculate emissions if this was not after an activity
				double travelTime = leaveTime - this.linkEnterMap.get(vehicleId).getSecond();
				Vehicle vehicle = VehicleUtils.findVehicle(vehicleId, scenario);
				if (vehicle != null) {
					emissionsCalculation(vehicleId, vehicle, link, leaveTime, travelTime);
					// remove vehicle from data structure (no activity on this link, thus not removing from vehicleEnters/LeavesTraffic)
					this.linkEnterMap.remove(vehicleId);
				} else {
					handleNullVehicle(vehicleId);
				}
			}
		} else { linkLeaveFirstActWarnCnt++; }

	}

	private void emissionsCalculation(Id<Vehicle> vehicleId, Vehicle vehicle, Link link, double leaveTime, double travelTime) {
		VehicleType vehicleType = vehicle.getType();
		Map<Pollutant, Double> warmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(vehicleType, vehicleId, link, travelTime);
		warmEmissionAnalysisModule.throwWarmEmissionEvent(leaveTime, link.getId(), vehicleId, warmEmissions);
	}

	private boolean linkLengthIsZero( Id<Link> linkId, double linkLength) {
		if (linkLength == 0.) {
			if (zeroLinkLengthWarnCnt == 0) {
				logger.warn("Length of the link {} is zero. No emissions will be estimated for this link. Make sure that this is intentional.", linkId);
				logger.warn(Gbl.ONLYONCE);
				zeroLinkLengthWarnCnt++;
			}
			return true;
		} else { return false; }
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
				logger.warn("No vehicle defined for id {}. The vehicle will be ignored.", vehicleId);
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
	/*package-private*/ int getSameLinkTrafficLeaveWarnCnt() {
		return sameLinkTrafficLeaveWarnCnt;
	}
	/*package-private*/ int getUnusualTrafficLeaveWarnCnt() {
		return unusualTrafficLeaveWarnCnt;
	}
	/*package-private*/ int getTrafficLeaveCnt() {
		return trafficLeaveCnt;
	}
}
