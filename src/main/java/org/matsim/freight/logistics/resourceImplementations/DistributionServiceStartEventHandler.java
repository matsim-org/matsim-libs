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
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.events.CarrierServiceStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceStartEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentLeg;
import org.matsim.freight.logistics.shipment.ShipmentPlanElement;
import org.matsim.freight.logistics.shipment.ShipmentUtils;

public class DistributionServiceStartEventHandler
    implements AfterMobsimListener,
        CarrierServiceStartEventHandler,
        LSPSimulationTracker<LSPShipment> {
  // Todo: I have made it (temporarily) public because of junit tests :( -- need to find another way
  // to do the junit testing. kmt jun'23

  private final CarrierService carrierService;
  private final LogisticChainElement logisticChainElement;
  private final LSPCarrierResource resource;
  private LSPShipment lspShipment;

  DistributionServiceStartEventHandler(
      CarrierService carrierService,
      LSPShipment lspShipment,
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
  public void handleEvent(CarrierServiceStartEvent event) {
    if (event.getServiceId() == carrierService.getId()
        && event.getCarrierId() == resource.getCarrier().getId()) {
      logTransport(event);
      logUnload(event);
    }
  }

  private void logTransport(CarrierServiceStartEvent event) {
    String idString = resource.getId() + "" + logisticChainElement.getId() + "TRANSPORT";
    Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
    ShipmentPlanElement abstractPlanElement =
        lspShipment.getShipmentLog().getPlanElements().get(id);
    if (abstractPlanElement instanceof ShipmentLeg transport) {
      transport.setEndTime(event.getTime());
    }
  }

  private void logUnload(CarrierServiceStartEvent event) {
    ShipmentUtils.LoggedShipmentUnloadBuilder builder =
        ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
    builder.setCarrierId(event.getCarrierId());
    builder.setLinkId(event.getLinkId());
    builder.setLogisticChainElement(logisticChainElement);
    builder.setResourceId(resource.getId());
    builder.setStartTime(event.getTime());
    builder.setEndTime(event.getTime() + event.getServiceDuration());
    ShipmentPlanElement unload = builder.build();
    String idString =
        unload.getResourceId()
            + ""
            + unload.getLogisticChainElement().getId()
            + unload.getElementType();
    Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
    lspShipment.getShipmentLog().addPlanElement(unloadId, unload);
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

  public LSPCarrierResource getResource() {
    return resource;
  }

  @Override
  public void setEmbeddingContainer(LSPShipment pointer) {
    this.lspShipment = pointer;
  }

  @Override
  public void notifyAfterMobsim(AfterMobsimEvent event) {}
}
