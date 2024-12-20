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

import jakarta.inject.Inject;
import org.apache.commons.lang3.mutable.MutableLong;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * @author jbischoff
 */
public class LinkLengthBasedParkingManagerWithRandomInitialUtilisation implements ParkingSearchManager {

	Map<Id<Link>, Integer> capacity = new HashMap<>();
	Map<Id<Link>, MutableLong> occupation = new HashMap<>();
	Map<Id<Vehicle>, Id<Link>> parkingPosition = new HashMap<>();
	Random rand = MatsimRandom.getLocalInstance();
	int parkedVehicles = 0;
	int unparkedVehicles = 0;

	@Inject
	private ParkingStatsWriter writer;

	@Inject
	public LinkLengthBasedParkingManagerWithRandomInitialUtilisation(Network network, Config config) {
		double assumedParkedVehicleLength = 4.0;
		double shareOfLinkLengthUsedForParking = 0.7;

		//TODO: Make this configurable
		for (Link link : network.getLinks().values()) {
			int maxCapacity = (int) (link.getLength() * shareOfLinkLengthUsedForParking / assumedParkedVehicleLength);
			this.capacity.put(link.getId(), maxCapacity);
			this.occupation.put(link.getId(), new MutableLong(rand.nextInt(maxCapacity)));

		}
	}

	@Override
	public boolean reserveSpaceIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<Link> linkId) {
		return (this.occupation.get(linkId).intValue() < this.capacity.get(linkId)) ? true : false;

	}

	@Override
	public Id<Link> getVehicleParkingLocation(Id<Vehicle> vehicleId) {

		return this.parkingPosition.get(vehicleId);
	}

	@Override
	public boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (this.occupation.get(linkId).intValue() < this.capacity.get(linkId)) {
			this.occupation.get(linkId).increment();
			this.parkingPosition.put(vehicleId, linkId);
			parkedVehicles++;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (!linkId.equals(this.parkingPosition.get(vehicleId))) {
			return false;
		} else {
			this.parkingPosition.remove(vehicleId);
			this.occupation.get(linkId).decrement();
			unparkedVehicles++;
			return true;
		}

	}

	public List<String> produceStatistics() {
		List<String> stats = new ArrayList<>();
		stats.add("parking occurences: " + parkedVehicles);
		stats.add("unparking occurences: " + unparkedVehicles);
		return stats;
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		writer.writePlainStats(produceStatistics(), event.getIteration());
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {

	}
}
