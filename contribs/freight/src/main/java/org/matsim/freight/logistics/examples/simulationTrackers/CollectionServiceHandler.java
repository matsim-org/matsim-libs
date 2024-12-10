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

import java.util.ArrayList;
import java.util.Collection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.events.CarrierServiceEndEvent;
import org.matsim.freight.carriers.events.CarrierServiceStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceStartEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/*package-private*/ class CollectionServiceHandler
    implements CarrierServiceStartEventHandler, CarrierServiceEndEventHandler {

  private final Collection<ServiceTuple> tuples;
  private final Vehicles allVehicles;
  private double totalLoadingCosts;
  private int totalNumberOfShipments;
  private int totalWeightOfShipments;

  public CollectionServiceHandler(Scenario scenario) {
    this.allVehicles = VehicleUtils.getOrCreateAllvehicles(scenario);
    this.tuples = new ArrayList<>();
  }

  @Override
  public void reset(int iteration) {
    tuples.clear();
    totalNumberOfShipments = 0;
    totalWeightOfShipments = 0;
  }

  @Override
  public void handleEvent(CarrierServiceEndEvent event) {
    System.out.println("Service Ends");
    double loadingCosts;
    for (ServiceTuple tuple : tuples) {
      if (tuple.getServiceId() == event.getServiceId()) {
        double serviceDuration = event.getTime() - tuple.getStartTime();

        final Vehicle vehicle = allVehicles.getVehicles().get(event.getVehicleId());
        loadingCosts = serviceDuration * vehicle.getType().getCostInformation().getCostsPerSecond();
        totalLoadingCosts = totalLoadingCosts + loadingCosts;
        tuples.remove(tuple);
        break;
      }
    }
  }

  @Override
  public void handleEvent(CarrierServiceStartEvent event) {
    totalNumberOfShipments++;
    totalWeightOfShipments = totalWeightOfShipments + event.getCapacityDemand();
    tuples.add(new ServiceTuple(event.getServiceId(), event.getTime()));
  }

  public double getTotalLoadingCosts() {
    return totalLoadingCosts;
  }

  public int getTotalNumberOfShipments() {
    return totalNumberOfShipments;
  }

  public int getTotalWeightOfShipments() {
    return totalWeightOfShipments;
  }

  private static class ServiceTuple {
    private final Id<CarrierService> serviceId;
    private final double startTime;

    public ServiceTuple(Id<CarrierService> serviceId, double startTime) {
      this.serviceId = serviceId;
      this.startTime = startTime;
    }

    public Id<CarrierService> getServiceId() {
      return serviceId;
    }

    public double getStartTime() {
      return startTime;
    }
  }
}
