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

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.events.CarrierServiceEndEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceEndEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentLeg;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

/*package-private*/ class CollectionServiceEndEventHandler
    implements AfterMobsimListener,
        CarrierServiceEndEventHandler,
        LSPSimulationTracker<LspShipment> {

  private final CarrierService carrierService;
  private final LogisticChainElement logisticChainElement;
  private final LSPCarrierResource resource;
  private LspShipment lspShipment;

  public CollectionServiceEndEventHandler(
      CarrierService carrierService,
      LspShipment lspShipment,
      LogisticChainElement element,
      LSPCarrierResource resource) {
    this.carrierService = carrierService;
    this.lspShipment = lspShipment;
    this.logisticChainElement = element;
    this.resource = resource;
  }

  @Override
  public void reset(int iteration) {
    // TODO Auto-generated method stub

  }

  @Override
  public void handleEvent(CarrierServiceEndEvent event) {
    if (event.getServiceId() == carrierService.getId()
        && event.getCarrierId() == resource.getCarrier().getId()) {
      logLoad(event);
      logTransport(event);
    }
  }

  private void logLoad(CarrierServiceEndEvent event) {
    LspShipmentUtils.LoggedShipmentLoadBuilder builder =
        LspShipmentUtils.LoggedShipmentLoadBuilder.newInstance();
    builder.setStartTime(event.getTime() - event.getServiceDuration());
    builder.setEndTime(event.getTime());
    builder.setLogisticsChainElement(logisticChainElement);
    builder.setResourceId(resource.getId());
    builder.setLinkId(event.getLinkId());
    builder.setCarrierId(event.getCarrierId());
    LspShipmentPlanElement loggedShipmentLoad = builder.build();
    String idString =
        loggedShipmentLoad.getResourceId()
            + ""
            + loggedShipmentLoad.getLogisticChainElement().getId()
            + loggedShipmentLoad.getElementType();
    Id<LspShipmentPlanElement> loadId = Id.create(idString, LspShipmentPlanElement.class);
    lspShipment.getShipmentLog().addPlanElement(loadId, loggedShipmentLoad);
  }

  private void logTransport(CarrierServiceEndEvent event) {
    LspShipmentUtils.LoggedShipmentTransportBuilder builder =
        LspShipmentUtils.LoggedShipmentTransportBuilder.newInstance();
    builder.setStartTime(event.getTime());
    builder.setLogisticChainElement(logisticChainElement);
    builder.setResourceId(resource.getId());
    builder.setFromLinkId(event.getLinkId());
    builder.setCarrierId(event.getCarrierId());
    LspShipmentLeg transport = builder.build();
    String idString =
        transport.getResourceId()
            + ""
            + transport.getLogisticChainElement().getId()
            + transport.getElementType();
    Id<LspShipmentPlanElement> transportId = Id.create(idString, LspShipmentPlanElement.class);
    lspShipment.getShipmentLog().addPlanElement(transportId, transport);
  }

  public CarrierService getCarrierService() {
    return carrierService;
  }

  public LspShipment getLspShipment() {
    return lspShipment;
  }

  public LogisticChainElement getElement() {
    return logisticChainElement;
  }

  public Id<LSPResource> getResourceId() {
    return resource.getId();
  }

  @Override
  public void setEmbeddingContainer(LspShipment pointer) {
    this.lspShipment = pointer;
  }

  @Override
  public void notifyAfterMobsim(AfterMobsimEvent event) {}
}
