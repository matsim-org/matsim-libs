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

import java.util.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.Tour.Leg;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.VehicleType;

/**
 * In the case of the MainRunResource, the incoming LSPShipments are bundled together until their
 * total weight exceeds the capacity of the deployed vehicle type. Then, this bundle of LSPShipments
 * is converted to a scheduled tour from the freight contrib of MATSim. The start of this tour is
 * located at the first TranshipmentHub and the end at the second one. All LSPShipments are
 * converted to services that take place at the end point of the tour.
 *
 * <p>Tour is routed by MATSim Network Router.
 *
 * <p>* The tour starts after the last shipment * has arrived and the time necessary for loading all
 * shipments into the vehicle * has passed.
 */
/*package-private*/ class MainRunCarrierScheduler extends LSPResourceScheduler {
  
  private Carrier carrier;
  private MainRunCarrierResource resource;
  private ArrayList<LSPShipmentCarrierServicePair> pairs;
  private final Scenario scenario;
  private int tourIdIndex = 1; // Have unique TourIds for the MainRun.

  /*package-private*/ MainRunCarrierScheduler(Scenario scenario) {
    this.pairs = new ArrayList<>();
    this.scenario = scenario;
  }

  @Override
  protected void initializeValues(LSPResource resource) {
    this.pairs = new ArrayList<>();
    if (resource.getClass() == MainRunCarrierResource.class) {
      this.resource = (MainRunCarrierResource) resource;
      this.carrier = this.resource.getCarrier();
      this.carrier.getServices().clear();
      this.carrier.getShipments().clear();
      this.carrier.getPlans().clear();
    }
  }

  @Override
  protected void scheduleResource() {
    int load = 0;
    List<LspShipment> copyOfAssignedShipments = new ArrayList<>(lspShipmentsToSchedule);
    copyOfAssignedShipments.sort(Comparator.comparingDouble(LspShipment::getTime));
    ArrayList<LspShipment> shipmentsInCurrentTour = new ArrayList<>();
    //		ArrayList<ScheduledTour> scheduledTours = new ArrayList<>();
    List<CarrierPlan> scheduledPlans = new LinkedList<>();

    for (LspShipment tuple : copyOfAssignedShipments) {
      // Add job as "services" to the carrier. So the carrier has this available
      CarrierService carrierService = convertToCarrierService(tuple);
      carrier.getServices().put(carrierService.getId(), carrierService);

      VehicleType vehicleType =
          ResourceImplementationUtils.getVehicleTypeCollection(carrier).iterator().next();
      if ((load + tuple.getSize())
          > vehicleType.getCapacity().getOther().intValue()) {
        load = 0;
        CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
        scheduledPlans.add(plan);
        shipmentsInCurrentTour.clear();
      }
      shipmentsInCurrentTour.add(tuple);
      load = load + tuple.getSize();
    }
    if (!shipmentsInCurrentTour.isEmpty()) {
      CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
      scheduledPlans.add(plan);
      shipmentsInCurrentTour.clear();
    }

    List<ScheduledTour> scheduledTours = new LinkedList<>();
    for (CarrierPlan scheduledPlan : scheduledPlans) {
      scheduledTours.addAll(scheduledPlan.getScheduledTours());
    }
    CarrierPlan plan = new CarrierPlan(carrier, scheduledTours);
    plan.setScore(CarrierSchedulerUtils.sumUpScore(scheduledPlans));
    carrier.setSelectedPlan(plan);
  }

  private CarrierPlan createPlan(Carrier carrier, List<LspShipment> tuples) {

    // TODO: Allgemein: Hier ist alles manuell zusammen gesetzt; es findet KEINE Tourenplanung
    // statt!
    NetworkBasedTransportCosts.Builder tpcostsBuilder =
        NetworkBasedTransportCosts.Builder.newInstance(
            scenario.getNetwork(),
            ResourceImplementationUtils.getVehicleTypeCollection(resource.getCarrier()));
    NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
    Collection<ScheduledTour> tours = new ArrayList<>();

    Tour.Builder tourBuilder = Tour.Builder.newInstance(Id.create(tourIdIndex, Tour.class));
    tourIdIndex++;
    tourBuilder.scheduleStart(Id.create(resource.getStartLinkId(), Link.class));

    double totalLoadingTime = 0;
    double latestTupleTime = 0;

    for (LspShipment tuple : tuples) {
      totalLoadingTime = totalLoadingTime + tuple.getDeliveryServiceTime();
      if (tuple.getTime() > latestTupleTime) {
        latestTupleTime = tuple.getTime();
      }
      tourBuilder.addLeg(new Leg());
      CarrierService carrierService = convertToCarrierService(tuple);
      pairs.add(new LSPShipmentCarrierServicePair(tuple, carrierService));
      tourBuilder.scheduleService(carrierService);
    }

    tourBuilder.addLeg(new Leg());
    switch (resource.getVehicleReturn()) {
      case returnToFromLink -> // The more "urban" behaviour: The vehicle returns to its origin
                               // (startLink).
      tourBuilder.scheduleEnd(Id.create(resource.getStartLinkId(), Link.class));
      case endAtToLink -> // The more "long-distance" behaviour: The vehicle ends at its destination
                          // (toLink).
      tourBuilder.scheduleEnd(Id.create(resource.getEndLinkId(), Link.class));
      default -> throw new IllegalStateException(
          "Unexpected value: " + resource.getVehicleReturn());
    }
    Tour vehicleTour = tourBuilder.build();
    CarrierVehicle vehicle =
        carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
    double tourStartTime = latestTupleTime + totalLoadingTime;
    ScheduledTour sTour = ScheduledTour.newInstance(vehicleTour, vehicle, tourStartTime);

    tours.add(sTour);
    CarrierPlan plan = new CarrierPlan(carrier, tours);
    NetworkRouter.routePlan(plan, netbasedTransportcosts);
    plan.setScore(scorePlanManually(plan));
    return plan;
  }

  /**
   * For the main run, there is currently (nov'22) no jsprit planning. The plan is instead
   * constructed manually. As a consequence, there is no score (from jsprit) for this plan
   * available. To avoid issues in later scoring of the LSP, we would like to hava also a score for
   * the MainRunCarrier. This is calculated here manually
   *
   * <p>It bases on the - vehicle's fixed costs - distance dependent costs - (expected) travel time
   * dependent costs NOT included is the calculation of activity times,... But this is currently
   * also missing e.g. in the distributionCarrier, where the VRP setup does not include this :(
   *
   * @param plan The carrierPlan, that should get scored.
   * @return the calculated score
   */
  private double scorePlanManually(CarrierPlan plan) {
    // score plan // Note: Activities are not scored, but they are also NOT scored for the
    // Distribution carrier (as the VRP is currently set up) kmt nov'22
    double score = 0.;
    for (ScheduledTour scheduledTour : plan.getScheduledTours()) {
      // vehicle fixed costs
      score = score + scheduledTour.getVehicle().getType().getCostInformation().getFixedCosts();

      // distance
      double distance = 0.0;
      double time = 0.0;
      for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {
        if (tourElement instanceof Leg leg) {
          // distance
          NetworkRoute route = (NetworkRoute) leg.getRoute();
          for (Id<Link> linkId : route.getLinkIds()) {
            distance = distance + scenario.getNetwork().getLinks().get(linkId).getLength();
          }
          if (route.getEndLinkId()
              != route
                  .getStartLinkId()) { // Do not calculate any distance, if start and endpoint are
                                       // identical
            distance =
                distance + scenario.getNetwork().getLinks().get(route.getEndLinkId()).getLength();
          }

          // travel time (exp.)
          time = time + leg.getExpectedTransportTime();
        }
      }
      score =
          score
              + scheduledTour.getVehicle().getType().getCostInformation().getCostsPerMeter()
                  * distance;
      score =
          score
              + scheduledTour.getVehicle().getType().getCostInformation().getCostsPerSecond()
                  * time;
    }
    return (-score); // negative, because we are looking at "costs" instead of "utility"
  }

  private CarrierService convertToCarrierService(LspShipment tuple) {
    Id<CarrierService> serviceId =
        Id.create(tuple.getId().toString(), CarrierService.class);
    CarrierService.Builder builder =
        CarrierService.Builder.newInstance(serviceId, resource.getEndLinkId());
    builder.setCapacityDemand(tuple.getSize());
    builder.setServiceDuration(tuple.getDeliveryServiceTime());
    return builder.build();
  }

  @Override
  protected void updateShipments() {
    for (LspShipment LspShipment : lspShipmentsToSchedule) {
      for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
        Tour tour = scheduledTour.getTour();
        for (TourElement element : tour.getTourElements()) {
          if (element instanceof Tour.ServiceActivity serviceActivity) {
            LSPShipmentCarrierServicePair carrierPair =
                new LSPShipmentCarrierServicePair(
                    LspShipment, serviceActivity.getService());
            for (LSPShipmentCarrierServicePair pair : pairs) {
              if (pair.tuple == carrierPair.tuple
                  && pair.carrierService.getId() == carrierPair.carrierService.getId()) {
                addShipmentLoadElement(LspShipment, tour);
                addShipmentTransportElement(LspShipment, tour, serviceActivity);
                addShipmentUnloadElement(LspShipment, tour, serviceActivity);
                addMainTourRunStartEventHandler(pair.carrierService, LspShipment, resource, tour);
                addMainRunTourEndEventHandler(pair.carrierService, LspShipment, resource, tour);
              }
            }
          }
        }
      }
    }
  }

  private void addShipmentLoadElement(
      LspShipment tuple, Tour tour) {
    LspShipmentUtils.ScheduledShipmentLoadBuilder builder =
        LspShipmentUtils.ScheduledShipmentLoadBuilder.newInstance();
    builder.setResourceId(resource.getId());
    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        builder.setLogisticChainElement(element);
      }
    }
    int startIndex =
        tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
    Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
    double startTimeOfTransport = legAfterStart.getExpectedDepartureTime();
    double cumulatedLoadingTime = 0;
    for (TourElement element : tour.getTourElements()) {
      if (element instanceof Tour.ServiceActivity activity) {
        cumulatedLoadingTime = cumulatedLoadingTime + activity.getDuration();
      }
    }
    builder.setStartTime(startTimeOfTransport - cumulatedLoadingTime);
    builder.setEndTime(startTimeOfTransport);

    LspShipmentPlanElement load = builder.build();
    String idString =
        load.getResourceId()
            + String.valueOf(load.getLogisticChainElement().getId())
            + load.getElementType();
    Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
    LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getId())
        .addPlanElement(id, load);
  }

  private void addShipmentTransportElement(
      LspShipment tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
    LspShipmentUtils.ScheduledShipmentTransportBuilder builder =
        LspShipmentUtils.ScheduledShipmentTransportBuilder.newInstance();
    builder.setResourceId(resource.getId());
    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        builder.setLogisticChainElement(element);
      }
    }
    int startIndex =
        tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
    Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
    double startTimeOfTransport = legAfterStart.getExpectedDepartureTime();
    builder.setStartTime(startTimeOfTransport);
    builder.setEndTime(legAfterStart.getExpectedTransportTime() + startTimeOfTransport);
    builder.setCarrierId(carrier.getId());
    builder.setFromLinkId(tour.getStartLinkId());
    builder.setToLinkId(tour.getEndLinkId());
    builder.setCarrierService(serviceActivity.getService());
    LspShipmentPlanElement transport = builder.build();
    String idString =
        transport.getResourceId()
            + String.valueOf(transport.getLogisticChainElement().getId())
            + transport.getElementType();
    Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
    LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getId())
        .addPlanElement(id, transport);
  }

  private void addShipmentUnloadElement(
      LspShipment tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
    LspShipmentUtils.ScheduledShipmentUnloadBuilder builder =
        LspShipmentUtils.ScheduledShipmentUnloadBuilder.newInstance();
    builder.setResourceId(resource.getId());
    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        builder.setLogisticsChainElement(element);
      }
    }
    double cumulatedLoadingTime = 0;
    for (TourElement element : tour.getTourElements()) {
      if (element instanceof Tour.ServiceActivity activity) {
        cumulatedLoadingTime = cumulatedLoadingTime + activity.getDuration();
      }
    }
    int startIndex =
        tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
    Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
    builder.setStartTime(
        legAfterStart.getExpectedDepartureTime() + legAfterStart.getExpectedTransportTime());
    builder.setEndTime(
        legAfterStart.getExpectedDepartureTime()
            + legAfterStart.getExpectedTransportTime()
            + cumulatedLoadingTime);

    LspShipmentPlanElement unload = builder.build();
    String idString =
        unload.getResourceId()
            + String.valueOf(unload.getLogisticChainElement().getId())
            + unload.getElementType();
    Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
    LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getId())
        .addPlanElement(id, unload);
  }

  private void addMainTourRunStartEventHandler(
      CarrierService carrierService,
      LspShipment tuple,
      LSPCarrierResource resource,
      Tour tour) {
    for (LogisticChainElement element : this.resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        LSPTourStartEventHandler handler =
            new LSPTourStartEventHandler(tuple, carrierService, element, resource, tour);
        tuple.addSimulationTracker(handler);
        break;
      }
    }
  }

  private void addMainRunTourEndEventHandler(
      CarrierService carrierService,
      LspShipment tuple,
      LSPCarrierResource resource,
      Tour tour) {
    for (LogisticChainElement element : this.resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        LSPTourEndEventHandler handler =
            new LSPTourEndEventHandler(tuple, carrierService, element, resource, tour);
        tuple.addSimulationTracker(handler);
        break;
      }
    }
  }

  private record LSPShipmentCarrierServicePair(LspShipment tuple, CarrierService carrierService) {}
}
