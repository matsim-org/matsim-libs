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

package org.matsim.contrib.transEnergySim.vehicles.energyConsumption;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.transEnergySim.analysis.energyConsumption.EnergyConsumptionLogRow;
import org.matsim.contrib.transEnergySim.analysis.energyConsumption.EnergyConsumptionOutputLog;
import org.matsim.contrib.transEnergySim.controllers.AddHandlerAtStartupControler;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;

/**
 * This module can handle both the energy consumption of jdeqsim and qsim.
 * 
 * TODO: add tests for this also
 * 
 * @author wrashid
 * 
 */

public class EnergyConsumptionTracker implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentDepartureEventHandler,
		AgentArrivalEventHandler {

	private EnergyConsumptionOutputLog log;

	HashMap<Id, Vehicle> vehicles;

	DoubleValueHashMap<Id> linkEnterTime;
	HashMap<Id, Id> previousLinkEntered;

	private final Network network;

	public EnergyConsumptionTracker(HashMap<Id, Vehicle> vehicles, Network network) {
		this.vehicles = vehicles;
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		linkEnterTime = new DoubleValueHashMap<Id>();
		previousLinkEntered = new HashMap<Id, Id>();
		setLog(new EnergyConsumptionOutputLog());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			handleEnergyConsumption(event.getPersonId(), event.getLinkId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			Id personId = event.getPersonId();
			linkEnterTime.put(personId, event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		handleEnergyConsumption(event.getPersonId(), event.getLinkId(), event.getTime());
	}

	private void handleEnergyConsumption(Id personId, Id linkId, double linkLeaveTime) {
		double linkEnterTime = this.linkEnterTime.get(personId);
		double timeSpendOnLink = GeneralLib.getIntervalDuration(linkEnterTime, linkLeaveTime);

		Link link = network.getLinks().get(linkId);
		double averageSpeedDrivenInMetersPerSecond = link.getLength() / timeSpendOnLink;

		if (zeroTravelTime(linkEnterTime, linkLeaveTime)) {
			return;
		}

		Vehicle vehicle = vehicles.get(personId);
		double energyConsumptionInJoule = vehicle.updateEnergyUse(link, averageSpeedDrivenInMetersPerSecond);

		getLog().add(new EnergyConsumptionLogRow(personId, linkId, energyConsumptionInJoule));
	}

	private boolean zeroTravelTime(double linkEnterTime, double linkLeaveTime) {
		return linkEnterTime == linkLeaveTime;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();
		linkEnterTime.put(personId, event.getTime());
	}

	public EnergyConsumptionOutputLog getLog() {
		return log;
	}

	public void setLog(EnergyConsumptionOutputLog log) {
		this.log = log;
	}

}
