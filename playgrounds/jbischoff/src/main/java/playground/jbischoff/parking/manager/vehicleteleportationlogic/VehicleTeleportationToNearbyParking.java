/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.parking.manager.vehicleteleportationlogic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.jbischoff.parking.choice.ParkingChoiceLogic;
import playground.jbischoff.parking.choice.RandomParkingChoiceLogic;
import playground.jbischoff.parking.manager.ParkingManager;

/**
 * teleports vehicle to  a location near the agent, if vehicle is more than  a certain threshold in metres away
 * @author  jbischoff
 * 
 */
public class VehicleTeleportationToNearbyParking implements VehicleTeleportationLogic {
	
	private double maximumWalkDistance  = 2000;
	//TODO: Make this configurable
	
	private double beelineDistanceFactor;
	@Inject
	ParkingManager manager;
	
	ParkingChoiceLogic parkingLogic;  
	Network network;
	
	@Inject
	/**
	 * 
	 */
	public VehicleTeleportationToNearbyParking(Config config, Network network) {
		// TODO Auto-generated constructor stub
		this.beelineDistanceFactor = config.plansCalcRoute().getModeRoutingParams().get(TransportMode.walk).getBeelineDistanceFactor();
		this.parkingLogic = new RandomParkingChoiceLogic(network);
		this.network = network;
	}

	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.vehicleteleportationlogic.VehicleTeleportationLogic#getVehicleLocation(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id)
	 */
	@Override
	public Id<Link> getVehicleLocation(Id<Link> agentLinkId, Id<Vehicle> vehicleId, Id<Link> vehicleLinkId, double time) {
		double walkDistance = CoordUtils.calcEuclideanDistance(network.getLinks().get(vehicleLinkId).getCoord(), network.getLinks().get(agentLinkId).getCoord()) * this.beelineDistanceFactor;
		if (walkDistance<=this.maximumWalkDistance){
			return vehicleLinkId;
		} 
		Id<Link> parkingLinkId = agentLinkId;
		while (!this.manager.reserveSpaceIfVehicleCanParkHere(vehicleId, parkingLinkId)){
			parkingLinkId = parkingLogic.getNextLink(parkingLinkId);
		}
		manager.parkVehicleHere(vehicleId, parkingLinkId, time);
		return parkingLinkId;
	}
	
}
