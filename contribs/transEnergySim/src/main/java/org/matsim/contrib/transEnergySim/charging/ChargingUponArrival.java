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
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingLogRowFacilityLevel;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingOutputLog;
import org.matsim.contrib.transEnergySim.analysis.charging.StationaryChargingOutputLog;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

/**
 * This module is not compatible with parking search, but should be with parking
 * choice. TODO: when those modules are finished, check if this is really a
 * concern.
 * 
 * 
 * @author wrashid
 * 
 */
public class ChargingUponArrival implements ActivityStartEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler,
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

	HashMap<Id<Vehicle>, Vehicle> vehicles;

	DoubleValueHashMap<Id<Vehicle>> firstDepartureTimeOfDay;
	DoubleValueHashMap<Id<Vehicle>> previousCarArrivalTime;

	HashMap<Id<Vehicle>, String> firstActivityTypeAfterCarArrival;
	HashMap<Id<Vehicle>, Id<ActivityFacility>> firstFacilityIdAfterCarArrival;

	HashMap<Id<Vehicle>, Id<Link>> previousCarArrivalLinkId;

	private PowerAvalabilityParameters powerAvalabilityParameters;

	private boolean loggingEnabled;

	private MatsimServices controller;

	public ChargingUponArrival(HashMap<Id<Vehicle>, Vehicle> vehicles, MatsimServices controller) {
		this.vehicles = vehicles;
		this.controller = controller;
		this.setDefaultValues(chargablePowerAtActivityTypes);
		controller.addControlerListener(this);
		chargablePowerAtActivityTypes = new DoubleValueHashMap<String>();
		enableLogging();
	}

	@Override
	public void reset(int iteration) {
		firstDepartureTimeOfDay = new DoubleValueHashMap<Id<Vehicle>>();
		previousCarArrivalTime = new DoubleValueHashMap<Id<Vehicle>>();
		firstActivityTypeAfterCarArrival = new HashMap<Id<Vehicle>, String>();
		firstFacilityIdAfterCarArrival = new HashMap<>();
		setLog(new StationaryChargingOutputLog());
		previousCarArrivalLinkId = new HashMap<>();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!isVehicleWithBattery(Id.create(event.getPersonId(), Vehicle.class))) {
			return;
		}

		Id<Person> personId = event.getPersonId();
		Id<Vehicle> vehicleId = Id.create(personId, Vehicle.class);

		if (event.getLegMode().equals(TransportMode.car) && vehicles.containsKey(vehicleId)) {
			if (isFirstCarDepartureOfDay(vehicleId)) {
				firstDepartureTimeOfDay.put(vehicleId, event.getTime());
			} else {
				double carArrivalTime = previousCarArrivalTime.get(vehicleId);
				double carDepartureTime = event.getTime();

				chargeVehicle(vehicleId, carArrivalTime, carDepartureTime);
			}
		}
	}

	private void chargeVehicle(Id<Vehicle> personId, double carArrivalTime, double carDepartureTime) {

		double parkingDuration = GeneralLib.getIntervalDuration(carArrivalTime, carDepartureTime);
		Double availablePowerInWatt = null;
		String activityType = firstActivityTypeAfterCarArrival.get(personId);

		availablePowerInWatt = getChargablePowerAtActivityTypes().get(activityType);

		if (availablePowerInWatt == 0) {
			return;
		}

		if (availablePowerInWatt == null) {
			DebugLib.stopSystemAndReportInconsistency("power at all activity types needs to be specified, missing:" + activityType);
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
			vehicleWithBattery.chargeVehicle(energyToChargeInJoules);
			
			if (loggingEnabled) {
				Id<ActivityFacility> facilityId = firstFacilityIdAfterCarArrival.get(personId);
				
				if (facilityId != Id.create(facilityId, ActivityFacility.class)) {
					System.out.println("id has wrong type");
				}
				
				Id<Link> linkId = controller.getScenario().getActivityFacilities().getFacilities().get(facilityId).getLinkId();
				
				if (linkId==null){
                    linkId = NetworkUtils.getNearestLink((controller.getScenario().getNetwork()), controller.getScenario().getActivityFacilities().getFacilities().get(facilityId).getCoord()).getId();
				}
				
				ChargingLogRowFacilityLevel chargingLogRow = new ChargingLogRowFacilityLevel(personId, linkId,facilityId,
						carArrivalTime, energyToChargeInJoules / availablePowerInWatt, energyToChargeInJoules);
				getLog().add(chargingLogRow);
			}
		}

	}

	private boolean isFirstCarDepartureOfDay(Id<Vehicle> vehicleId) {
		return !firstDepartureTimeOfDay.containsKey(vehicleId);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!isVehicleWithBattery(Id.create(event.getPersonId(), Vehicle.class))) {
			return;
		}

		if (event.getLegMode().equals(TransportMode.car)) {
			initFirstActivityAfterCarArrival(event);
			updateCarArrivalTime(event);
			updatePreviousCarArrivalLinkId(event);
		}
	}

	private boolean isVehicleWithBattery(Id<Vehicle> vehicleId) {
		return vehicles.get(vehicleId) instanceof VehicleWithBattery;
	}

	private void updatePreviousCarArrivalLinkId(PersonArrivalEvent event) {
		previousCarArrivalLinkId.put(Id.create(event.getPersonId(),Vehicle.class), event.getLinkId());
	}

	private void updateCarArrivalTime(PersonArrivalEvent event) {
		previousCarArrivalTime.put(Id.create(event.getPersonId(),Vehicle.class), event.getTime());
	}

	private void initFirstActivityAfterCarArrival(PersonArrivalEvent event) {
		firstActivityTypeAfterCarArrival.remove(Id.create(event.getPersonId(), Vehicle.class));
		firstFacilityIdAfterCarArrival.remove(Id.create(event.getPersonId(), Vehicle.class));
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		updateFirstActivityTypeAfterCarArrival(event);
	}

	private void updateFirstActivityTypeAfterCarArrival(ActivityStartEvent event) {
		if (!isVehicleWithBattery(Id.create(event.getPersonId(), Vehicle.class))){
			return;
		}

		Id<Vehicle> vehicleId = Id.create(event.getPersonId(), Vehicle.class);
		if (!firstActivityTypeAfterCarArrival.containsKey(vehicleId)) {
			firstActivityTypeAfterCarArrival.put(vehicleId, event.getActType());
		}

		if (!firstFacilityIdAfterCarArrival.containsKey(vehicleId)) {
			firstFacilityIdAfterCarArrival.put(Id.create(event.getPersonId(),Vehicle.class), event.getFacilityId());
		}
	}

	public void handleLastParkingOfDay() {
		for (Id<Vehicle> vehicleId : vehicles.keySet()) {
			double carArrivalTime = previousCarArrivalTime.get(vehicleId);
			double carDepartureTime = firstDepartureTimeOfDay.get(vehicleId);

			chargeVehicle(vehicleId, carArrivalTime, carDepartureTime);
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

	public void enableLogging() {
		loggingEnabled = true;
	}

	public void disableLogging() {
		loggingEnabled = false;
	}

}
