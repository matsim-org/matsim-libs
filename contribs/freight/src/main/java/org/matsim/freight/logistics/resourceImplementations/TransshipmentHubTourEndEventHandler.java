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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourEndEventHandler;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.events.HandlingInHubStartsEvent;
import org.matsim.freight.logistics.shipment.*;

/*package-private*/ class TransshipmentHubTourEndEventHandler
    implements AfterMobsimListener, LSPSimulationTracker<LSPResource>, CarrierTourEndEventHandler {

  //This class *should* also get merged into {@link LSPTourEndEventHandler}.
  //Currently, this is not possible very easily, because of missing injection (of the scenario.)
  //KMT, KN (Jan'24)

  private final Scenario scenario;
  private final HashMap<CarrierService, TransshipmentHubEventHandlerPair> servicesWaitedFor;
  private final TransshipmentHubResource transshipmentHubResource;
  private final Id<LSPResource> resourceId;
  private final Id<Link> linkId;
  private EventsManager eventsManager;

  /**
   * This is a TourEndEvent-Handler, doing some stuff regarding the {@link
   * TransshipmentHubResource}.
   *
   * @param transshipmentHubResource hub
   * @param scenario The scenario. Is used to get the Carrier(s).
   */
  TransshipmentHubTourEndEventHandler(
      TransshipmentHubResource transshipmentHubResource, Scenario scenario) {
    this.transshipmentHubResource = transshipmentHubResource;
    this.linkId = transshipmentHubResource.getEndLinkId();
    this.resourceId = transshipmentHubResource.getId();
    this.scenario = scenario;
    this.servicesWaitedFor = new HashMap<>();
    this.transshipmentHubResource.addSimulationTracker(this);
  }

  @Override
  public void setEmbeddingContainer(LSPResource pointer) {}

  @Override
  public void setEventsManager(EventsManager eventsManager) {
    this.eventsManager = eventsManager;
  }

  @Override
  public void notifyAfterMobsim(AfterMobsimEvent event) {
    servicesWaitedFor
        .clear(); // cleanup after Mobsim ends (instead of doing it in reset() = before Mobsim
                  // starts.) kmt oct'22
  }

  @Override
  public void reset(int iteration) {
    // not implemented; cleanup is done after Mobsim ends, because the internal state is (re)set
    // before Mobsim starts.
    // --> cleaning up here is too late.
    // This is maybe not ideal, but works; kmt oct'22
  }

  public void addShipment(
          LspShipment lspShipment, LogisticChainElement logisticChainElement, LspShipmentPlan lspShipmentPlan) {
    TransshipmentHubEventHandlerPair pair =
        new TransshipmentHubEventHandlerPair(lspShipment, logisticChainElement);

    for (LspShipmentPlanElement planElement : lspShipmentPlan.getPlanElements().values()) {
      if (planElement instanceof LspShipmentLeg transport) {
        if (transport.getLogisticChainElement().getNextElement() == logisticChainElement) {
          servicesWaitedFor.put(transport.getCarrierService(), pair);
        }
      }
    }
  }

  @Override
  public void handleEvent(CarrierTourEndEvent event) {
    Tour tour = null;
    Carrier carrier = CarriersUtils.getCarriers(scenario).getCarriers().get(event.getCarrierId());
    Collection<ScheduledTour> scheduledTours = carrier.getSelectedPlan().getScheduledTours();
    for (ScheduledTour scheduledTour : scheduledTours) {
      if (scheduledTour.getTour().getId() == event.getTourId()) {
        tour = scheduledTour.getTour();
        break;
      }
    }
    if ((event.getLinkId() == this.linkId)) {
      assert tour != null;

      if (ResourceImplementationUtils.getCarrierType(carrier)
          == ResourceImplementationUtils.CARRIER_TYPE.mainRunCarrier) {
        if (allShipmentsOfTourEndInOnePoint(tour)) {
          for (TourElement tourElement : tour.getTourElements()) {
            if (tourElement instanceof ServiceActivity serviceActivity) {
              if (serviceActivity.getLocation() == transshipmentHubResource.getStartLinkId()
                  && allServicesAreInOnePoint(tour)
                  && (tour.getStartLinkId() != transshipmentHubResource.getStartLinkId())) {

                final CarrierService carrierService = serviceActivity.getService();
                final LspShipment lspShipment = servicesWaitedFor.get(carrierService).lspShipment;
                // NOTE: Do NOT add time vor unloading all goods, because they are included for the
                // main run (Service activity at end of tour)
                final double expHandlingDuration =
                    transshipmentHubResource.getCapacityNeedFixed()
                        + (transshipmentHubResource.getCapacityNeedLinear()
                            * lspShipment.getSize());
                final double startTime = event.getTime();
                final double endTime = startTime + expHandlingDuration;

                logHandlingInHub(serviceActivity.getService(), startTime, endTime);
                throwHandlingEvent(event, lspShipment, expHandlingDuration);
              }
            }
          }
        }
      } else if ((ResourceImplementationUtils.getCarrierType(carrier)
          == ResourceImplementationUtils.CARRIER_TYPE.collectionCarrier)) {
        for (TourElement tourElement : tour.getTourElements()) {
          if (tourElement instanceof ServiceActivity serviceActivity) {
            if (tour.getEndLinkId() == transshipmentHubResource.getStartLinkId()) {

              final CarrierService carrierService = serviceActivity.getService();
              final LspShipment lspShipment = servicesWaitedFor.get(carrierService).lspShipment;

              // TODO: Adding this here to be more in line with the schedule and have the shipment
              // log fitting to it.
              // This does NOT mean, that it really makes sense, because we decided for some
              // reasons, that the handlingEvent start once the vehicle has arrived.
              // It may change again, once we have unloading events available.
              final double expUnloadingTime = getTotalUnloadingTime(tour);
              final double expHandlingDuration =
                  transshipmentHubResource.getCapacityNeedFixed()
                      + (transshipmentHubResource.getCapacityNeedLinear() * lspShipment.getSize());
              final double startTime = event.getTime() + expUnloadingTime;
              final double endTime = startTime + expHandlingDuration;

              logHandlingInHub(carrierService, startTime, endTime);
              throwHandlingEvent(event, lspShipment, expHandlingDuration);
            }
          }
        }
      }
    }
  }

  private boolean allShipmentsOfTourEndInOnePoint(Tour tour) {
    boolean allShipmentsOfTourEndInOnePoint = true;
    for (TourElement tourElement : tour.getTourElements()) {
      if (tourElement instanceof ServiceActivity serviceActivity) {
        if (!servicesWaitedFor.containsKey(serviceActivity.getService())) {
          return false;
        }
      }
    }
    return allShipmentsOfTourEndInOnePoint;
  }

  private double getTotalUnloadingTime(Tour tour) {
    double totalTime = 0;
    for (TourElement element : tour.getTourElements()) {
      if (element instanceof ServiceActivity serviceActivity) {
        totalTime = totalTime + serviceActivity.getDuration();
      }
    }
    return totalTime;
  }

  private void logHandlingInHub(
          CarrierService carrierService, double startTime, double endTime) {

    LspShipment lspShipment = servicesWaitedFor.get(carrierService).lspShipment;

    { // Old logging approach - will be removed at some point in time
      LspShipmentPlanElement handle =
          LspShipmentUtils.LoggedShipmentHandleBuilder.newInstance()
              .setLinkId(linkId)
              .setResourceId(resourceId)
              .setStartTime(startTime)
              .setEndTime(endTime)
              .setLogisticsChainElement(servicesWaitedFor.get(carrierService).logisticChainElement)
              .build();
      Id<LspShipmentPlanElement> loadId =
          Id.create(
              handle.getResourceId()
                  + String.valueOf(handle.getLogisticChainElement().getId())
                  + handle.getElementType(),
              LspShipmentPlanElement.class);
      if (!lspShipment.getShipmentLog().getPlanElements().containsKey(loadId)) {
        lspShipment.getShipmentLog().addPlanElement(loadId, handle);
      }
    }
  }

  private void throwHandlingEvent(
          CarrierTourEndEvent event, LspShipment lspShipment, double expHandlingDuration) {
    // New event-based approach
    // Todo: We need to decide what we write into the exp. handling duration: See #175 for
    // discussion.
    // The start time, must start at the same time as the triggering event. -> keep events stream
    // ordered.
    eventsManager.processEvent(
        new HandlingInHubStartsEvent(
            event.getTime(), linkId, lspShipment.getId(), resourceId, expHandlingDuration));
  }

  private boolean allServicesAreInOnePoint(Tour tour) {
    for (TourElement element : tour.getTourElements()) {
      if (element instanceof ServiceActivity activity) {
        if (activity.getLocation() != tour.getEndLinkId()) {
          return false;
        }
      }
    }
    return true;
  }

  public Map<CarrierService, TransshipmentHubEventHandlerPair> getServicesWaitedFor() {
    return servicesWaitedFor;
  }

  public TransshipmentHubResource getTranshipmentHub() {
    return transshipmentHubResource;
  }

  public Id<LSPResource> getResourceId() {
    return resourceId;
  }

  public Id<Link> getLinkId() {
    return linkId;
  }

  public static class TransshipmentHubEventHandlerPair {
    public final LspShipment lspShipment;
    public final LogisticChainElement logisticChainElement;

    public TransshipmentHubEventHandlerPair(LspShipment lspShipment, LogisticChainElement element) {
      this.lspShipment = lspShipment;
      this.logisticChainElement = element;
    }
  }
}
