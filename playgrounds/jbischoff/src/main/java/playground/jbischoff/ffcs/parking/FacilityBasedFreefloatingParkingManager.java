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
package playground.jbischoff.ffcs.parking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.jbischoff.ffcs.FFCSConfigGroup;
import playground.jbischoff.ffcs.FFCSUtils;
import playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class FacilityBasedFreefloatingParkingManager implements ParkingSearchManager {

	private final Map<Id<ActivityFacility>, MutableInt> occupancies = new HashMap<>();
	private final Map<Id<ActivityFacility>, MutableInt> csoccupancy = new HashMap<>();
	
	private final Map<Id<ActivityFacility>, ActivityFacility> parkingFacilities= new HashMap<>();
	
	private final Map<Id<Vehicle>, Id<ActivityFacility>> parkingLocations = new HashMap<>();
	private final Map<Id<Vehicle>, Id<ActivityFacility>> parkingReservation = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Link>> parkingLocationsOutsideFacilities = new HashMap<>();
	private final Map<Id<Link>, Set<Id<ActivityFacility>>> facilitiesPerLink = new HashMap<>();
	private final FreefloatingCarsharingManager ffcmanager;
	private final Random random = MatsimRandom.getLocalInstance();
	private final FFCSConfigGroup ffcs;
	private final Set<Id<Vehicle>> parkedInCsFacility = new HashSet<>();
	Network network;

	/**
	 * 
	 */
	@Inject
	public FacilityBasedFreefloatingParkingManager(Scenario scenario, FreefloatingCarsharingManager ffcmanager, Config config) {
		this.ffcmanager = ffcmanager;
		this.network = scenario.getNetwork();
		this.ffcs = (FFCSConfigGroup) config.getModule(FFCSConfigGroup.GROUP_NAME);
		parkingFacilities.putAll(scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingUtils.PARKACTIVITYTYPE));
		parkingFacilities.putAll(scenario.getActivityFacilities().getFacilitiesForActivityType(FFCSUtils.FREEFLOATINGPARKACTIVITYTYPE));

		Logger.getLogger(getClass()).info(parkingFacilities);
		
		

		for (ActivityFacility fac : this.parkingFacilities.values()) {
			Id<Link> linkId = fac.getLinkId();
			Set<Id<ActivityFacility>> parkingOnLink = new HashSet<>();
			if (this.facilitiesPerLink.containsKey(linkId)) {
				parkingOnLink = this.facilitiesPerLink.get(linkId);
			}
			parkingOnLink.add(fac.getId());
			this.facilitiesPerLink.put(linkId, parkingOnLink);
			this.occupancies.put(fac.getId(), new MutableInt(0));
			this.csoccupancy.put(fac.getId(), new MutableInt(0));

		}
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.jbischoff.parking.manager.ParkingManager#canVehicleParkHere(
	 * org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id)
	 */
	@Override
	public boolean reserveSpaceIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<Link> linkId) {
		boolean canPark = false;

		if (linkIdHasAvailableParkingForVehicle(linkId, vehicleId)) {
			canPark = true;
			// Logger.getLogger(getClass()).info("veh: "+vehicleId+" link
			// "+linkId + " can park "+canPark);
		}

		return canPark;
	}

	private boolean linkIdHasAvailableParkingForVehicle(Id<Link> linkId, Id<Vehicle> vid) {
		// Logger.getLogger(getClass()).info("link "+linkId+" vehicle "+vid);
		if (!this.facilitiesPerLink.containsKey(linkId)) {
			// this implies: If no parking facility is present, we suppose that
			// we can park freely (i.e. the matsim standard approach)
			// it also means: a link without any parking spaces should have a
			// parking facility with 0 capacity.
			// Logger.getLogger(getClass()).info("link not listed as parking
			// space, we will say yes "+linkId);

			return true;
		}
		
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		boolean isFFCSVehicle = ffcmanager.isFFCSVehicle(vid);
		for (Id<ActivityFacility> fac : parkingFacilitiesAtLink) {
			double ordinaryCap = (this.parkingFacilities.get(fac).getActivityOptions().containsKey(ParkingUtils.PARKACTIVITYTYPE)?this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity():0);			
			
			double ordinaryOccupancy = this.occupancies.get(fac).doubleValue();
			boolean hasOrdinaryCapacity = false;
			
			if (ordinaryOccupancy< ordinaryCap) {
				// Logger.getLogger(getClass()).info("occ:
				// "+this.occupation.get(fac).toString()+" cap: "+cap);
				if (!isFFCSVehicle){
					this.occupancies.get(fac).increment();
					this.parkingReservation.put(vid, fac);
					return true;
				} else {
					hasOrdinaryCapacity = true;
				}

			}
			if (isFFCSVehicle){
				double ffcsCap = (this.parkingFacilities.get(fac).getActivityOptions().containsKey(FFCSUtils.FREEFLOATINGPARKACTIVITYTYPE)?this.parkingFacilities.get(fac).getActivityOptions().get(FFCSUtils.FREEFLOATINGPARKACTIVITYTYPE).getCapacity():0);			
				double ffcsOccupancy = this.csoccupancy.get(fac).doubleValue();
				boolean hasCSCapacity = false;
				if (ffcsOccupancy<ffcsCap){
					hasCSCapacity = true;
				}
				if (hasCSCapacity && hasOrdinaryCapacity){
					if (random.nextBoolean()){
						hasCSCapacity = false;
						// we force to use ordinary parking
					}
				} else if (hasCSCapacity){
					this.csoccupancy.get(fac).increment();
					this.parkedInCsFacility.add(vid);
					this.parkingReservation.put(vid, fac);
					return true;
				} else if (hasOrdinaryCapacity){
					this.occupancies.get(fac).increment();
					this.parkingReservation.put(vid, fac);
					return true;
				}

			}
			
		}
		
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.jbischoff.parking.manager.ParkingManager#
	 * getVehicleParkingLocation(org.matsim.api.core.v01.Id)
	 */
	@Override
	public Id<Link> getVehicleParkingLocation(Id<Vehicle> vehicleId) {
		if (this.parkingLocations.containsKey(vehicleId)) {
			return this.parkingFacilities.get(this.parkingLocations.get(vehicleId)).getLinkId();
		} else if (this.parkingLocationsOutsideFacilities.containsKey(vehicleId)) {
			return this.parkingLocationsOutsideFacilities.get(vehicleId);
		} else
			return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.jbischoff.parking.manager.ParkingManager#parkVehicleHere(org.
	 * matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (parkVehicleAtLink(vehicleId, linkId, time)) {
			return true;
		} else
			return false;
	}

	/**
	 * @param vehicleId
	 * @param linkId
	 * @param time
	 */
	private boolean parkVehicleAtLink(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		if (parkingFacilitiesAtLink == null) {
			this.parkingLocationsOutsideFacilities.put(vehicleId, linkId);
			return true;
		} else {
			Id<ActivityFacility> fac = this.parkingReservation.remove(vehicleId);
			if (fac != null) {
				this.parkingLocations.put(vehicleId, fac);
				return true;
			} else {
				throw new RuntimeException("no parking reservation found for vehicle " + vehicleId.toString()
						+ "arrival on link " + linkId + " with parking restriction");
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.jbischoff.parking.manager.ParkingManager#unParkVehicleHere(org
	 * .matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (!this.parkingLocations.containsKey(vehicleId)) {
			return true;
			// we assume the person parks somewhere else
		} else {
			Id<ActivityFacility> fac = this.parkingLocations.remove(vehicleId);
			if (parkedInCsFacility.contains(vehicleId)){
				parkedInCsFacility.remove(vehicleId);
				this.csoccupancy.get(fac).decrement();
			}
			else{
			this.occupancies.get(fac).decrement();
			}
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.jbischoff.parking.manager.ParkingManager#produceStatistics()
	 */
	@Override
	public List<String> produceStatistics() {
		List<String> stats = new ArrayList<>();
		for (Entry<Id<ActivityFacility>, MutableInt> e : this.occupancies.entrySet()) {
			Id<Link> linkId = this.parkingFacilities.get(e.getKey()).getLinkId();
			double capacity = this.parkingFacilities.get(e.getKey()).getActivityOptions()
					.get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
			double ffcapacity = 0;
			try {
			ffcapacity = this.parkingFacilities.get(e.getKey()).getActivityOptions()
					.get(FFCSUtils.FREEFLOATINGPARKACTIVITYTYPE).getCapacity();}
			catch (NullPointerException ex){
			
			} ;
			int ffocupancy = this.csoccupancy.get(e.getKey()).intValue();
			
			String s = linkId.toString() + ";" + e.getKey().toString() + ";" + capacity + ";" + e.getValue().toString()+";"+ffcapacity+";"+ffocupancy;
			stats.add(s);
		}
		return stats;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.jbischoff.parking.manager.ParkingManager#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}
	
	ActivityFacility findNearestFreeCarsharingParkingFacility(Id<Link> linkId){
		double bestSquareDist = Double.MAX_VALUE;
		ActivityFacility bestFac = null;
		Coord linkCoord = network.getLinks().get(linkId).getCoord();
		for (Entry<Id<ActivityFacility>, MutableInt> e : this.csoccupancy.entrySet()){
			if (e.getValue().intValue()>0){
				ActivityFacility fac = this.parkingFacilities.get(e.getKey()); 
				Coord facCoord = fac.getCoord();
				double dist = DistanceUtils.calculateSquaredDistance(facCoord, linkCoord);
				if (dist<bestSquareDist){
					bestSquareDist = dist;
					bestFac = fac;
				}
			}
		}
		if (bestSquareDist <= Math.pow(ffcs.getMaximumWalkDistance(),2)){
			return bestFac;
		} else	return null;
		
	}

}
