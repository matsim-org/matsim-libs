/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2022 by the members listed in the COPYING,       *
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
  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.simulationTrackers;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/*package-private*/ class DistanceAndTimeHandler
    implements LinkEnterEventHandler, VehicleLeavesTrafficEventHandler, LinkLeaveEventHandler {
  private static final Logger log = LogManager.getLogger(DistanceAndTimeHandler.class);

  private final Map<Id<Vehicle>, LinkEnterEvent> events;
  private final Vehicles allVehicles;
  private final Network network;
  private double distanceCosts;
  private double timeCosts;

  DistanceAndTimeHandler(Scenario scenario) {
    this.network = scenario.getNetwork();
    this.events = new LinkedHashMap<>();
    this.allVehicles = VehicleUtils.getOrCreateAllvehicles(scenario);
  }

  @Override
  public void handleEvent(LinkEnterEvent event) {
    events.put(event.getVehicleId(), event);
  }

  @Override
  public void reset(int iteration) {
    events.clear();
  }

  @Override
  public void handleEvent(VehicleLeavesTrafficEvent leaveEvent) {
    processLeaveEvent(leaveEvent.getVehicleId(), leaveEvent.getTime());
  }

  @Override
  public void handleEvent(LinkLeaveEvent leaveEvent) {
    processLeaveEvent(leaveEvent.getVehicleId(), leaveEvent.getTime());
  }

  private void processLeaveEvent(Id<Vehicle> vehicleId, double time) {

    LinkEnterEvent enterEvent = events.remove(vehicleId);
    if (enterEvent != null) {
      Vehicle carrierVehicle = this.allVehicles.getVehicles().get(vehicleId);
      double linkDuration = time - enterEvent.getTime();
      timeCosts += linkDuration * carrierVehicle.getType().getCostInformation().getCostsPerSecond();
      double linkLength = network.getLinks().get(enterEvent.getLinkId()).getLength();
      distanceCosts +=
          linkLength * carrierVehicle.getType().getCostInformation().getCostsPerMeter();
    }
    // (there might not be a corresponding enter-event if vehicle just entered traffic.  Could add
    // that as well, but then we would need to compensate for fact that this covers little distance.
    // kai, jul'22)

  }

  public double getDistanceCosts() {
    return distanceCosts;
  }

  public double getTimeCosts() {
    return timeCosts;
  }
}
