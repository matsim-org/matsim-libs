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

package org.matsim.contrib.transEnergySim.chargingInfrastructure.road;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingLogRowLinkLevel;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingOutputLog;
import org.matsim.contrib.transEnergySim.analysis.charging.InductiveChargingAtRoadOutputLog;
import org.matsim.contrib.transEnergySim.vehicles.api.InductivlyChargable;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

/**
 * This class should work both with jdeqsim and mobsim.
 * 
 * @author wrashid
 * 
 */
public class InductiveStreetCharger implements PersonDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
		PersonArrivalEventHandler, StartupListener, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private DoubleValueHashMap<Id<Link>> chargableStreets;
	private ChargingOutputLog log;

	private HashMap<Id<Vehicle>, Vehicle> vehicles;

	DoubleValueHashMap<Id<Vehicle>> linkEnterTime;
	HashMap<Id, Id> previousLinkEntered;

	private Double samePowerAtAllLinks;
	private boolean loggingEnabled;
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	public InductiveStreetCharger(HashMap<Id<Vehicle>, Vehicle> vehicles, Network network, MatsimServices controller) {
		this.setVehicles(vehicles);
		controller.addControlerListener(this);
		enableLogging();
	}

	private void setVehicles(HashMap<Id<Vehicle>, Vehicle> vehicles) {
		this.vehicles = vehicles;
	}

	@Override
	public void reset(int iteration) {
		linkEnterTime = new DoubleValueHashMap<Id<Vehicle>>();
		previousLinkEntered = new HashMap<Id, Id>();
		setLog(new InductiveChargingAtRoadOutputLog());
		delegate.reset(iteration);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
	       Id<Vehicle> vehicleId = Id.create(event.getPersonId(),Vehicle.class);

		if (ignoreAgent(vehicleId, event.getLinkId())) {
			return;
		}

		handleCharging(vehicleId, event.getLinkId(), event.getTime());
	}

	private boolean ignoreAgent(Id<Vehicle> vehicleId, Id<Link> linkId) {
		return (samePowerAtAllLinks == null && !getChargableStreets().containsKey(linkId))
				&& !(getVehicles().get(vehicleId) instanceof VehicleWithBattery);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (ignoreAgent(Id.create(event.getPersonId(),Vehicle.class), event.getLinkId())) {
			return;
		}

		if (event.getLegMode().equals(TransportMode.car)) {
			Id<Vehicle> vehicleId = Id.create(event.getPersonId(),Vehicle.class);
			linkEnterTime.put(vehicleId, event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
	    Id<Vehicle> vehicleId = Id.create(delegate.getDriverOfVehicle(event.getVehicleId()),Vehicle.class);
		if (ignoreAgent(vehicleId, event.getLinkId())) {
			return;
		}

		handleCharging(vehicleId, event.getLinkId(), event.getTime());
	}

	private void handleCharging(Id<Vehicle> vehicleId, Id<Link> linkId, double linkLeaveTime) {
		if (!(getVehicles().get(vehicleId) instanceof InductivlyChargable)) {
			return;
		}

		double linkEnterTime = this.linkEnterTime.get(vehicleId);
		double timeSpendOnLink = GeneralLib.getIntervalDuration(linkEnterTime, linkLeaveTime);

		if (shouldLinkBeIgnored(linkEnterTime, linkLeaveTime)) {
			return;
		}

		double availablePowerInWatt;
		if (samePowerAtAllLinks != null) {
			availablePowerInWatt = samePowerAtAllLinks;
		} else {
			availablePowerInWatt = getChargableStreets().get(linkId);
		}

		double chargableEnergyInJoules = availablePowerInWatt * timeSpendOnLink;

		VehicleWithBattery vehicleWithBattery = (VehicleWithBattery) getVehicles().get(vehicleId);
		double energyToChargeInJoules = 0;
		if (vehicleWithBattery.getRequiredEnergyInJoules() <= chargableEnergyInJoules) {
			energyToChargeInJoules = vehicleWithBattery.getRequiredEnergyInJoules();
		} else {
			energyToChargeInJoules = chargableEnergyInJoules;
		}

		if (energyToChargeInJoules > 0) {
			vehicleWithBattery.chargeVehicle(energyToChargeInJoules);

			if (loggingEnabled) {
				ChargingLogRowLinkLevel chargingLogRow = new ChargingLogRowLinkLevel(vehicleId, linkId, linkEnterTime, energyToChargeInJoules
						/ availablePowerInWatt, energyToChargeInJoules);
				getLog().add(chargingLogRow);
			}
		}

	}

	private boolean shouldLinkBeIgnored(double linkEnterTime, double linkLeaveTime) {
		return linkEnterTime == linkLeaveTime;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (ignoreAgent(Id.create(delegate.getDriverOfVehicle(event.getVehicleId()), Vehicle.class), event.getLinkId())) {
			return;
		}

		linkEnterTime.put(Id.create(delegate.getDriverOfVehicle(event.getVehicleId()), Vehicle.class), event.getTime());
	}

	public ChargingOutputLog getLog() {
		return log;
	}

	private void setLog(ChargingOutputLog log) {
		this.log = log;
	}

	public HashMap<Id<Vehicle>, Vehicle> getVehicles() {
		return vehicles;
	}

	public DoubleValueHashMap<Id<Link>> getChargableStreets() {
		return chargableStreets;
	}

	public void setChargableStreets(DoubleValueHashMap<Id<Link>> chargableStreets) {
		this.chargableStreets = chargableStreets;
	}

	public void setSamePowerAtAllStreets(double powerInWatt) {
		samePowerAtAllLinks = powerInWatt;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		if (samePowerAtAllLinks == null) {
			return;
		}

		if (getChargableStreets() != null && getChargableStreets().size() > 0) {
			DebugLib.stopSystemAndReportInconsistency("when using method 'setSamePowerAtAllStreets', manipulation of individual roads not allowed ");
		}
	}

	public void enableLogging() {
		loggingEnabled = true;
	}

	public void disableLogging() {
		loggingEnabled = false;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}

}
