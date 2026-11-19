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

import org.matsim.freight.carriers.CarrierJob;
import org.matsim.freight.carriers.events.CarrierServiceStartEvent;
import org.matsim.freight.carriers.events.CarrierShipmentDeliveryStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceStartEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierShipmentDeliveryStartEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LspShipment;

/*package-private*/ class DistributionJobEventHandler extends AbstractLspShipmentEventHandler
        implements CarrierServiceStartEventHandler, CarrierShipmentDeliveryStartEventHandler {

  private final CarrierJob carrierJob;

  DistributionJobEventHandler(CarrierJob carrierJob, LspShipment lspShipment, LogisticChainElement element, LSPCarrierResource resource) {
    super(lspShipment, element, resource);
    this.carrierJob = carrierJob;
  }

  @Override
  public void handleEvent(CarrierServiceStartEvent event) {
    if (event.getServiceId() == carrierJob.getId() && isCarrierOfResource(event.getCarrierId())) {
      logTransportEnd(event.getTime(), null);
      logUnload(event.getCarrierId(), event.getLinkId(), event.getTime(), event.getTime() + event.getServiceDuration());
    }
  }

  //TODO: Inhaltlich ansehen, was hier passiert. Ist aktuell nur Copy und Paste aus Service-Variante
  @Override
  public void handleEvent(CarrierShipmentDeliveryStartEvent event) {
    if (event.getShipmentId() == this.carrierJob.getId() && isCarrierOfResource(event.getCarrierId())) {
      logTransportEnd(event.getTime(), null);
      logUnload(event.getCarrierId(), event.getLinkId(), event.getTime(), event.getTime() + event.getDeliveryDuration());
    }
  }

  //Todo: Wird das auch inhaltlich irgendwo genutzt,oder ist das nur für die Tests da?
  //todo ctd. Brauchen wir den CarrierService hier eigentlich wirklich oder kann das zurück gebaut werden? KMT Okt'24
  public CarrierJob getCarrierJob() {return carrierJob;  }

  public LSPCarrierResource getResource() {
    return getCarrierResource();
  }
}
