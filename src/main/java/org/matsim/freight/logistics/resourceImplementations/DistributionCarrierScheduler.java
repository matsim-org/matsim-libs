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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.Tour.Leg;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.shipment.ShipmentPlanElement;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.matsim.vehicles.VehicleType;

/**
 * Ähnlich zu CollectionCarrierScheduler: Nun werden Sendungen verteilt statt eingesammelt.
 *
 * <p>BUT: scheduleResource() is different from the one used in the case of collection. The
 * LSPShipments are not simply handed over to jsprit which calculates the vehicle tours, but rather
 * loaded into a waiting distribution vehicle in the order of their arrival at the depot. Once this
 * vehicle is full, the tour for this single one is planned by jsprit. All vehicles are thus filled
 * and scheduled consecutively.
 */
/*package-private*/ class DistributionCarrierScheduler extends LSPResourceScheduler {

  private Carrier carrier;
  private DistributionCarrierResource resource;
  private ArrayList<LSPCarrierPair> pairs;
  private int carrierCnt = 1;
  private RoadPricingScheme rpscheme = null;

  DistributionCarrierScheduler() {
    this.pairs = new ArrayList<>();
  }

  /**
   * Constructor for the DistributionCarrierScheduler.
   * TODO: In the future, the road pricing scheme should come from some the scenario: RoadPricingUtils.getRoadPricingScheme(scenario). This here is only a dirty workaround. KMT'Aug'24
   * @deprecated This is only a dirty workaround. KMT'Aug'24
   * @param rpscheme the road pricing scheme
   */
  @Deprecated
  DistributionCarrierScheduler(RoadPricingScheme rpscheme) {
    this.pairs = new ArrayList<>();
    this.rpscheme = rpscheme;
  }

  @Override
  protected void initializeValues(LSPResource resource) {
    this.pairs = new ArrayList<>();
    if (resource.getClass() == DistributionCarrierResource.class) {
      this.resource = (DistributionCarrierResource) resource;
      this.carrier = this.resource.getCarrier();
      this.carrier.getServices().clear();
      this.carrier.getShipments().clear();
      this.carrier.getPlans().clear();
    }
  }

  @Override
  protected void scheduleResource() {
    int load = 0;
    double cumulatedLoadingTime = 0;
    double availabilityTimeOfLastShipment = 0;
    ArrayList<LspShipmentWithTime> copyOfAssignedShipments = new ArrayList<>(lspShipmentsWithTime);
    ArrayList<LspShipmentWithTime> shipmentsInCurrentTour = new ArrayList<>();
    List<CarrierPlan> scheduledPlans = new LinkedList<>();

    for (LspShipmentWithTime tuple : copyOfAssignedShipments) {
      // TODO KMT: Verstehe es nur mäßig, was er hier mit den Fahrzeugtypen macht. Er nimmt einfach
      // das erste/nächste(?) und schaut ob es da rein passt... Aber was ist, wenn es mehrere
      // gibt???
      VehicleType vehicleType =
          ResourceImplementationUtils.getVehicleTypeCollection(carrier).iterator().next();
      if ((load + tuple.getLspShipment().getSize())
          > vehicleType.getCapacity().getOther().intValue()) {
        load = 0;
        Carrier auxiliaryCarrier =
            CarrierSchedulerUtils.solveVrpWithJsprit(
                createAuxiliaryCarrier(
                    shipmentsInCurrentTour, availabilityTimeOfLastShipment + cumulatedLoadingTime),
                resource.getNetwork(), rpscheme);
        scheduledPlans.add(auxiliaryCarrier.getSelectedPlan());
        carrier.getServices().putAll(auxiliaryCarrier.getServices());
        cumulatedLoadingTime = 0;
        shipmentsInCurrentTour.clear();
      }
      shipmentsInCurrentTour.add(tuple);
      load = load + tuple.getLspShipment().getSize();
      cumulatedLoadingTime = cumulatedLoadingTime + tuple.getLspShipment().getDeliveryServiceTime();
      availabilityTimeOfLastShipment = tuple.getTime();
    }

    if (!shipmentsInCurrentTour.isEmpty()) {
      Carrier auxiliaryCarrier =
          CarrierSchedulerUtils.solveVrpWithJsprit(
              createAuxiliaryCarrier(
                  shipmentsInCurrentTour, availabilityTimeOfLastShipment + cumulatedLoadingTime),
              resource.getNetwork(), rpscheme);
      scheduledPlans.add(auxiliaryCarrier.getSelectedPlan());
      carrier.getServices().putAll(auxiliaryCarrier.getServices());
      shipmentsInCurrentTour.clear();
    }

    CarrierPlan plan = new CarrierPlan(carrier, unifyTourIds(scheduledPlans));
    plan.setScore(CarrierSchedulerUtils.sumUpScore(scheduledPlans));
    plan.setJspritScore(CarrierSchedulerUtils.sumUpJspritScore(scheduledPlans));
    carrier.setSelectedPlan(plan);
  }

  /**
   * This method unifies the tourIds of the CollectionCarrier.
   *
   * <p>It is done because in the current setup, there is one (auxiliary) Carrier per Tour. ---> in
   * each Carrier the Tour has the id 1. In a second step all of that tours were put together in one
   * single carrier {@link #scheduleResource()} But now, this carrier can have several tours, all
   * with the same id (1).
   *
   * <p>In this method all tours copied but with a new (unique) TourId.
   *
   * <p>This is a workaround. In my (KMT, sep'22) opinion it would be better to switch so {@link
   * CarrierShipment}s instead of {@link CarrierService} and use only on DistributionCarrier with
   * only one VRP and only one jsprit-Run. This would avoid this workaround and also improve the
   * solution, because than the DistributionCarrier can decide on it one which shipments will go
   * into which tours
   *
   * @param carrierPlans Collection of CarrierPlans
   * @return Collection<ScheduledTour> the scheduledTours with unified tour Ids.
   */
  //	private Collection<ScheduledTour> unifyTourIds(Collection<ScheduledTour> scheduledTours) {
  private Collection<ScheduledTour> unifyTourIds(Collection<CarrierPlan> carrierPlans) {
    int tourIdIndex = 1;
    List<ScheduledTour> scheduledToursUnified = new LinkedList<>();

    for (CarrierPlan carrierPlan : carrierPlans) {
      for (ScheduledTour scheduledTour : carrierPlan.getScheduledTours()) {
        var newTour =
            scheduledTour
                .getTour()
                .duplicateWithNewId(Id.create("dist_" + tourIdIndex, Tour.class));
        tourIdIndex++;
        var newScheduledTour =
            ScheduledTour.newInstance(
                newTour, scheduledTour.getVehicle(), scheduledTour.getDeparture());
        scheduledToursUnified.add(newScheduledTour);
      }
    }
    return scheduledToursUnified;
  }

  private CarrierService convertToCarrierService(LspShipmentWithTime tuple) {
    Id<CarrierService> serviceId =
        Id.create(tuple.getLspShipment().getId().toString(), CarrierService.class);
    CarrierService.Builder builder =
        CarrierService.Builder.newInstance(serviceId, tuple.getLspShipment().getTo());
    builder.setCapacityDemand(tuple.getLspShipment().getSize());
    builder.setServiceDuration(tuple.getLspShipment().getDeliveryServiceTime());
    CarrierService carrierService = builder.build();
    pairs.add(new LSPCarrierPair(tuple, carrierService));
    return carrierService;
  }

  @Override
  protected void updateShipments() {
    for (LspShipmentWithTime tuple : lspShipmentsWithTime) {
      for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
        Tour tour = scheduledTour.getTour();
        for (TourElement element : tour.getTourElements()) {
          if (element instanceof ServiceActivity serviceActivity) {
            LSPCarrierPair carrierPair = new LSPCarrierPair(tuple, serviceActivity.getService());
            for (LSPCarrierPair pair : pairs) {
              if (pair.tuple == carrierPair.tuple
                  && pair.carrierService.getId() == carrierPair.carrierService.getId()) {
                addShipmentLoadElement(tuple, tour, serviceActivity);
                addShipmentTransportElement(tuple, tour, serviceActivity);
                addShipmentUnloadElement(tuple, tour, serviceActivity);
                addDistributionTourStartEventHandler(pair.carrierService, tuple, resource, tour);
                addDistributionServiceEventHandler(pair.carrierService, tuple, resource);
              }
            }
          }
        }
      }
    }
  }

  private void addShipmentLoadElement(
      LspShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
    ShipmentUtils.ScheduledShipmentLoadBuilder builder =
        ShipmentUtils.ScheduledShipmentLoadBuilder.newInstance();
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
      if (element instanceof ServiceActivity activity) {
        cumulatedLoadingTime = cumulatedLoadingTime + activity.getDuration();
      }
    }
    builder.setStartTime(startTimeOfTransport - cumulatedLoadingTime);
    builder.setEndTime(startTimeOfTransport);

    ShipmentPlanElement load = builder.build();
    String idString =
        load.getResourceId() + "" + load.getLogisticChainElement().getId() + load.getElementType();
    Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
    ShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getLspShipment().getId())
        .addPlanElement(id, load);
  }

  private void addShipmentTransportElement(
      LspShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
    ShipmentUtils.ScheduledShipmentTransportBuilder builder =
        ShipmentUtils.ScheduledShipmentTransportBuilder.newInstance();
    builder.setResourceId(resource.getId());
    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        builder.setLogisticChainElement(element);
      }
    }
    int startIndex =
        tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
    final Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
    final int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
    final Leg legBeforeService = (Leg) tour.getTourElements().get(serviceIndex - 1);
    final double startTimeOfTransport = legAfterStart.getExpectedDepartureTime();
    final double endTimeOfTransport =
        legBeforeService.getExpectedTransportTime() + legBeforeService.getExpectedDepartureTime();
    Assert.isTrue(
        endTimeOfTransport >= startTimeOfTransport,
        "latest End must be later than earliest start. start: "
            + startTimeOfTransport
            + " ; end: "
            + endTimeOfTransport);

    builder.setStartTime(startTimeOfTransport);
    builder.setEndTime(endTimeOfTransport);
    builder.setCarrierId(carrier.getId());
    builder.setFromLinkId(tour.getStartLinkId());
    builder.setToLinkId(serviceActivity.getLocation());
    builder.setCarrierService(serviceActivity.getService());
    ShipmentPlanElement transport = builder.build();
    String idString =
        transport.getResourceId()
            + ""
            + transport.getLogisticChainElement().getId()
            + transport.getElementType();
    Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
    ShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getLspShipment().getId())
        .addPlanElement(id, transport);
  }

  private void addShipmentUnloadElement(
      LspShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
    ShipmentUtils.ScheduledShipmentUnloadBuilder builder =
        ShipmentUtils.ScheduledShipmentUnloadBuilder.newInstance();
    builder.setResourceId(resource.getId());
    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        builder.setLogisticsChainElement(element);
      }
    }
    int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
    ServiceActivity serviceAct = (ServiceActivity) tour.getTourElements().get(serviceIndex);

    final double startTime = serviceAct.getExpectedArrival();
    final double endTime = startTime + serviceAct.getDuration();
    Assert.isTrue(
        endTime >= startTime,
        "latest End must be later than earliest start. start: " + startTime + " ; end: " + endTime);

    builder.setStartTime(startTime);
    builder.setEndTime(endTime);

    ShipmentPlanElement unload = builder.build();
    String idString =
        unload.getResourceId()
            + String.valueOf(unload.getLogisticChainElement().getId())
            + unload.getElementType();
    Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
    ShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getLspShipment().getId())
        .addPlanElement(id, unload);
  }

  private Carrier createAuxiliaryCarrier(
      ArrayList<LspShipmentWithTime> shipmentsInCurrentTour, double startTime) {
    final Id<Carrier> carrierId = Id.create(carrier.getId().toString() + carrierCnt, Carrier.class);
    carrierCnt++;
    Carrier auxiliaryCarrier = CarriersUtils.createCarrier(carrierId);
    CarrierVehicle carrierVehicle =
        carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
    final VehicleType vehicleType = carrierVehicle.getType();

    CarrierVehicle.Builder vBuilder =
        CarrierVehicle.Builder.newInstance(
            carrierVehicle.getId(), carrierVehicle.getLinkId(), vehicleType);
    vBuilder.setEarliestStart(startTime);
    vBuilder.setLatestEnd(24 * 60 * 60);
    CarrierVehicle cv = vBuilder.build();
    auxiliaryCarrier.getCarrierCapabilities().getCarrierVehicles().put(cv.getId(), cv);
    auxiliaryCarrier.getCarrierCapabilities().setFleetSize(FleetSize.FINITE);

    for (LspShipmentWithTime tuple : shipmentsInCurrentTour) {
      CarrierService carrierService = convertToCarrierService(tuple);
      auxiliaryCarrier.getServices().put(carrierService.getId(), carrierService);
    }
    return auxiliaryCarrier;
  }

  private void addDistributionServiceEventHandler(
      CarrierService carrierService, LspShipmentWithTime tuple, LSPCarrierResource resource) {
    for (LogisticChainElement element : this.resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        DistributionServiceStartEventHandler handler =
            new DistributionServiceStartEventHandler(
                carrierService, tuple.getLspShipment(), element, resource);
        tuple.getLspShipment().addSimulationTracker(handler);
        break;
      }
    }
  }

  private void addDistributionTourStartEventHandler(
      CarrierService carrierService,
      LspShipmentWithTime tuple,
      LSPCarrierResource resource,
      Tour tour) {
    for (LogisticChainElement element : this.resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        LSPTourStartEventHandler handler =
            new LSPTourStartEventHandler(
                tuple.getLspShipment(), carrierService, element, resource, tour);
        tuple.getLspShipment().addSimulationTracker(handler);
        break;
      }
    }
  }

  private record LSPCarrierPair(LspShipmentWithTime tuple, CarrierService carrierService) {}
}
