/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.transEnergySim.charging;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPowerAtActivity;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPowerAtLink;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;

/**
 * This module is not compatible with parking search, but should be with parking
 * choice. TODO: when those modules are finished, check if this is really a
 * concern.
 * 
 * 
 * @author wrashid
 * 
 */
public class ChargingUponArrival implements ActivityStartEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler {

	// TODO: add location filter, which should be adressable through config and
	// programmatically
	// TODO: one should be able to turn this mode on.

	// TODO: perhaps also allow to specify, what kind of charging is available
	// where: e.g. plug or contactless or genral.
	// TODO: these things are more relevant, with parking choice and search.

	// TODO: perhaps even make own controler for this?

	// TODO: allow to define the share of each type of vehicle from config - or
	// at least the code.

	// TODO: energy consumption per link auch genau fuehren (in separate file).

	HashMap<Id, VehicleWithBattery> chargableVehicles;

	DoubleValueHashMap<Id> firstDepartureTimeOfDay;
	DoubleValueHashMap<Id> previousCarArrivalTime;

	HashMap<Id, String> firstActivityTypeAfterCarArrival;

	HashMap<Id, Id> previousCarArrivalLinkId;

	Object chargingPower;

	public ChargingUponArrival(HashMap<Id, VehicleWithBattery> chargableVehicles, Object chargingPower) {
		this.chargableVehicles = chargableVehicles;
		this.chargingPower = chargingPower;
	}

	@Override
	public void reset(int iteration) {
		firstDepartureTimeOfDay = new DoubleValueHashMap<Id>();
		previousCarArrivalTime = new DoubleValueHashMap<Id>();
		firstActivityTypeAfterCarArrival = new HashMap<Id, String>();
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Id personId = event.getPersonId();

		if (event.getLegMode().equals(TransportMode.car) && chargableVehicles.containsKey(personId)) {
			if (isFirstCarDepartureOfDay(personId)) {
				firstDepartureTimeOfDay.put(personId, event.getTime());
			} else {
				double carArrivalTime = previousCarArrivalTime.get(personId);
				double carDepartureTime = event.getTime();

				chargeVehicle(personId, carArrivalTime, carDepartureTime);
			}
		}
	}

	private void chargeVehicle(Id personId, double carArrivalTime, double carDepartureTime) {

		double parkingDuration = GeneralLib.getIntervalDuration(carArrivalTime, carDepartureTime);
		double availablePowerInWatt = 0;
		String activityType = firstActivityTypeAfterCarArrival.get(personId);
		if (chargingPower instanceof ChargingPowerAtActivity) {
			availablePowerInWatt = ((ChargingPowerAtActivity) chargingPower).getAvailableChargingPower(activityType);
		} else if (chargingPower instanceof ChargingPowerAtLink) {
			availablePowerInWatt = ((ChargingPowerAtLink) chargingPower).getAvailableChargingPowerInWatt(previousCarArrivalLinkId
					.get(personId));
		}

		double chargableEnergyInJoules = availablePowerInWatt * parkingDuration;
		
		VehicleWithBattery vehicleWithBattery = chargableVehicles.get(personId);
		double energyToChargeInJoules = 0;
		if (vehicleWithBattery.getRequiredEnergyInJoules() <= chargableEnergyInJoules) {
			energyToChargeInJoules = vehicleWithBattery.getRequiredEnergyInJoules();
		} else {
			energyToChargeInJoules = chargableEnergyInJoules;
		}
		vehicleWithBattery.chargeBattery(energyToChargeInJoules);
		// TODO: log this properly! (who charged, where, how much
		// energy, start time of charging.)
	}

	private boolean isFirstCarDepartureOfDay(Id personId) {
		return !firstDepartureTimeOfDay.containsKey(personId);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			initFirstActivityTypeAfterCarArrival(event);
			updateCarArrivalTime(event);
			updatePreviousCarArrivalLinkId(event);
		}
	}

	private void updatePreviousCarArrivalLinkId(AgentArrivalEvent event) {
		previousCarArrivalLinkId.put(event.getPersonId(), event.getLinkId());
	}

	private void updateCarArrivalTime(AgentArrivalEvent event) {
		previousCarArrivalTime.put(event.getPersonId(), event.getTime());
	}

	private void initFirstActivityTypeAfterCarArrival(AgentArrivalEvent event) {
		firstActivityTypeAfterCarArrival.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		updateFirstActivityTypeAfterCarArrival(event);
	}

	private void updateFirstActivityTypeAfterCarArrival(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		if (!firstActivityTypeAfterCarArrival.containsKey(personId)) {
			firstActivityTypeAfterCarArrival.put(personId, event.getActType());
		}
	}

	public void handleLastParkingOfDay() {
		for (Id personId : chargableVehicles.keySet()) {
			double carArrivalTime = previousCarArrivalTime.get(personId);
			double carDepartureTime = firstDepartureTimeOfDay.get(personId);

			chargeVehicle(personId, carArrivalTime, carDepartureTime);
		}
	}

}
