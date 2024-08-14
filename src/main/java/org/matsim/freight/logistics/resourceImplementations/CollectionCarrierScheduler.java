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

import java.util.ArrayList;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.Tour.Leg;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

/**
 * Schedules the {@link CollectionCarrierResource}.
 *
 * <p>Converts the {@link LspShipment}s into {@link CarrierService}s that are needed for the {@link
 * Carrier} from the freight contrib of MATSim and then routes the vehicles of this {@link Carrier}
 * through the network by calling the corresponding methods of jsprit
 */
/*package-private*/ class CollectionCarrierScheduler extends LSPResourceScheduler {

  private Carrier carrier;
  private CollectionCarrierResource resource;
  private ArrayList<LSPCarrierPair> pairs;
  private final Scenario scenario;

  /**
   * Constructor for the CollectionCarrierScheduler.
   * TODO: In the future, the scenario should come via injection(?) This here is only a dirty workaround. KMT'Aug'24
   *
   * @param scenario the road pricing scheme
   */
  CollectionCarrierScheduler(Scenario scenario) {
    this.pairs = new ArrayList<>();
    this.scenario = scenario;
  }

  @Override
  public void initializeValues(LSPResource resource) {
    this.pairs = new ArrayList<>();
    if (resource.getClass() == CollectionCarrierResource.class) {
      this.resource = (CollectionCarrierResource) resource;
      this.carrier = this.resource.getCarrier();
      this.carrier.getServices().clear();
      this.carrier.getShipments().clear();
      this.carrier.getPlans().clear();
    }
  }

  @Override
  public void scheduleResource() {
    for (LspShipment lspShipmentToBeAssigned : lspShipmentsToSchedule) {
      CarrierService carrierService = convertToCarrierService(lspShipmentToBeAssigned);
      carrier.getServices().put(carrierService.getId(), carrierService);
    }
    CarrierSchedulerUtils.solveVrpWithJsprit(carrier, scenario);
  }

  private CarrierService convertToCarrierService(LspShipment lspShipment) {
    Id<CarrierService> serviceId = Id.create(lspShipment.getId().toString(), CarrierService.class);
    CarrierService carrierService = CarrierService.Builder.newInstance(serviceId, lspShipment.getFrom())
            .setServiceStartTimeWindow(lspShipment.getPickupTimeWindow())
            .setCapacityDemand(lspShipment.getSize())
            .setServiceDuration(lspShipment.getDeliveryServiceTime())
            .build();
    pairs.add(new LSPCarrierPair(lspShipment, carrierService));
    return carrierService;
  }

  @Override
  protected void updateShipments() {
    for (LspShipment lspShipment : lspShipmentsToSchedule) {
      for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
        Tour tour = scheduledTour.getTour();
        for (TourElement element : tour.getTourElements()) {
          if (element instanceof ServiceActivity serviceActivity) {
            for (LSPCarrierPair pair : pairs) {
              if (pair.lspShipment == lspShipment
                  && pair.carrierService.getId() == serviceActivity.getService().getId()) {
                addShipmentLoadElement(lspShipment, tour, serviceActivity);
                addShipmentTransportElement(lspShipment, tour, serviceActivity);
                addShipmentUnloadElement(lspShipment, tour);
                addCollectionTourEndEventHandler(pair.carrierService, lspShipment, resource, tour);
                addCollectionServiceEventHandler(pair.carrierService, lspShipment, resource);
              }
            }
          }
        }
      }
    }
  }

  private void addShipmentLoadElement(
      LspShipment lspShipment, Tour tour, ServiceActivity serviceActivity) {

    LspShipmentUtils.ScheduledShipmentLoadBuilder builder =
        LspShipmentUtils.ScheduledShipmentLoadBuilder.newInstance();
    builder.setResourceId(resource.getId());

    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
        builder.setLogisticChainElement(element);
      }
    }

    int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
    Leg legBeforeService = (Leg) tour.getTourElements().get(serviceIndex - 1);
    double startTimeOfLoading =
        legBeforeService.getExpectedDepartureTime() + legBeforeService.getExpectedTransportTime();
    builder.setStartTime(startTimeOfLoading);
    builder.setEndTime(startTimeOfLoading + lspShipment.getDeliveryServiceTime());

    LspShipmentPlanElement load = builder.build();
    String idString =
        load.getResourceId() + "" + load.getLogisticChainElement().getId() + load.getElementType();
    Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
    LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, lspShipment.getId())
        .addPlanElement(id, load);
  }

  private void addShipmentTransportElement(
      LspShipment lspShipment, Tour tour, Tour.ServiceActivity serviceActivity) {

    LspShipmentUtils.ScheduledShipmentTransportBuilder builder =
        LspShipmentUtils.ScheduledShipmentTransportBuilder.newInstance();
    builder.setResourceId(resource.getId());

    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
        builder.setLogisticChainElement(element);
      }
    }

    int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
    Leg legAfterService = (Leg) tour.getTourElements().get(serviceIndex + 1);
    double startTimeOfTransport = legAfterService.getExpectedDepartureTime();
    builder.setStartTime(startTimeOfTransport);
    Leg lastLeg = (Leg) tour.getTourElements().getLast();
    double endTimeOfTransport = lastLeg.getExpectedDepartureTime() + lastLeg.getExpectedTransportTime();
    builder.setEndTime(endTimeOfTransport);
    builder.setCarrierId(carrier.getId());
    builder.setFromLinkId(serviceActivity.getLocation());
    builder.setToLinkId(tour.getEndLinkId());
    builder.setCarrierService(serviceActivity.getService());
    LspShipmentPlanElement transport = builder.build();
    String idString =
        transport.getResourceId()
            + ""
            + transport.getLogisticChainElement().getId()
            + transport.getElementType();
    Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
    LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, lspShipment.getId())
        .addPlanElement(id, transport);
  }

  private void addCollectionServiceEventHandler(
      CarrierService carrierService, LspShipment lspShipment, LSPCarrierResource resource) {

    for (LogisticChainElement element : this.resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
        CollectionServiceEndEventHandler endHandler =
            new CollectionServiceEndEventHandler(
                carrierService, lspShipment, element, resource);
        lspShipment.addSimulationTracker(endHandler);
        break;
      }
    }
  }

  private void addCollectionTourEndEventHandler(
      CarrierService carrierService,
      LspShipment lspShipment,
      LSPCarrierResource resource,
      Tour tour) {
    for (LogisticChainElement element : this.resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
        LSPTourEndEventHandler handler =
            new LSPTourEndEventHandler(
					lspShipment, carrierService, element, resource, tour);
        lspShipment.addSimulationTracker(handler);
        break;
      }
    }
  }

  private void addShipmentUnloadElement(LspShipment lspShipment, Tour tour) {

    LspShipmentUtils.ScheduledShipmentUnloadBuilder builder =
        LspShipmentUtils.ScheduledShipmentUnloadBuilder.newInstance();
    builder.setResourceId(resource.getId());
    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
        builder.setLogisticsChainElement(element);
      }
    }
    Leg lastLeg = (Leg) tour.getTourElements().getLast();
    double startTime = lastLeg.getExpectedDepartureTime() + lastLeg.getExpectedTransportTime();
    builder.setStartTime(startTime);
    builder.setEndTime(startTime + getUnloadEndTime(tour));

    LspShipmentPlanElement unload = builder.build();
    String idString =
        unload.getResourceId()
            + ""
            + unload.getLogisticChainElement().getId()
            + unload.getElementType();
    Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
    LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, lspShipment.getId())
        .addPlanElement(id, unload);
  }

  private double getUnloadEndTime(Tour tour) {
    double unloadEndTime = 0;
    for (TourElement element : tour.getTourElements()) {
      if (element instanceof Tour.ServiceActivity serviceActivity) {
        unloadEndTime = unloadEndTime + serviceActivity.getDuration();
      }
    }
    return unloadEndTime;
  }

  private record LSPCarrierPair(LspShipment lspShipment, CarrierService carrierService) {}
}
