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

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;

/**
 * This module can handle both the energy consumption of jdeqsim and qsim.
 * @author wrashid
 *
 */

public class EnergyConsumptionTracker implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentDepartureEventHandler,
AgentArrivalEventHandler {

	HashMap<Id, Vehicle> vehicles;
	
	DoubleValueHashMap<Id> linkEnterTime;
	HashMap<Id, Id> previousLinkEntered;

	private final Network network;
	
	public EnergyConsumptionTracker(HashMap<Id, Vehicle> vehicles, Network network){
		this.vehicles=vehicles;
		this.network = network;
	}
	
	@Override
	public void reset(int iteration) {
		linkEnterTime=new DoubleValueHashMap<Id>();
		previousLinkEntered=new HashMap<Id, Id>();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		handleEnergyConsumption(event.getPersonId(),event.getLinkId(),event.getTime());
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)){
			Id personId = event.getPersonId();
			linkEnterTime.put(personId, event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		handleEnergyConsumption(event.getPersonId(),event.getLinkId(),event.getTime());
	}
	
	private void handleEnergyConsumption(Id personId,Id linkId,double linkLeaveTime){
		double linkEnterTime=this.linkEnterTime.get(personId);
		double timeSpendOnLink = GeneralLib.getIntervalDuration(linkEnterTime, linkLeaveTime);
		
		Link link = network.getLinks().get(linkId);
		double averageSpeedDrivenInMetersPerSecond = link.getLength() / timeSpendOnLink;
		
		if (shouldLinkBeIgnored(linkLeaveTime, timeSpendOnLink, link, averageSpeedDrivenInMetersPerSecond)) {
			return;
		}
		
		Vehicle vehicle = vehicles.get(personId);
		vehicle.updateEnergyUse(link, averageSpeedDrivenInMetersPerSecond);
		
		//TODO: also log this... (per vehicle energy consumption)
	}

	private boolean shouldLinkBeIgnored(double linkLeaveTime, double timeSpendOnLink, Link link,
			double averageSpeedDrivenInMetersPerSecond) {
		return timeSpendOnLink == linkLeaveTime || averageSpeedDrivenInMetersPerSecond > link.getFreespeed();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();
		linkEnterTime.put(personId, event.getTime());
	}

}
