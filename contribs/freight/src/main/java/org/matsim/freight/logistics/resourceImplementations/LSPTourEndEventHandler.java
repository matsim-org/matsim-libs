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

import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourEndEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LspShipment;

/*package-private*/ class LSPTourEndEventHandler extends AbstractLspShipmentEventHandler implements CarrierTourEndEventHandler {

  private final CarrierService carrierService;
  private final Tour tour;
  private final double unloadingDuration;

  /**
   * @param unloadingDuration the time needed at the end of the tour to unload all shipments of the tour, i.e. the sum of
   *                          their delivery service times
   */
  public LSPTourEndEventHandler(
      LspShipment lspShipment,
      CarrierService carrierService,
      LogisticChainElement logisticChainElement,
      LSPCarrierResource resource,
      Tour tour,
      double unloadingDuration) {
    super(lspShipment, logisticChainElement, resource);
    this.carrierService = carrierService;
    this.tour = tour;
    this.unloadingDuration = unloadingDuration;
  }

  @Override
  public void handleEvent(CarrierTourEndEvent event) {
    if (event.getTourId().equals(tour.getId())) {
      for (TourElement element : tour.getTourElements()) {
        if (element instanceof ServiceActivity serviceActivity) {
          if (serviceActivity.getService().getId() == carrierService.getId()
              && isCarrierOfResource(event.getCarrierId())) {
            if (getCarrierResource() instanceof CollectionCarrierResource) {
              logTransportEnd(event.getTime(), tour.getEndLinkId());
              logUnload(event.getCarrierId(), null, event.getTime(), event.getTime() + unloadingDuration);
            } else if (getCarrierResource() instanceof MainRunCarrierResource) {
              logUnload(event.getCarrierId(), null, event.getTime() - unloadingDuration, event.getTime());
              logTransportEnd(event.getTime() - unloadingDuration, event.getLinkId());
            }
          }
        }
      }
    }
  }


  public CarrierService getCarrierService() {
    return carrierService;
  }
}
