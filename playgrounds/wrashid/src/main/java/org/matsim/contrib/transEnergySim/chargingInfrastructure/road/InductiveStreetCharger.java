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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingLogRow;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingOutputLog;
import org.matsim.contrib.transEnergySim.analysis.charging.InductiveChargingAtRoadOutput;
import org.matsim.contrib.transEnergySim.controllers.AddHandlerAtStartupControler;
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.InductivlyChargable;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;

/**
 * This class should work both with jdeqsim and mobsim.
 * 
 * @author wrashid
 * 
 */
public class InductiveStreetCharger implements AgentDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
		AgentArrivalEventHandler, StartupListener {

	private DoubleValueHashMap<Id> chargableStreets;
	private ChargingOutputLog log;

	private HashMap<Id, Vehicle> vehicles;

	DoubleValueHashMap<Id> linkEnterTime;
	HashMap<Id, Id> previousLinkEntered;

	private final Network network;
	private Double samePowerAtAllLinks;

	public InductiveStreetCharger(HashMap<Id, Vehicle> vehicles, Network network,
			AddHandlerAtStartupControler controller) {
		this.setVehicles(vehicles);
		this.network = network;
		controller.addControlerListener(this);
	}

	private void setVehicles(HashMap<Id, Vehicle> vehicles) {
		this.vehicles = vehicles;
	}

	@Override
	public void reset(int iteration) {
		linkEnterTime = new DoubleValueHashMap<Id>();
		previousLinkEntered = new HashMap<Id, Id>();
		setLog(new InductiveChargingAtRoadOutput());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (ignoreAgent(event.getPersonId(), event.getLinkId())) {
			return;
		}

		handleCharging(event.getPersonId(), event.getLinkId(), event.getTime());
	}

	private boolean ignoreAgent(Id agentId, Id linkId) {
		return (samePowerAtAllLinks==null && !getChargableStreets().containsKey(linkId)) && !(getVehicles().get(agentId) instanceof VehicleWithBattery);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (ignoreAgent(event.getPersonId(), event.getLinkId())) {
			return;
		}

		if (event.getLegMode().equals(TransportMode.car)) {
			Id personId = event.getPersonId();
			linkEnterTime.put(personId, event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (ignoreAgent(event.getPersonId(), event.getLinkId())) {
			return;
		}

		handleCharging(event.getPersonId(), event.getLinkId(), event.getTime());
	}

	private void handleCharging(Id personId, Id linkId, double linkLeaveTime) {
		if (!(getVehicles().get(personId) instanceof InductivlyChargable)) {
			return;
		}

		double linkEnterTime = this.linkEnterTime.get(personId);
		double timeSpendOnLink = GeneralLib.getIntervalDuration(linkEnterTime, linkLeaveTime);

		Link link = network.getLinks().get(linkId);
		double averageSpeedDrivenInMetersPerSecond = link.getLength() / timeSpendOnLink;

		if (shouldLinkBeIgnored(linkEnterTime, linkLeaveTime)) {
			return;
		}

		double availablePowerInWatt;
		if (samePowerAtAllLinks!=null){
			availablePowerInWatt=samePowerAtAllLinks;
		} else {
			availablePowerInWatt = getChargableStreets().get(linkId);
		}

		double chargableEnergyInJoules = availablePowerInWatt * timeSpendOnLink;

		VehicleWithBattery vehicleWithBattery = (VehicleWithBattery) getVehicles().get(personId);
		double energyToChargeInJoules = 0;
		if (vehicleWithBattery.getRequiredEnergyInJoules() <= chargableEnergyInJoules) {
			energyToChargeInJoules = vehicleWithBattery.getRequiredEnergyInJoules();
		} else {
			energyToChargeInJoules = chargableEnergyInJoules;
		}

		if (energyToChargeInJoules > 0) {
			vehicleWithBattery.chargeBattery(energyToChargeInJoules);

			ChargingLogRow chargingLogRow = new ChargingLogRow(personId, linkId, linkEnterTime, energyToChargeInJoules
					/ availablePowerInWatt, energyToChargeInJoules);
			getLog().add(chargingLogRow);
		}

	}

	private boolean shouldLinkBeIgnored(double linkEnterTime, double linkLeaveTime) {
		return linkEnterTime == linkLeaveTime;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (ignoreAgent(event.getPersonId(), event.getLinkId())) {
			return;
		}

		Id personId = event.getPersonId();
		linkEnterTime.put(personId, event.getTime());
	}

	public ChargingOutputLog getLog() {
		return log;
	}

	private void setLog(ChargingOutputLog log) {
		this.log = log;
	}

	public HashMap<Id, Vehicle> getVehicles() {
		return vehicles;
	}

	public DoubleValueHashMap<Id> getChargableStreets() {
		return chargableStreets;
	}

	public void setChargableStreets(DoubleValueHashMap<Id> chargableStreets) {
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

}
