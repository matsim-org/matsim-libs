/*
 *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       :  (C) 2024 by the members listed in the COPYING,       *
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

package org.matsim.freight.logistics.analysis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Basic event handler that collects the relation between vehicles and drivers.
 * Necessary since link enter and leave events do not contain the driver anymore.
 * <p>
 * This is the vice versa implementation of {@link org.matsim.core.events.algorithms.Vehicle2DriverEventHandler}.
 * <p>
 * In a first step only used internally. When needed more often, I have nothing against putting it more central. -> matsim-libs
 *
 * @author kturner
 */
public class Driver2VehicleEventHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

  private final Map<Id<Person>, Id<Vehicle>> driversVehicles = new ConcurrentHashMap<>();

  @Override
  public void reset(int iteration) {
    driversVehicles.clear();
  }

  @Override
  public void handleEvent(VehicleEntersTrafficEvent event) {
    driversVehicles.put(event.getPersonId(), event.getVehicleId());
  }

  @Override
  public void handleEvent(VehicleLeavesTrafficEvent event) {
    driversVehicles.remove(event.getPersonId());
  }

  /**
   * @param personId the unique driver identifier.
   * @return vehicle id of the driver's vehicle
   */
  public Id<Vehicle> getVehicleOfDriver(Id<Person> personId) {
    return driversVehicles.get(personId);
  }

}
