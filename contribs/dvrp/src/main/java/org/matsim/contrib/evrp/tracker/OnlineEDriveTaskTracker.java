/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.evrp.tracker;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.contrib.evrp.VrpPathEnergyConsumptions;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class OnlineEDriveTaskTracker implements OnlineDriveTaskTracker, ETaskTracker {
	private final EvDvrpVehicle vehicle;
	private final OnlineDriveTaskTracker driveTracker;

	public OnlineEDriveTaskTracker(EvDvrpVehicle vehicle, MobsimTimer timer, OnlineDriveTaskTracker driveTracker) {
		this.vehicle = vehicle;
		this.driveTracker = driveTracker;
	}

	@Override
	public double predictChargeAtEnd() {
		ElectricVehicle ev = vehicle.getElectricVehicle();
		double currentCharge = ev.getBattery().getCharge();
		double driveEnergy = VrpPathEnergyConsumptions.calcRemainingTotalEnergy(ev, getPath(), getCurrentLinkIdx(),
				getCurrentLinkEnterTime());
		return currentCharge - driveEnergy;
	}

	@Override
	public double predictEndTime() {
		return driveTracker.predictEndTime();
	}

	@Override
	public LinkTimePair getDiversionPoint() {
		return driveTracker.getDiversionPoint();
	}

	@Override
	public int getCurrentLinkIdx() {
		return driveTracker.getCurrentLinkIdx();
	}

	@Override
	public double getCurrentLinkEnterTime() {
		return driveTracker.getCurrentLinkEnterTime();
	}

	@Override
	public VrpPath getPath() {
		return driveTracker.getPath();
	}

	@Override
	public void divertPath(VrpPathWithTravelData newSubPath) {
		driveTracker.divertPath(newSubPath);
	}

	@Override
	public void movedOverNode(Link nextLink) {
		driveTracker.movedOverNode(nextLink);
	}
}
