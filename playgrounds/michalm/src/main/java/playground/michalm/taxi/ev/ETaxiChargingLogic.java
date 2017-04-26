/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.ev;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import playground.michalm.ev.charging.FixedSpeedChargingWithQueueingLogic;
import playground.michalm.ev.data.*;
import playground.michalm.taxi.data.EvrpVehicle.Ev;

public class ETaxiChargingLogic extends FixedSpeedChargingWithQueueingLogic {
	// fast charging up to 80% of the battery capacity
	private static final double MAX_RELATIVE_SOC = 0.8;

	private final Map<Id<Vehicle>, ElectricVehicle> assignedVehicles = new HashMap<>();
	private final double effectivePower;

	public ETaxiChargingLogic(Charger charger, double chargingSpeedFactor) {
		super(charger);
		effectivePower = charger.getPower() * chargingSpeedFactor;
	}

	// at this point ETaxiChargingTask should point to Charger
	public void addAssignedVehicle(ElectricVehicle ev) {
		assignedVehicles.put(ev.getId(), ev);
	}

	// on deleting ETaxiChargingTask or vehicle arrival (the veh becomes plugged or queued)
	public void removeAssignedVehicle(ElectricVehicle ev) {
		if (assignedVehicles.remove(ev.getId()) == null) {
			throw new IllegalArgumentException();
		}
	}

	@Override
	protected boolean doStopCharging(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		return b.getSoc() >= MAX_RELATIVE_SOC * b.getCapacity();
	}

	@Override
	protected void notifyVehicleQueued(ElectricVehicle ev, double now) {
		((Ev)ev).getAtChargerActivity().vehicleQueued(now);
	}

	@Override
	protected void notifyChargingStarted(ElectricVehicle ev, double now) {
		((Ev)ev).getAtChargerActivity().chargingStarted(now);
	}

	@Override
	protected void notifyChargingEnded(ElectricVehicle ev, double now) {
		((Ev)ev).getAtChargerActivity().chargingEnded(now);
	}

	public double getEnergyToCharge(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		return Math.max(0, MAX_RELATIVE_SOC * b.getCapacity() - b.getSoc());
	}

	public double estimateChargeTime(ElectricVehicle ev) {
		// System.err.println("energy to charge" + getEnergyToCharge(vehicle));
		// System.err.println("effectivePower = " + effectivePower);
		return getEnergyToCharge(ev) / effectivePower;
	}

	// TODO using task timing from schedules will be more accurate in predicting charge demand

	// does not include further demand (AUX for queued vehs)
	public double estimateMaxWaitTimeOnArrival() {
		if (pluggedVehicles.size() < charger.getPlugs()) {
			return 0;
		}

		double sum = sumEnergyToCharge(pluggedVehicles.values()) + sumEnergyToCharge(queuedVehicles);
		return sum / effectivePower / charger.getPlugs();
	}

	// does not include further demand (AUX for queued vehs; AUX+driving for dispatched vehs)
	public double estimateAssignedWorkload() {
		double total = sumEnergyToCharge(pluggedVehicles.values()) //
				+ sumEnergyToCharge(queuedVehicles) //
				+ sumEnergyToCharge(assignedVehicles.values());
		return total / effectivePower;
	}

	private double sumEnergyToCharge(Iterable<ElectricVehicle> evs) {
		double energyToCharge = 0;
		for (ElectricVehicle ev : evs) {
			energyToCharge += getEnergyToCharge(ev);
		}
		return energyToCharge;
	}

	public int getPluggedCount() {
		return pluggedVehicles.size();
	}

	public int getQueuedCount() {
		return queuedVehicles.size();
	}

	public int getAssignedCount() {
		return assignedVehicles.size();
	}
}
