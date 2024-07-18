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
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.freight.carriers.events.CarrierServiceEndEvent;
import org.matsim.freight.carriers.events.CarrierServiceStartEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceStartEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourStartEventHandler;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LSPUtils;
import org.matsim.freight.logistics.LogisticChain;

/*package-private*/ class LinearCostTracker
    implements AfterMobsimListener,
        LSPSimulationTracker<LogisticChain>,
        LinkEnterEventHandler,
        VehicleLeavesTrafficEventHandler,
        CarrierTourStartEventHandler,
        CarrierServiceStartEventHandler,
        CarrierServiceEndEventHandler,
        LinkLeaveEventHandler {

  private final Collection<EventHandler> eventHandlers;
  private final double shareOfFixedCosts;
  //	private final Collection<LSPInfo> infos;
  private double distanceCosts;
  private double timeCosts;
  private double loadingCosts;
  private double vehicleFixedCosts;
  private int totalNumberOfShipments;
  private int totalWeightOfShipments;
  private double fixedUnitCosts;
  private double linearUnitCosts;
  private LogisticChain logisticChain;

  public LinearCostTracker(double shareOfFixedCosts) {
    this.shareOfFixedCosts = shareOfFixedCosts;
      this.eventHandlers = new ArrayList<>();
  }

  public final Collection<EventHandler> getEventHandlers() {
    return eventHandlers;
  }

  @Override
  public void notifyAfterMobsim(AfterMobsimEvent event) {
    for (EventHandler handler : eventHandlers) {
      if (handler instanceof TourStartHandler startHandler) {
        this.vehicleFixedCosts = startHandler.getVehicleFixedCosts();
      }
      if (handler instanceof DistanceAndTimeHandler distanceHandler) {
        this.distanceCosts = distanceHandler.getDistanceCosts();
        this.timeCosts = distanceHandler.getTimeCosts();
      }
      if (handler instanceof CollectionServiceHandler collectionHandler) {
        totalNumberOfShipments = collectionHandler.getTotalNumberOfShipments();
        System.out.println(totalNumberOfShipments);
        totalWeightOfShipments = collectionHandler.getTotalWeightOfShipments();
        loadingCosts = collectionHandler.getTotalLoadingCosts();
      }
    }

    double totalCosts = distanceCosts + timeCosts + loadingCosts + vehicleFixedCosts;
    fixedUnitCosts = (totalCosts * shareOfFixedCosts) / totalNumberOfShipments;
    linearUnitCosts = (totalCosts * (1 - shareOfFixedCosts)) / totalWeightOfShipments;

      LSPUtils.setFixedCost(this.logisticChain, fixedUnitCosts);
    LSPUtils.setVariableCost(this.logisticChain, linearUnitCosts);
  }

  @Override
  public void reset(int iteration) {
    distanceCosts = 0;
    timeCosts = 0;
    loadingCosts = 0;
    vehicleFixedCosts = 0;
    totalNumberOfShipments = 0;
    totalWeightOfShipments = 0;
    fixedUnitCosts = 0;
    linearUnitCosts = 0;
  }

  @Override
  public void setEmbeddingContainer(LogisticChain pointer) {
    this.logisticChain = pointer;
  }

  @Override
  public void handleEvent(LinkEnterEvent event) {
    for (EventHandler eventHandler : this.eventHandlers) {
      if (eventHandler instanceof LinkEnterEventHandler) {
        ((LinkEnterEventHandler) eventHandler).handleEvent(event);
      }
    }
  }

  @Override
  public void handleEvent(VehicleLeavesTrafficEvent event) {
    for (EventHandler eventHandler : this.eventHandlers) {
      if (eventHandler instanceof VehicleLeavesTrafficEventHandler) {
        ((VehicleLeavesTrafficEventHandler) eventHandler).handleEvent(event);
      }
    }
  }

  @Override
  public void handleEvent(CarrierTourStartEvent event) {
    for (EventHandler eventHandler : this.eventHandlers) {
      if (eventHandler instanceof CarrierTourStartEventHandler) {
        ((CarrierTourStartEventHandler) eventHandler).handleEvent(event);
      }
    }
  }

  @Override
  public void handleEvent(CarrierServiceEndEvent event) {
    for (EventHandler eventHandler : this.eventHandlers) {
      if (eventHandler instanceof CarrierServiceEndEventHandler) {
        ((CarrierServiceEndEventHandler) eventHandler).handleEvent(event);
      }
    }
  }

  @Override
  public void handleEvent(CarrierServiceStartEvent event) {
    for (EventHandler eventHandler : this.eventHandlers) {
      if (eventHandler instanceof CarrierServiceStartEventHandler) {
        ((CarrierServiceStartEventHandler) eventHandler).handleEvent(event);
      }
    }
  }

  @Override
  public void handleEvent(LinkLeaveEvent event) {
    for (EventHandler eventHandler : this.eventHandlers) {
      if (eventHandler instanceof LinkLeaveEventHandler) {
        ((LinkLeaveEventHandler) eventHandler).handleEvent(event);
      }
    }
  }
}
