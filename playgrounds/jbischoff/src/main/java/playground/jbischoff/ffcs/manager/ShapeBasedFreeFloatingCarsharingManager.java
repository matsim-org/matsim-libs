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
package playground.jbischoff.ffcs.manager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.jbischoff.ffcs.FFCSConfigGroup;
import playground.jbischoff.ffcs.data.CarsharingData;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ShapeBasedFreeFloatingCarsharingManager implements FreefloatingCarsharingManager {

	private Map<Id<Vehicle>, Id<Link>> idleVehicleLocations = new LinkedHashMap<>();
	private Map<Id<Vehicle>, Id<Link>> busyVehicleLastRentalLocations = new LinkedHashMap<>();
	private CarsharingData data;
	private Network network;
	private final FFCSConfigGroup ffcsConfigGroup;
	private ParkingSearchManager parkingManager;

	@Inject
	public ShapeBasedFreeFloatingCarsharingManager(Network network, ParkingSearchManager parkingManager, CarsharingData data, Config config) {
		this.data=data;
		this.idleVehicleLocations.putAll(data.getVehiclesStartLocations());;
		this.network = network;
		this.parkingManager = parkingManager;
		this.ffcsConfigGroup = (FFCSConfigGroup) config.getModule("freefloating");
		
	}
	
	/* (non-Javadoc)
	 * @see playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager#findAndReserveFreefloatingVehicleForLeg(org.matsim.api.core.v01.population.Leg, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public Tuple<Id<Link>, Id<Vehicle>> findAndReserveFreefloatingVehicleForLeg(Leg leg, Id<Person> personId, double time) {
		
		Coord fromCoord = network.getLinks().get(leg.getRoute().getStartLinkId()).getCoord();
		
		double closestDistance = Double.MAX_VALUE;
		Id<Vehicle> closestVehicle = null;
		
		for (Entry<Id<Vehicle>, Id<Link>> e : this.idleVehicleLocations.entrySet()){
			Coord toCoord = network.getLinks().get(e.getValue()).getCoord();
			double distance = DistanceUtils.calculateSquaredDistance(fromCoord, toCoord);
			if (distance < closestDistance){
				closestVehicle = e.getKey();
				closestDistance = distance;
			}		
		}
		if (closestVehicle!=null){
			if (Math.sqrt(closestDistance)>ffcsConfigGroup.getMaximumWalkDistance()){
				closestVehicle = null;
			}
			else {
				Tuple<Id<Link>, Id<Vehicle>> best = new Tuple<Id<Link>,Id<Vehicle>>(this.idleVehicleLocations.remove(closestVehicle),closestVehicle);
				this.busyVehicleLastRentalLocations.put(best.getSecond(), best.getFirst());
				return best;
			}
		}
		
		return null;
	}


	/* (non-Javadoc)
	 * @see playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager#endRental(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public boolean endRental(Id<Link> linkId, Id<Person> personId, Id<Vehicle> vehicleId, double time) {
		this.busyVehicleLastRentalLocations.remove(vehicleId);
		this.idleVehicleLocations.put(vehicleId, linkId);
		return true;
	}


	/* (non-Javadoc)
	 * @see playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		if (ffcsConfigGroup.resetVehicles()){
			for (Entry<Id<Vehicle>, Id<Link>> vid : idleVehicleLocations.entrySet()){
				this.parkingManager.unParkVehicleHere(vid.getKey(), vid.getValue(), 30*3600);
			}
			this.idleVehicleLocations.clear();
			this.idleVehicleLocations.putAll(data.getVehiclesStartLocations());;
}
		else
		if (!this.busyVehicleLastRentalLocations.isEmpty()){
			Logger.getLogger(getClass()).info("Iteration "+iteration+": "+busyVehicleLastRentalLocations.size()+"  carsharing vehicles are reset to last know position:");
			idleVehicleLocations.putAll(busyVehicleLastRentalLocations);
		}
		busyVehicleLastRentalLocations.clear();
	}
	
	/**
	 * @return the idleVehicleLocations
	 */
	@Override
	public Map<Id<Vehicle>, Id<Link>> getIdleVehicleLocations() {
		return idleVehicleLocations;
	}


	/* (non-Javadoc)
	 * @see playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager#isFFCSVehicle(org.matsim.api.core.v01.Id)
	 */
	@Override
	public boolean isFFCSVehicle(Id<Vehicle> vehicleId) {
	
		return (data.getVehiclesStartLocations().containsKey(vehicleId));
	}



}
