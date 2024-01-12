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

package org.matsim.freight.logistics.resourceImplementations;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourStartEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentLeg;
import org.matsim.freight.logistics.shipment.ShipmentPlanElement;
import org.matsim.freight.logistics.shipment.ShipmentUtils;

public class LSPTourStartEventHandler
    implements CarrierTourStartEventHandler, LSPSimulationTracker<LSPShipment> {
  // Todo: I have made it (temporarily) public because of junit tests :( -- need to find another way
  // to do the junit testing. kmt jun'23

  private final CarrierService carrierService;
  private final LogisticChainElement logisticChainElement;
  private final LSPCarrierResource resource;
  private final Tour tour;
  private LSPShipment lspShipment;

  LSPTourStartEventHandler(
      LSPShipment lspShipment,
      CarrierService carrierService,
      LogisticChainElement logisticChainElement,
      LSPCarrierResource resource,
      Tour tour) {
    this.lspShipment = lspShipment;
    this.carrierService = carrierService;
    this.logisticChainElement = logisticChainElement;
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
            if (resource instanceof DistributionCarrierResource) {
              logLoad(
                  event.getCarrierId(),
                  event.getLinkId(),
                  event.getTime() - getCumulatedLoadingTime(tour),
                  event.getTime());
              logTransport(
                  event.getCarrierId(), event.getLinkId(), tour.getEndLinkId(), event.getTime());
            } // else if (resource instanceof MainRunCarrierResource) {
                //....
//            }

          }
        }
      }
    }
  }

  private void logLoad(Id<Carrier> carrierId, Id<Link> linkId,
      double startTime, double endTime) {
    ShipmentUtils.LoggedShipmentLoadBuilder builder =
        ShipmentUtils.LoggedShipmentLoadBuilder.newInstance();
    builder.setCarrierId(carrierId);
    builder.setLinkId(linkId);
    builder.setLogisticsChainElement(logisticChainElement);
    builder.setResourceId(resource.getId());
    builder.setStartTime(startTime);
    builder.setEndTime(endTime);
    ShipmentPlanElement loggedShipmentLoad = builder.build();
    String idString =
        loggedShipmentLoad.getResourceId()
            + ""
            + loggedShipmentLoad.getLogisticChainElement().getId()
            + loggedShipmentLoad.getElementType();
    Id<ShipmentPlanElement> loadId = Id.create(idString, ShipmentPlanElement.class);
    lspShipment.getShipmentLog().addPlanElement(loadId, loggedShipmentLoad);
  }

  private void logTransport(Id<Carrier> carrierId, Id<Link> fromLinkId,
      Id<Link> toLinkId, double startTime) {
    ShipmentUtils.LoggedShipmentTransportBuilder builder =
        ShipmentUtils.LoggedShipmentTransportBuilder.newInstance();
    builder.setCarrierId(carrierId);
    builder.setFromLinkId(fromLinkId);
    builder.setToLinkId(toLinkId);
    builder.setLogisticChainElement(logisticChainElement);
    builder.setResourceId(resource.getId());
    builder.setStartTime(startTime);
    ShipmentLeg transport = builder.build();
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

  public LogisticChainElement getLogisticChainElement() {
    return logisticChainElement;
  }

  public Id<LSPResource> getResourceId() {
    return resource.getId();
  }

  @Override
  public void setEmbeddingContainer(LSPShipment pointer) {
    this.lspShipment = pointer;
  }
}
