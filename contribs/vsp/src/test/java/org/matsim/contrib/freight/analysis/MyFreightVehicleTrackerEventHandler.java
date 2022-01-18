/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2021 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.analysis;

import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

  class MyFreightVehicleTrackerEventHandler implements ActivityStartEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	 private final Vehicles vehicles;
	 private final Network network;
	 private final Carriers carriers;



	 private FreightAnalysisVehicleTracking vehicleTracking = new FreightAnalysisVehicleTracking();

	 MyFreightVehicleTrackerEventHandler(Vehicles vehicles, Network network, Carriers carriers) {
		 this.network = network;
		 this.carriers = carriers;
		 this.vehicles = vehicles;
		 this.init();
	 }

	 private void init() {
		 for (Vehicle vehicle : vehicles.getVehicles().values()) {
			 String vehicleIdString = vehicle.getId().toString();
			 if (vehicle.getId().toString().contains("freight")) {
				 vehicleTracking.addTracker(vehicle);
			 }
		 }
	 }

	 @Override
	 public void handleEvent(ActivityStartEvent activityStartEvent) {
		 if (activityStartEvent.getActType().equals("end")) {
			 vehicleTracking.endVehicleUsage(activityStartEvent.getPersonId());
		 }
	 }

	 // link events are used to calculate vehicle travel time and distance
	 @Override
	 public void handleEvent(LinkEnterEvent linkEnterEvent) {
	 	vehicleTracking.trackLinkEnterEvent(linkEnterEvent);
	 }

	 @Override
	 public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
		 vehicleTracking.trackLinkLeaveEvent(linkLeaveEvent, network.getLinks().get(linkLeaveEvent.getLinkId()).getLength());
	 }

	 // Person<>Vehicle relations and vehicle usage times are tracked
	 @Override
	 public void handleEvent(PersonEntersVehicleEvent event) {
		 vehicleTracking.addDriver2Vehicle(event.getPersonId(), event.getVehicleId(), event.getTime());
	 }

	 @Override
	 public void handleEvent(PersonLeavesVehicleEvent event) {
	 	vehicleTracking.registerVehicleLeave(event);
	 }

	 public FreightAnalysisVehicleTracking getVehicleTracking() {
	 	return vehicleTracking;
	 }
 }
