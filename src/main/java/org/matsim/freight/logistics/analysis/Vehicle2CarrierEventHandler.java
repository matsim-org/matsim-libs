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
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourStartEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * Basic event handler that collects the relation between vehicles and carrier.
 * Necessary since there is no event having all this information together.
 * <p>
 * This is a modified implementation of {@link org.matsim.core.events.algorithms.Vehicle2DriverEventHandler}.
 * <p>
 * In a first step only used internally. When needed more often, I have nothing against putting it more central. -> matsim-libs
 *
 * @author kturner
 */
public class Vehicle2CarrierEventHandler implements CarrierTourStartEventHandler, CarrierTourEndEventHandler {

  private final Map<Id<Vehicle>, Id<Carrier>> vehicle2carrier = new ConcurrentHashMap<>();

  @Override
  public void reset(int iteration) {
    vehicle2carrier.clear();
  }

  @Override
  public void handleEvent(CarrierTourStartEvent event) {
    vehicle2carrier.put(event.getVehicleId(), event.getCarrierId());
  }

  @Override
  public void handleEvent(CarrierTourEndEvent event) {
    vehicle2carrier.remove(event.getVehicleId());
  }

  /**
   * @param vehicleId the unique vehicle Id
   * @return  id of the vehicle's carrier
   */
  public Id<Carrier> getCarrierOfVehicle(Id<Vehicle> vehicleId) {
    return vehicle2carrier.get(vehicleId);
  }

}
