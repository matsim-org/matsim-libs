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
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.InductivlyChargable;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
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

	HashMap<Id, Vehicle> vehicles;

	DoubleValueHashMap<Id> linkEnterTime;
	HashMap<Id, Id> previousLinkEntered;

	private final Network network;

	public InductiveStreetCharger(DoubleValueHashMap<Id> powerAtInductiveStreets, HashMap<Id, Vehicle> vehicles, Network network) {
		this.chargableStreets = powerAtInductiveStreets;
		this.vehicles = vehicles;
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		linkEnterTime = new DoubleValueHashMap<Id>();
		previousLinkEntered = new HashMap<Id, Id>();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (notChargableRoad(event.getLinkId())) {
			return;
		}

		handleCharging(event.getPersonId(), event.getLinkId(), event.getTime());
	}

	private boolean notChargableRoad(Id id) {
		return !chargableStreets.containsKey(id);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (notChargableRoad(event.getLinkId())) {
			return;
		}

		if (event.getLegMode().equals(TransportMode.car)) {
			Id personId = event.getPersonId();
			linkEnterTime.put(personId, event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (notChargableRoad(event.getLinkId())) {
			return;
		}

		handleCharging(event.getPersonId(), event.getLinkId(), event.getTime());
	}

	private void handleCharging(Id personId, Id linkId, double linkLeaveTime) {
		if (!(vehicles.get(personId) instanceof InductivlyChargable)) {
			return;
		}

		double linkEnterTime = this.linkEnterTime.get(personId);
		double timeSpendOnLink = GeneralLib.getIntervalDuration(linkEnterTime, linkLeaveTime);

		Link link = network.getLinks().get(linkId);
		double averageSpeedDrivenInMetersPerSecond = link.getLength() / timeSpendOnLink;

		if (shouldLinkBeIgnored(linkLeaveTime, timeSpendOnLink, link, averageSpeedDrivenInMetersPerSecond)) {
			return;
		}

		double availablePowerInWatt = chargableStreets.get(linkId);

		double chargableEnergyInJoules = availablePowerInWatt * timeSpendOnLink;

		BatteryElectricVehicle vehicleWithBattery = (BatteryElectricVehicle) vehicles.get(personId);
		double energyToChargeInJoules = 0;
		if (vehicleWithBattery.getRequiredEnergyInJoules() <= chargableEnergyInJoules) {
			energyToChargeInJoules = vehicleWithBattery.getRequiredEnergyInJoules();
		} else {
			energyToChargeInJoules = chargableEnergyInJoules;
		}
		vehicleWithBattery.chargeBattery(energyToChargeInJoules);

		if (log!=null){
			ChargingLogRow chargingLogRow = new ChargingLogRow(personId,linkId,linkEnterTime,energyToChargeInJoules/availablePowerInWatt,energyToChargeInJoules);
			log.add(chargingLogRow);
		}
		
	}

	private boolean shouldLinkBeIgnored(double linkLeaveTime, double timeSpendOnLink, Link link,
			double averageSpeedDrivenInMetersPerSecond) {
		return timeSpendOnLink == linkLeaveTime || averageSpeedDrivenInMetersPerSecond > link.getFreespeed();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (notChargableRoad(event.getLinkId())) {
			return;
		}

		Id personId = event.getPersonId();
		linkEnterTime.put(personId, event.getTime());
	}

	public void allStreetsCanChargeWithPower(double powerInWatt) {
		chargableStreets=new DoubleValueHashMap<Id>();
		for (Id linkId:network.getLinks().keySet()){
			chargableStreets.put(linkId, powerInWatt);
		}
	}

}
