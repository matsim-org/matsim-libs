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
import org.matsim.freight.carriers.CarrierJob;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourStartEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LspShipment;

/*package-private*/ class LSPTourStartEventHandler extends AbstractLspShipmentEventHandler implements CarrierTourStartEventHandler {

  private final Tour tour;
  private final CarrierJob carrierJob;
  private final double loadingDuration;

  /**
   * @param loadingDuration the time needed before the tour start to load all shipments of the tour, i.e. the sum of their
   *                        pickup service times
   */
  public LSPTourStartEventHandler(LspShipment lspShipment, CarrierJob carrierJob, LogisticChainElement logisticChainElement, LSPCarrierResource resource,
                                  Tour tour, double loadingDuration) {
    super(lspShipment, logisticChainElement, resource);
    this.carrierJob = carrierJob;
    this.tour = tour;
    this.loadingDuration = loadingDuration;
  }

  @Override
  public void handleEvent(CarrierTourStartEvent event) {
    if (event.getTourId().equals(tour.getId()) && isCarrierOfResource(event.getCarrierId())) {
      for (TourElement tourElement : tour.getTourElements()) {
        switch (tourElement) {
          //This could even be short, if the Id would be available already on the level of Tour.TourActivity. KMT Oct'24
          case ServiceActivity serviceActivity -> {
            if (serviceActivity.getService().getId() == carrierJob.getId()) {
              logLoadAndTransport(event);
            }
          }
          case Tour.ShipmentBasedActivity shipmentBasedActivity -> {
            if (Objects.equals(shipmentBasedActivity.getShipment().getId().toString(), carrierJob.getId().toString())) {
              logLoadAndTransport(event);
            }
          }
          case null, default -> {}
        }
      }
    }
  }

  private void logLoadAndTransport(CarrierTourStartEvent event) {
    if (getCarrierResource() instanceof DistributionCarrierResource || getCarrierResource() instanceof MainRunCarrierResource) {
      logLoad(event.getCarrierId(), event.getLinkId(), event.getTime() - loadingDuration, event.getTime());
      logTransportStart(event.getCarrierId(), event.getLinkId(), tour.getEndLinkId(), event.getTime());
    }
  }


  public CarrierJob getCarrierJob() {
    return carrierJob;
  }
}
