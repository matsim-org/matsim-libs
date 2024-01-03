/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.resourceImplementations.distributionCarrier;

import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourStartEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentPlanElement;
import org.matsim.freight.logistics.shipment.ShipmentUtils;

public class DistributionTourStartEventHandler
    implements CarrierTourStartEventHandler, LSPSimulationTracker<LSPShipment> {
  // Todo: I have made it (temporarily) public because of junit tests :( -- need to find another way
  // to do the junit testing. kmt jun'23

  private final CarrierService carrierService;
  private final LogisticChainElement element;
  private final LSPCarrierResource resource;
  private final Tour tour;
  private LSPShipment lspShipment;

  DistributionTourStartEventHandler(
      CarrierService carrierService,
      LSPShipment lspShipment,
      LogisticChainElement element,
      LSPCarrierResource resource,
      Tour tour) {
    this.carrierService = carrierService;
    this.lspShipment = lspShipment;
    this.element = element;
    this.resource = resource;
    this.tour = tour;
  }

  @Override
  public void reset(int iteration) {
    // TODO Auto-generated method stub
  }

  @Override
  public void handleEvent(CarrierTourStartEvent event) {
    if (event.getTourId().equals(tour.getId())) {
      for (TourElement tourElement : tour.getTourElements()) {
        if (tourElement instanceof ServiceActivity serviceActivity) {
          if (serviceActivity.getService().getId() == carrierService.getId()
              && event.getCarrierId() == resource.getCarrier().getId()) {
            logLoad(event);
            logTransport(event);
          }
        }
      }
    }
  }

  private void logLoad(CarrierTourStartEvent event) {
    ShipmentUtils.LoggedShipmentLoadBuilder builder =
        ShipmentUtils.LoggedShipmentLoadBuilder.newInstance();
    builder.setCarrierId(event.getCarrierId());
    builder.setLinkId(event.getLinkId());
    builder.setLogisticsChainElement(element);
    builder.setResourceId(resource.getId());
    builder.setEndTime(event.getTime());
    builder.setStartTime(event.getTime() - getCumulatedLoadingTime(tour));
    ShipmentPlanElement loggedShipmentLoad = builder.build();
    String idString =
        loggedShipmentLoad.getResourceId()
            + ""
            + loggedShipmentLoad.getLogisticChainElement().getId()
            + loggedShipmentLoad.getElementType();
    Id<ShipmentPlanElement> loadId = Id.create(idString, ShipmentPlanElement.class);
    lspShipment.getShipmentLog().addPlanElement(loadId, loggedShipmentLoad);
  }

  private void logTransport(CarrierTourStartEvent event) {
    ShipmentUtils.LoggedShipmentTransportBuilder builder =
        ShipmentUtils.LoggedShipmentTransportBuilder.newInstance();
    builder.setCarrierId(event.getCarrierId());
    builder.setFromLinkId(event.getLinkId());
    builder.setToLinkId(tour.getEndLinkId());
    builder.setLogisticChainElement(element);
    builder.setResourceId(resource.getId());
    builder.setStartTime(event.getTime());
    ShipmentPlanElement transport = builder.build();
    String idString =
        transport.getResourceId()
            + ""
            + transport.getLogisticChainElement().getId()
            + transport.getElementType();
    Id<ShipmentPlanElement> transportId = Id.create(idString, ShipmentPlanElement.class);
    lspShipment.getShipmentLog().addPlanElement(transportId, transport);
  }

  private double getCumulatedLoadingTime(Tour tour) {
    double cumulatedLoadingTime = 0;
    for (TourElement tourElement : tour.getTourElements()) {
      if (tourElement instanceof ServiceActivity serviceActivity) {
        cumulatedLoadingTime = cumulatedLoadingTime + serviceActivity.getDuration();
      }
    }
    return cumulatedLoadingTime;
  }

  public CarrierService getCarrierService() {
    return carrierService;
  }

  public LSPShipment getLspShipment() {
    return lspShipment;
  }

  public LogisticChainElement getElement() {
    return element;
  }

  public LSPCarrierResource getResource() {
    return resource;
  }

  @Override
  public void setEmbeddingContainer(LSPShipment pointer) {
    this.lspShipment = pointer;
  }
}
