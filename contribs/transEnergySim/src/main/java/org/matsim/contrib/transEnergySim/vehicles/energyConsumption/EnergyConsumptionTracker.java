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
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.transEnergySim.analysis.energyConsumption.EnergyConsumptionLogRow;
import org.matsim.contrib.transEnergySim.analysis.energyConsumption.EnergyConsumptionOutputLog;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

/**
 * This module tracks the energy consumption of vehicles based on event
 * handling.
 * 
 * Special attention should be paid, when using this module in connection with
 * parallelEventHandling, see comments regarding this on the matsim.org website. 
 * 
 * 
 * 
 * This module can handle both the energy consumption of
 * jdeqsim and qsim.
 * 
 * TODO: add tests for this also
 * 
 * @author rashid_waraich
 * 			jbischoff
 * 
 */

public class EnergyConsumptionTracker implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler,
		PersonArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private EnergyConsumptionOutputLog log;

	HashMap<Id<Vehicle>, Vehicle> vehicles;

	DoubleValueHashMap<Id<Vehicle>> linkEnterTime;
//	HashMap<Id, Id> previousLinkEntered;

	private final Network network;

	private boolean loggingEnabled;
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	public EnergyConsumptionTracker(HashMap<Id<Vehicle>, Vehicle> vehicles, Network network) {
		this.vehicles = vehicles;
		this.network = network;
		enableLogging();
		reset(-1);
	}

	@Override
	public void reset(int iteration) {
		linkEnterTime = new DoubleValueHashMap<Id<Vehicle>>();
//		previousLinkEntered = new HashMap<Id, Id>();
		setLog(new EnergyConsumptionOutputLog());

		for (Vehicle vehicle : vehicles.values()) {
			vehicle.reset();
		}
		
		delegate.reset(iteration);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			handleEnergyConsumption(Id.create(event.getPersonId(), Vehicle.class), event.getLinkId(), event.getTime());
			    
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			Id<Vehicle> vehicleId = Id.create(event.getPersonId(), Vehicle.class);
			linkEnterTime.put(vehicleId, event.getTime());
			//assumption  - Vehicle and Person Ids are the same
			
		
			}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		handleEnergyConsumption(Id.create(delegate.getDriverOfVehicle(event.getVehicleId()), Vehicle.class), event.getLinkId(), event.getTime());
	}

	private void handleEnergyConsumption(Id<Vehicle> vehicleId, Id<Link> linkId, double linkLeaveTime) {
		double linkEnterTime = this.linkEnterTime.get(vehicleId);
		double timeSpendOnLink = GeneralLib.getIntervalDuration(linkEnterTime, linkLeaveTime);

		Link link = network.getLinks().get(linkId);
		double averageSpeedDrivenInMetersPerSecond = link.getLength() / timeSpendOnLink;

		if (zeroTravelTime(linkEnterTime, linkLeaveTime)) {
			return;
		}

		if (vehicles.containsKey(vehicleId)){
			
		Vehicle vehicle = vehicles.get(vehicleId);
		
		double energyConsumptionInJoule=0;
		if (averageSpeedDrivenInMetersPerSecond<=link.getFreespeed()){
			energyConsumptionInJoule= vehicle.updateEnergyUse(link, averageSpeedDrivenInMetersPerSecond);
		} else {
			
			energyConsumptionInJoule= vehicle.updateEnergyUse(link.getFreespeed()*timeSpendOnLink, link.getFreespeed(), link.getFreespeed());
		}
		 

		if (loggingEnabled) {
			
			double soc = 0;
			double loc = 0;
			if (vehicle instanceof VehicleWithBattery){
				VehicleWithBattery a = (VehicleWithBattery)vehicle;
				soc = a.getSocInJoules();
				loc = soc / a.getUsableBatteryCapacityInJoules();
			}
				
//			Id ll = new IdImpl(Math.round(link.getLength()));
//			 getLog().add(new EnergyConsumptionLogRow(personId, ll, energyConsumptionInJoule));

			getLog().add(new EnergyConsumptionLogRow(vehicleId, linkId, energyConsumptionInJoule));
		}
		}
	}

	private boolean zeroTravelTime(double linkEnterTime, double linkLeaveTime) {
		return linkEnterTime == linkLeaveTime;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Vehicle> vehicleId = Id.create(delegate.getDriverOfVehicle(event.getVehicleId()), Vehicle.class);
		linkEnterTime.put(vehicleId, event.getTime());
	}

	public EnergyConsumptionOutputLog getLog() {
		return log;
	}

	public void setLog(EnergyConsumptionOutputLog log) {
		this.log = log;
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
