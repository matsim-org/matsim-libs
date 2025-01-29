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
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.events.CarrierServiceStartEvent;
import org.matsim.freight.carriers.events.CarrierShipmentDeliveryStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceStartEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierShipmentDeliveryStartEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentLeg;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

import static org.matsim.freight.logistics.LSPConstants.TRANSPORT;

/*package-private*/ class DistributionServiceStartEventHandler
        implements AfterMobsimListener,
        CarrierServiceStartEventHandler,
        CarrierShipmentDeliveryStartEventHandler,
        LSPSimulationTracker<LspShipment> {

  private final CarrierService carrierService;
  private final CarrierShipment carrierShipment;
  private final LogisticChainElement logisticChainElement;
  private final LSPCarrierResource resource;
  private LspShipment lspShipment;

  DistributionServiceStartEventHandler(
          CarrierService carrierService,
          LspShipment lspShipment,
          LogisticChainElement element,
          LSPCarrierResource resource,
          CarrierShipment carrierShipment) {
    this.carrierShipment = carrierShipment;
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

  @Override
  public void handleEvent(CarrierShipmentDeliveryStartEvent event) {
    if (event.getShipmentId() == this.carrierShipment.getId()
            && event.getCarrierId() == resource.getCarrier().getId()) {
      logTransport(event);
      logUnload(event);
    }
  }

  private void logTransport(CarrierServiceStartEvent event) {
    String idString = resource.getId() + "" + logisticChainElement.getId() + TRANSPORT;
    Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
    LspShipmentPlanElement abstractPlanElement =
            lspShipment.getShipmentLog().getPlanElements().get(id);
    if (abstractPlanElement instanceof LspShipmentLeg transport) {
      transport.setEndTime(event.getTime());
    }
  }

  //TODO: Inhaltlich ansehen, was hier passiert. Ist aktuell nur Copy und Paste aus Service-Variante
  private void logTransport(CarrierShipmentDeliveryStartEvent event) {
    String idString = resource.getId() + "" + logisticChainElement.getId() + TRANSPORT;
    Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
    LspShipmentPlanElement abstractPlanElement =
            lspShipment.getShipmentLog().getPlanElements().get(id);
    if (abstractPlanElement instanceof LspShipmentLeg transport) {
      transport.setEndTime(event.getTime());
    }
  }

  private void logUnload(CarrierServiceStartEvent event) {
    LspShipmentUtils.LoggedShipmentUnloadBuilder builder =
            LspShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
    builder.setCarrierId(event.getCarrierId());
    builder.setLinkId(event.getLinkId());
    builder.setLogisticChainElement(logisticChainElement);
    builder.setResourceId(resource.getId());
    builder.setStartTime(event.getTime());
    builder.setEndTime(event.getTime() + event.getServiceDuration());
    LspShipmentPlanElement unload = builder.build();
    String idString =
            unload.getResourceId()
                    + ""
                    + unload.getLogisticChainElement().getId()
                    + unload.getElementType();
    Id<LspShipmentPlanElement> unloadId = Id.create(idString, LspShipmentPlanElement.class);
    lspShipment.getShipmentLog().addPlanElement(unloadId, unload);
  }

  //TODO: Inhaltlich ansehen, was hier passiert. Ist aktuell nur Copy und Paste aus Service-Variante
  private void logUnload(CarrierShipmentDeliveryStartEvent event) {
    LspShipmentUtils.LoggedShipmentUnloadBuilder builder =
            LspShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
    builder.setCarrierId(event.getCarrierId());
    builder.setLinkId(event.getLinkId());
    builder.setLogisticChainElement(logisticChainElement);
    builder.setResourceId(resource.getId());
    builder.setStartTime(event.getTime());
    builder.setEndTime(event.getTime() + event.getDeliveryDuration());
    LspShipmentPlanElement unload = builder.build();
    String idString =
            unload.getResourceId()
                    + ""
                    + unload.getLogisticChainElement().getId()
                    + unload.getElementType();
    Id<LspShipmentPlanElement> unloadId = Id.create(idString, LspShipmentPlanElement.class);
    lspShipment.getShipmentLog().addPlanElement(unloadId, unload);
  }

  //Todo: Wird das auch inhaltlich irgendwo genutzt,oder ist das nur für die Tests da?
  //todo ctd. Brauchen wir den CarrierService hier eigentlich wirklich oder kann das zurück gebaut werden? KMT Okt'24
  public CarrierService getCarrierService() {
    return carrierService;
  }

  public LspShipment getLspShipment() {
    return lspShipment;
  }

  public LogisticChainElement getLogisticChainElement() {
    return logisticChainElement;
  }

  public LSPCarrierResource getResource() {
    return resource;
  }

  @Override
  public void setEmbeddingContainer(LspShipment pointer) {
    this.lspShipment = pointer;
  }

  @Override
  public void notifyAfterMobsim(AfterMobsimEvent event) {}
}
