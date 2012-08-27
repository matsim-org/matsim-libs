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

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;

/**
 * This class should work both with jdeqsim and mobsim.
 * 
 * @author wrashid
 * 
 */
public class InductiveStreetCharger implements AgentDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
		AgentArrivalEventHandler {

	private DoubleValueHashMap<Id> chargableStreets;
	private ChargingOutputLog log;

	private HashMap<Id, Vehicle> vehicles;

	DoubleValueHashMap<Id> linkEnterTime;
	HashMap<Id, Id> previousLinkEntered;

	private final Network network;

	public InductiveStreetCharger(DoubleValueHashMap<Id> powerAtInductiveStreets, HashMap<Id, Vehicle> vehicles, Network network,
			AddHandlerAtStartupControler controller) {
		this.setChargableStreets(powerAtInductiveStreets);
		this.setVehicles(vehicles);
		this.network = network;
		controller.addHandler(this);
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
		if (isAgentAndLinkRelevantForProcessing(event.getPersonId(), event.getLinkId())) {
			return;
		}

		handleCharging(event.getPersonId(), event.getLinkId(), event.getTime());
	}

	private boolean isAgentAndLinkRelevantForProcessing(Id agentId, Id linkId) {
		return !getChargableStreets().containsKey(linkId) && (getVehicles().get(agentId) instanceof VehicleWithBattery);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (isAgentAndLinkRelevantForProcessing(event.getPersonId(), event.getLinkId())) {
			return;
		}

		if (event.getLegMode().equals(TransportMode.car)) {
			Id personId = event.getPersonId();
			linkEnterTime.put(personId, event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (isAgentAndLinkRelevantForProcessing(event.getPersonId(), event.getLinkId())) {
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

		if (shouldLinkBeIgnored(linkLeaveTime, timeSpendOnLink, link, averageSpeedDrivenInMetersPerSecond)) {
			return;
		}

		double availablePowerInWatt = getChargableStreets().get(linkId);

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

	private boolean shouldLinkBeIgnored(double linkLeaveTime, double timeSpendOnLink, Link link,
			double averageSpeedDrivenInMetersPerSecond) {
		return timeSpendOnLink == linkLeaveTime || averageSpeedDrivenInMetersPerSecond > link.getFreespeed();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (isAgentAndLinkRelevantForProcessing(event.getPersonId(), event.getLinkId())) {
			return;
		}

		Id personId = event.getPersonId();
		linkEnterTime.put(personId, event.getTime());
	}

	public void allStreetsCanChargeWithPower(double powerInWatt) {
		setChargableStreets(new DoubleValueHashMap<Id>());
		for (Id linkId : network.getLinks().keySet()) {
			getChargableStreets().put(linkId, powerInWatt);
		}
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
}
