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
package playground.jbischoff.parking.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableInt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.jbischoff.parking.ParkingUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class FacilityBasedParkingManager implements ParkingManager {

	Map<Id<Link>,Integer> capacity = new HashMap<>();
	Map<Id<ActivityFacility>,MutableInt> occupation = new HashMap<>();
	Map<Id<ActivityFacility>,ActivityFacility> parkingFacilities;
	Map<Id<Vehicle>, Id<ActivityFacility>> parkingLocations;
	Map<Id<Link>,Set<Id<ActivityFacility>>> facilitiesPerLink;
	Map<Id<Link>,Id<Link>> backLinkId = new HashMap<>();;
	Network network;
	
	/**
	 * 
	 */
	@Inject
	public FacilityBasedParkingManager(Scenario scenario) {
	this.network  = scenario.getNetwork();
	parkingFacilities = scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingUtils.PARKACTIVITYTYPE);
	
	
	for (ActivityFacility fac : this.parkingFacilities.values()){
		Id<Link> linkId = fac.getLinkId();
		Set<Id<ActivityFacility>> parkingOnLink = new HashSet<>();
		if (this.facilitiesPerLink.containsKey(linkId)){
			parkingOnLink = this.facilitiesPerLink.get(linkId);
		}
		parkingOnLink.add(fac.getId());
		this.facilitiesPerLink.put(linkId, parkingOnLink);
		this.occupation.put(fac.getId(), new MutableInt(0));

		
	}
	for (Id<Link> linkId : facilitiesPerLink.keySet()){
		Link link = network.getLinks().get(linkId);
		Link backLink = getBackLink(link);
		if (backLink!=null){
			this.backLinkId.put(linkId, backLink.getId());
		}
	}
	}
		
	
	
	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.ParkingManager#canVehicleParkHere(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id)
	 */
	@Override
	public boolean canVehicleParkHere(Id<Vehicle> vehicleId, Id<Link> linkId) {
		boolean canPark = false;
		Id<Link> backLinkId = (this.backLinkId.containsKey(linkId)?this.backLinkId.get(linkId):null);
		
		if (linkIdHasAvailableParkingForVehicle(linkId,vehicleId)){
			canPark = true;
		}else if (backLinkId!=null){
			if (linkIdHasAvailableParkingForVehicle(backLinkId,vehicleId)){
				canPark = true;
			}	
		}
		
		return canPark;
	}
	
	private boolean linkIdHasAvailableParkingForVehicle(Id<Link> linkId, Id<Vehicle> vid){
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		for (Id<ActivityFacility> fac: parkingFacilitiesAtLink){
			double cap = this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
			if (this.occupation.get(fac).doubleValue()<=cap) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.ParkingManager#getVehicleParkingLocation(org.matsim.api.core.v01.Id)
	 */
	@Override
	public Id<Link> getVehicleParkingLocation(Id<Vehicle> vehicleId) {
		if (this.parkingLocations.containsKey(vehicleId)){
			return this.parkingFacilities.get(this.parkingLocations.get(vehicleId)).getLinkId();
		} else
		return null;
	}

	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.ParkingManager#parkVehicleHere(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		for (Id<ActivityFacility> fac: parkingFacilitiesAtLink){
			double cap = this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
			if (this.occupation.get(fac).doubleValue()<=cap) {
				this.occupation.get(fac).increment();
				this.parkingLocations.put(vehicleId, fac);
				return true;				
			}
		}
		Id<Link> backLinkId = (this.backLinkId.containsKey(linkId)?this.backLinkId.get(linkId):null);
		if (backLinkId!= null){
		Set<Id<ActivityFacility>> parkingFacilitiesAtBackLink = this.facilitiesPerLink.get(backLinkId);
		for (Id<ActivityFacility> fac: parkingFacilitiesAtBackLink){
			double cap = this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
			if (this.occupation.get(fac).doubleValue()<=cap) {
				this.occupation.get(fac).increment();
				this.parkingLocations.put(vehicleId, fac);
				return true;				
			}
		}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.ParkingManager#unParkVehicleHere(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if(!this.parkingLocations.containsKey(vehicleId)){
		return false;}
		else
		{
			Id<ActivityFacility> fac = this.parkingLocations.remove(vehicleId);
			this.occupation.get(fac).decrement();
			return true;
		}
	}
	
	private Link getBackLink(Link link) {
		for (Link outLink : link.getToNode().getOutLinks().values()) {
			if (link.getFromNode().equals(outLink.getToNode())) {
				return outLink;
			}
		}
		return null;
	}



	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.ParkingManager#produceStatistics()
	 */
	@Override
	public List<String> produceStatistics() {
		List<String> stats = new ArrayList<>();
		for (Entry<Id<ActivityFacility>, MutableInt> e :this.occupation.entrySet()){
			Id<Link> linkId = this.parkingFacilities.get(e.getKey()).getLinkId();
			double capacity = this.parkingFacilities.get(e.getKey()).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
			String s = linkId.toString()+";"+e.getKey().toString()+";"+capacity+";"+e.getValue().toString();
			stats.add(s);
		}
		return stats;
	}



	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.ParkingManager#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

}
