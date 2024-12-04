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

package org.matsim.freight.logistics.resourceImplementations;

import java.util.Objects;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourStartEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentLeg;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

/*package-private*/ class LSPTourStartEventHandler
        implements CarrierTourStartEventHandler, LSPSimulationTracker<LspShipment> {

  private final Tour tour;
  private final CarrierService carrierService;
  private final CarrierShipment carrierShipment;
  private final LogisticChainElement logisticChainElement;
  private final LSPCarrierResource resource;
  private LspShipment lspShipment;

  public LSPTourStartEventHandler(
          LspShipment lspShipment,
          CarrierService carrierService,
          LogisticChainElement logisticChainElement,
          LSPCarrierResource resource,
          Tour tour,
          CarrierShipment carrierShipment)
  {
    this.lspShipment = lspShipment;
    this.carrierService = carrierService;
    this.carrierShipment = carrierShipment;
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
    if (event.getTourId().equals(tour.getId())  && event.getCarrierId() == resource.getCarrier().getId()) {
      for (TourElement tourElement : tour.getTourElements()) {
        switch (tourElement) {
          //This could even be short, if the Id would be available already on the level of Tour.TourActivity. KMT Oct'24
          case ServiceActivity serviceActivity -> {
            if (serviceActivity.getService().getId() == carrierService.getId()) {
              logLoadAndTransport(event);
            }
          }
          case Tour.ShipmentBasedActivity shipmentBasedActivity -> {
            if (Objects.equals(shipmentBasedActivity.getShipment().getId().toString(), carrierService.getId().toString())) {
              logLoadAndTransport(event);
            }
          }
          case null, default -> {}
        }
      }
    }
  }

  private void logLoadAndTransport(CarrierTourStartEvent event) {
    if (resource instanceof DistributionCarrierResource || resource instanceof MainRunCarrierResource) {
      logLoad(event.getCarrierId(), event.getLinkId(), event.getTime() - getCumulatedLoadingTime(tour),  event.getTime());
      logTransport(event.getCarrierId(),  event.getLinkId(), tour.getEndLinkId(), event.getTime());
    }
  }

  private void logLoad(Id<Carrier> carrierId, Id<Link> linkId, double startTime, double endTime) {
    LspShipmentUtils.LoggedShipmentLoadBuilder builder =
            LspShipmentUtils.LoggedShipmentLoadBuilder.newInstance();
    builder.setCarrierId(carrierId);
    builder.setLinkId(linkId);
    builder.setLogisticsChainElement(logisticChainElement);
    builder.setResourceId(resource.getId());
    builder.setStartTime(startTime);
    builder.setEndTime(endTime);
    LspShipmentPlanElement loggedShipmentLoad = builder.build();
    String idString =
            loggedShipmentLoad.getResourceId()
                    + ""
                    + loggedShipmentLoad.getLogisticChainElement().getId()
                    + loggedShipmentLoad.getElementType();
    Id<LspShipmentPlanElement> loadId = Id.create(idString, LspShipmentPlanElement.class);
    lspShipment.getShipmentLog().addPlanElement(loadId, loggedShipmentLoad);
  }

  private void logTransport(Id<Carrier> carrierId, Id<Link> fromLinkId,
                            Id<Link> toLinkId, double startTime) {
    LspShipmentUtils.LoggedShipmentTransportBuilder builder =
            LspShipmentUtils.LoggedShipmentTransportBuilder.newInstance();
    builder.setCarrierId(carrierId);
    builder.setFromLinkId(fromLinkId);
    builder.setToLinkId(toLinkId);
    builder.setLogisticChainElement(logisticChainElement);
    builder.setResourceId(resource.getId());
    builder.setStartTime(startTime);
    LspShipmentLeg transport = builder.build();
    String idString =
            transport.getResourceId()
                    + ""
                    + transport.getLogisticChainElement().getId()
                    + transport.getElementType();
    Id<LspShipmentPlanElement> transportId = Id.create(idString, LspShipmentPlanElement.class);
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

  public LspShipment getLspShipment() {
    return lspShipment;
  }

  public LogisticChainElement getLogisticChainElement() {
    return logisticChainElement;
  }

  public Id<LSPResource> getResourceId() {
    return resource.getId();
  }

  @Override
  public void setEmbeddingContainer(LspShipment pointer) {
    this.lspShipment = pointer;
  }
}
