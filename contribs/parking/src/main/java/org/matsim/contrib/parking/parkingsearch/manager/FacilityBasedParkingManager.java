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

package org.matsim.contrib.parking.parkingsearch.manager;

import com.google.inject.Inject;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author  jbischoff, schlenther
 *
 */
public class FacilityBasedParkingManager implements ParkingSearchManager {

	protected Map<Id<Link>, Integer> capacity = new HashMap<>();
	protected Map<Id<org.matsim.facilities.Facility>, MutableLong> occupation = new HashMap<>();
	protected 	Map<Id<org.matsim.facilities.Facility>, ActivityFacility> parkingFacilities;
	protected Map<Id<Vehicle>, Id<org.matsim.facilities.Facility>> parkingLocations = new HashMap<>();
	protected Map<Id<Vehicle>, Id<org.matsim.facilities.Facility>> parkingReservation = new HashMap<>();
	protected Map<Id<Vehicle>, Id<Link>> parkingLocationsOutsideFacilities = new HashMap<>();
	protected Map<Id<Link>, Set<Id<org.matsim.facilities.Facility>>> facilitiesPerLink = new HashMap<>();

    protected Network network;

	@Inject
	public FacilityBasedParkingManager(Scenario scenario) {
		this.network = scenario.getNetwork();
		parkingFacilities = scenario.getActivityFacilities()
				.getFacilitiesForActivityType(ParkingUtils.PARKACTIVITYTYPE);
		Logger.getLogger(getClass()).info(parkingFacilities);

		for (ActivityFacility fac : this.parkingFacilities.values()) {
			Id<Link> linkId = fac.getLinkId();
			Set<Id<org.matsim.facilities.Facility>> parkingOnLink = new HashSet<>();
			if (this.facilitiesPerLink.containsKey(linkId)) {
				parkingOnLink = this.facilitiesPerLink.get(linkId);
			}
			parkingOnLink.add(fac.getId());
			this.facilitiesPerLink.put(linkId, parkingOnLink);
			this.occupation.put(fac.getId(), new MutableLong(0));

		}
	}

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
		Set<Id<org.matsim.facilities.Facility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		for (Id<org.matsim.facilities.Facility> fac : parkingFacilitiesAtLink) {
			double cap = this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE)
					.getCapacity();
			if (this.occupation.get(fac).doubleValue() < cap) {
				// Logger.getLogger(getClass()).info("occ:
				// "+this.occupation.get(fac).toString()+" cap: "+cap);
				this.occupation.get(fac).increment();
				this.parkingReservation.put(vid, fac);

				return true;
			}
		}
		return false;
	}

	@Override
	public Id<Link> getVehicleParkingLocation(Id<Vehicle> vehicleId) {
		if (this.parkingLocations.containsKey(vehicleId)) {
			return this.parkingFacilities.get(this.parkingLocations.get(vehicleId)).getLinkId();
		} else if (this.parkingLocationsOutsideFacilities.containsKey(vehicleId)) {
			return this.parkingLocationsOutsideFacilities.get(vehicleId);
		} else
			return null;
	}

	@Override
	public boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
        return parkVehicleAtLink(vehicleId, linkId, time);
	}

	protected boolean parkVehicleAtLink(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		Set<Id<org.matsim.facilities.Facility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		if (parkingFacilitiesAtLink == null) {
			this.parkingLocationsOutsideFacilities.put(vehicleId, linkId);
			return true;
		} else {
			Id<org.matsim.facilities.Facility> fac = this.parkingReservation.remove(vehicleId);
			if (fac != null) {
				this.parkingLocations.put(vehicleId, fac);
				return true;
			} else {
				throw new RuntimeException("no parking reservation found for vehicle " + vehicleId.toString()
						+ "arrival on link " + linkId + " with parking restriction");
			}
		}

	}

	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (!this.parkingLocations.containsKey(vehicleId)) {
			this.parkingLocationsOutsideFacilities.remove(vehicleId);
			return true;
			
			// we assume the person parks somewhere else
		} else {
			Id<org.matsim.facilities.Facility> fac = this.parkingLocations.remove(vehicleId);
			this.occupation.get(fac).decrement();
			return true;
		}
	}

	@Override
	public List<String> produceStatistics() {
		List<String> stats = new ArrayList<>();
		for (Entry<Id<org.matsim.facilities.Facility>, MutableLong> e : this.occupation.entrySet()) {
			Id<Link> linkId = this.parkingFacilities.get(e.getKey()).getLinkId();
			double capacity = this.parkingFacilities.get(e.getKey()).getActivityOptions()
					.get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
			String s = linkId.toString() + ";" + e.getKey().toString() + ";" + capacity + ";" + e.getValue().toString();
			stats.add(s);
		}
		return stats;
	}

	public double getNrOfAllParkingSpacesOnLink (Id<Link> linkId){
		double allSpaces = 0;
		Set<Id<org.matsim.facilities.Facility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		if (!(parkingFacilitiesAtLink == null)) {
			for (Id<org.matsim.facilities.Facility> fac : parkingFacilitiesAtLink){
				allSpaces += this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
			}
		}
		return allSpaces;
	}
	
	public double getNrOfFreeParkingSpacesOnLink (Id<Link> linkId){
		double allFreeSpaces = 0;
		Set<Id<org.matsim.facilities.Facility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		if (parkingFacilitiesAtLink == null) {
			return 0;
		} else {
			for (Id<org.matsim.facilities.Facility> fac : parkingFacilitiesAtLink){
				int cap = (int) this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
				allFreeSpaces += (cap - this.occupation.get(fac).intValue());
			}
		}
		return allFreeSpaces;
	}


	@Override
	public void reset(int iteration) {
	}

	
}
