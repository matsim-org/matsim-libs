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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.Tour.Leg;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
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

  Logger log = LogManager.getLogger(DistributionCarrierScheduler.class);

  private Carrier carrier;
  private DistributionCarrierResource resource;
  private int carrierCnt = 1;
  private final Scenario scenario;


  /**
   * Constructor for the DistributionCarrierScheduler.
   * TODO: In the future, the scenario should come via injection(?) This here is only a dirty workaround. KMT'Aug'24
   *
   * @param scenario the scenario
   */
  DistributionCarrierScheduler(Scenario scenario) {
    this.scenario = scenario;
  }

  @Override
  protected void initializeValues(LSPResource resource) {
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
    ArrayList<LspShipment> copyOfAssignedShipments = new ArrayList<>(lspShipmentsToSchedule);
    ArrayList<LspShipment> shipmentsInCurrentTour = new ArrayList<>();
    List<CarrierPlan> scheduledPlans = new LinkedList<>();

    for (LspShipment lspShipment : copyOfAssignedShipments) {
      // TODO KMT: Verstehe es nur mäßig, was er hier mit den Fahrzeugtypen macht. Er nimmt einfach
      // das erste/nächste(?) und schaut ob es da rein passt... Aber was ist, wenn es mehrere
      // gibt???
      VehicleType vehicleType =
              ResourceImplementationUtils.getVehicleTypeCollection(carrier).iterator().next();
      if ((load + lspShipment.getSize())
              > vehicleType.getCapacity().getOther().intValue()) {
        load = 0;
        Carrier auxiliaryCarrier =
                CarrierSchedulerUtils.solveVrpWithJsprit(
                        createAuxiliaryCarrier(shipmentsInCurrentTour, availabilityTimeOfLastShipment + cumulatedLoadingTime),
                        scenario);
        scheduledPlans.add(auxiliaryCarrier.getSelectedPlan());
        carrier.getServices().putAll(auxiliaryCarrier.getServices());
        cumulatedLoadingTime = 0;
        shipmentsInCurrentTour.clear();
      }
      shipmentsInCurrentTour.add(lspShipment);
      load = load + lspShipment.getSize();
      cumulatedLoadingTime = cumulatedLoadingTime + lspShipment.getDeliveryServiceTime();
      availabilityTimeOfLastShipment = LspShipmentUtils.getTimeOfLspShipment(lspShipment);
    }

    if (!shipmentsInCurrentTour.isEmpty()) {
      Carrier auxiliaryCarrier =
              CarrierSchedulerUtils.solveVrpWithJsprit(
                      createAuxiliaryCarrier(
                              shipmentsInCurrentTour, availabilityTimeOfLastShipment + cumulatedLoadingTime),
                      scenario);
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

  private CarrierService convertToCarrierService(LspShipment lspShipment) {
    Id<CarrierService> serviceId = Id.create(lspShipment.getId().toString(), CarrierService.class);
    CarrierService carrierService = CarrierService.Builder.newInstance(serviceId, lspShipment.getTo())
            .setCapacityDemand(lspShipment.getSize())
            .setServiceDuration(lspShipment.getDeliveryServiceTime())
            .build();
    //ensure that the ids of the lspShipment and the carrierService are the same. This is needed for updating the LSPShipmentPlan
    if (! Objects.equals(lspShipment.getId().toString(), carrierService.getId().toString())) {
      log.error("Id of LspShipment: {} and CarrierService: {} do not match", lspShipment.getId().toString(), carrierService.getId().toString(),
              new IllegalStateException("Id of LspShipment and CarrierService do not match"));
    }
    return carrierService;
  }

  @Override
  protected void updateShipments() {
    for (LspShipment lspShipment : lspShipmentsToSchedule) {
      for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
        Tour tour = scheduledTour.getTour();
        for (TourElement element : tour.getTourElements()) {
          if (element instanceof ServiceActivity serviceActivity) {
            if (Objects.equals(lspShipment.getId().toString(), serviceActivity.getService().getId().toString())) {
              addShipmentLoadElement(lspShipment, tour);
              addShipmentTransportElement(lspShipment, tour, serviceActivity);
              addShipmentUnloadElement(lspShipment, tour, serviceActivity);
              addDistributionTourStartEventHandler(serviceActivity.getService(), lspShipment, resource, tour);
              addDistributionServiceEventHandler(serviceActivity.getService(), lspShipment, resource);
            }
          }
        }
      }
    }
  }

  private void addShipmentLoadElement(LspShipment lspShipment, Tour tour) {
    LspShipmentUtils.ScheduledShipmentLoadBuilder builder =
            LspShipmentUtils.ScheduledShipmentLoadBuilder.newInstance();
    builder.setResourceId(resource.getId());

    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
        builder.setLogisticChainElement(element);
      }
    }

    int startIndex = tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
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

    int startIndex = tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
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

    int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
    ServiceActivity serviceAct = (ServiceActivity) tour.getTourElements().get(serviceIndex);

    final double startTime = serviceAct.getExpectedArrival();
    final double endTime = startTime + serviceAct.getDuration();
    Assert.isTrue(
            endTime >= startTime,
            "latest End must be later than earliest start. start: " + startTime + " ; end: " + endTime);

    builder.setStartTime(startTime);
    builder.setEndTime(endTime);

    LspShipmentPlanElement unload = builder.build();
    String idString =
            unload.getResourceId()
                    + String.valueOf(unload.getLogisticChainElement().getId())
                    + unload.getElementType();
    Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
    LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getId())
            .addPlanElement(id, unload);
  }

  private Carrier createAuxiliaryCarrier(ArrayList<LspShipment> shipmentsInCurrentTour, double startTime) {
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

    for (LspShipment lspShipment : shipmentsInCurrentTour) {
      CarrierService carrierService = convertToCarrierService(lspShipment);
      auxiliaryCarrier.getServices().put(carrierService.getId(), carrierService);
    }
    return auxiliaryCarrier;
  }

  private void addDistributionServiceEventHandler(
          CarrierService carrierService,
          LspShipment lspShipment,
          LSPCarrierResource resource) {

    for (LogisticChainElement element : this.resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
        DistributionServiceStartEventHandler handler =
                new DistributionServiceStartEventHandler(carrierService, lspShipment, element, resource);
        lspShipment.addSimulationTracker(handler);
        break;
      }
    }
  }

  private void addDistributionTourStartEventHandler(
          CarrierService carrierService,
          LspShipment lspShipment,
          LSPCarrierResource resource,
          Tour tour) {

    for (LogisticChainElement element : this.resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
        LSPTourStartEventHandler handler =
                new LSPTourStartEventHandler(lspShipment, carrierService, element, resource, tour);
        lspShipment.addSimulationTracker(handler);
        break;
      }
    }
  }

}
