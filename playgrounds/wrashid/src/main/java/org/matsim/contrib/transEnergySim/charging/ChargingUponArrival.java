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
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingLogRow;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingOutputLog;
import org.matsim.contrib.transEnergySim.analysis.charging.StationaryChargingOutput;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPowerAtActivity;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPowerAtLink;
import org.matsim.contrib.transEnergySim.controllers.AddHandlerAtStartupControler;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityOption;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;

/**
 * This module is not compatible with parking search, but should be with parking
 * choice. TODO: when those modules are finished, check if this is really a
 * concern.
 * 
 * 
 * @author wrashid
 * 
 */
public class ChargingUponArrival implements ActivityStartEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler,
		AfterMobsimListener, StartupListener {

	// TODO: add location filter, which should be adressable through config and
	// programmatically
	// TODO: one should be able to turn this mode on.

	// TODO: also provide option (e.g. in separate class), which restricts this
	// to facilities, as this is especially interesting
	// for parking facilities. in this case, we could also limit the number of
	// parking, which are available with
	// such a parking option.

	// TODO: perhaps also allow to specify, what kind of charging is available
	// where: e.g. plug or contactless or genral.
	// TODO: these things are more relevant, with parking choice and search.

	// TODO: perhaps even make own controler for this?

	// TODO: allow to define the share of each type of vehicle from config - or
	// at least the code.

	// TODO: energy consumption per link auch genau fuehren (in separate file).

	// TODO: perhaps provide an adapted version of this with parking also

	private ChargingOutputLog log;

	private DoubleValueHashMap<String> chargablePowerAtActivityTypes;

	HashMap<Id, Vehicle> vehicles;

	DoubleValueHashMap<Id> firstDepartureTimeOfDay;
	DoubleValueHashMap<Id> previousCarArrivalTime;

	HashMap<Id, String> firstActivityTypeAfterCarArrival;
	HashMap<Id, Id> firstFacilityIdAfterCarArrival;

	HashMap<Id, Id> previousCarArrivalLinkId;

	private PowerAvalabilityParameters powerAvalabilityParameters;

	public ChargingUponArrival(HashMap<Id, Vehicle> vehicles, 
			AddHandlerAtStartupControler controller) {
		this.vehicles = vehicles;
		this.setDefaultValues(chargablePowerAtActivityTypes);
		controller.addControlerListener(this);
		chargablePowerAtActivityTypes=new DoubleValueHashMap<String>();
	}

	@Override
	public void reset(int iteration) {
		firstDepartureTimeOfDay = new DoubleValueHashMap<Id>();
		previousCarArrivalTime = new DoubleValueHashMap<Id>();
		firstActivityTypeAfterCarArrival = new HashMap<Id, String>();
		firstFacilityIdAfterCarArrival = new HashMap<Id, Id>();
		setLog(new StationaryChargingOutput());
		previousCarArrivalLinkId = new HashMap<Id, Id>();
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (!isVehicleWithBattery(event.getPersonId())) {
			return;
		}

		Id personId = event.getPersonId();

		if (event.getLegMode().equals(TransportMode.car) && vehicles.containsKey(personId)) {
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
		Double availablePowerInWatt = null;
		String activityType = firstActivityTypeAfterCarArrival.get(personId);

		availablePowerInWatt = getChargablePowerAtActivityTypes().get(activityType);

		if (availablePowerInWatt==0){
			return;
		}
		
		if (availablePowerInWatt == null) {
			DebugLib.stopSystemAndReportInconsistency("power at all activity types needs to be specified, missing:"
					+ activityType);
		}

		double chargableEnergyInJoules = availablePowerInWatt * parkingDuration;

		VehicleWithBattery vehicleWithBattery = (VehicleWithBattery) vehicles.get(personId);
		double energyToChargeInJoules = 0;
		if (vehicleWithBattery.getRequiredEnergyInJoules() <= chargableEnergyInJoules) {
			energyToChargeInJoules = vehicleWithBattery.getRequiredEnergyInJoules();
		} else {
			energyToChargeInJoules = chargableEnergyInJoules;
		}

		if (energyToChargeInJoules > 0) {
			vehicleWithBattery.chargeBattery(energyToChargeInJoules);

			ChargingLogRow chargingLogRow = new ChargingLogRow(personId, firstFacilityIdAfterCarArrival.get(personId),
					carArrivalTime, energyToChargeInJoules / availablePowerInWatt, energyToChargeInJoules);
			getLog().add(chargingLogRow);
		}

	}

	private boolean isFirstCarDepartureOfDay(Id personId) {
		return !firstDepartureTimeOfDay.containsKey(personId);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (!isVehicleWithBattery(event.getPersonId())) {
			return;
		}

		if (event.getLegMode().equals(TransportMode.car)) {
			initFirstActivityAfterCarArrival(event);
			updateCarArrivalTime(event);
			updatePreviousCarArrivalLinkId(event);
		}
	}

	private boolean isVehicleWithBattery(Id personId) {
		return vehicles.get(personId) instanceof VehicleWithBattery;
	}

	private void updatePreviousCarArrivalLinkId(AgentArrivalEvent event) {
		previousCarArrivalLinkId.put(event.getPersonId(), event.getLinkId());
	}

	private void updateCarArrivalTime(AgentArrivalEvent event) {
		previousCarArrivalTime.put(event.getPersonId(), event.getTime());
	}

	private void initFirstActivityAfterCarArrival(AgentArrivalEvent event) {
		firstActivityTypeAfterCarArrival.remove(event.getPersonId());
		firstFacilityIdAfterCarArrival.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		updateFirstActivityTypeAfterCarArrival(event);
	}

	private void updateFirstActivityTypeAfterCarArrival(ActivityStartEvent event) {
		if (!isVehicleWithBattery(event.getPersonId())) {
			return;
		}

		Id personId = event.getPersonId();
		if (!firstActivityTypeAfterCarArrival.containsKey(personId)) {
			firstActivityTypeAfterCarArrival.put(personId, event.getActType());
		}

		if (!firstFacilityIdAfterCarArrival.containsKey(personId)) {
			firstFacilityIdAfterCarArrival.put(event.getPersonId(), event.getFacilityId());
		}
	}

	public void handleLastParkingOfDay() {
		for (Id personId : vehicles.keySet()) {
			double carArrivalTime = previousCarArrivalTime.get(personId);
			double carDepartureTime = firstDepartureTimeOfDay.get(personId);

			chargeVehicle(personId, carArrivalTime, carDepartureTime);
		}
	}

	public ChargingOutputLog getLog() {
		return log;
	}

	public void setLog(ChargingOutputLog log) {
		this.log = log;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		handleLastParkingOfDay();
	}

	public void setPowerForNonInitializedActivityTypes(ActivityFacilities facilities, double powerInWatt) {
		this.powerAvalabilityParameters = new PowerAvalabilityParameters(facilities, powerInWatt);
	}

	public void setChargablePowerAtActivityTypes(DoubleValueHashMap<String> chargablePowerAtActivityTypes) {
		this.chargablePowerAtActivityTypes = chargablePowerAtActivityTypes;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		initPowerAvailableAtStartup();
	}

	private void initPowerAvailableAtStartup() {
		if (this.powerAvalabilityParameters != null) {
			for (ActivityFacility facility : this.powerAvalabilityParameters.getFacilities().getFacilities().values()) {
				for (ActivityOption actOption : facility.getActivityOptions().values()) {
					String actType = actOption.getType();
					if (!getChargablePowerAtActivityTypes().containsKey(actType)) {
						getChargablePowerAtActivityTypes().put(actType, this.powerAvalabilityParameters.getPowerInWatt());
					}
				}
			}
		}
	}

	public DoubleValueHashMap<String> getChargablePowerAtActivityTypes() {
		return chargablePowerAtActivityTypes;
	}

	public void setDefaultValues(DoubleValueHashMap<String> chargablePowerAtActivityTypes) {
		this.setChargablePowerAtActivityTypes(chargablePowerAtActivityTypes);
	}

	

	private class PowerAvalabilityParameters {

		private ActivityFacilities facilities;
		private double powerInWatt;

		public ActivityFacilities getFacilities() {
			return facilities;
		}

		public double getPowerInWatt() {
			return powerInWatt;
		}

		PowerAvalabilityParameters(ActivityFacilities facilities, double powerInWatt) {
			this.facilities = facilities;
			this.powerInWatt = powerInWatt;
		}

	}

}
