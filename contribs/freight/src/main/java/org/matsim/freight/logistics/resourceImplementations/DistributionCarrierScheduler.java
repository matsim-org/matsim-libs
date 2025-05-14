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
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.Tour.Leg;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.VehicleType;

/**
 * Similar to the CollectionCarrierScheduler: But here the jobs are delivery instead of collected.
 *
 * <p>BUT: scheduleResource() is different from the one used in the case of collection. The
 * LSPShipments are not simply handed over to jsprit which calculates the vehicle tours, but rather
 * loaded into a waiting distribution vehicle in the order of their arrival at the depot. Once this
 * vehicle is full, the tour for this single one is planned by jsprit. All vehicles are thus filled
 * and scheduled consecutively. (Service-based approach by Tilmann Matteis)
 * <p>
 * There is now also the Shipment-based approach: Add all jobs and run one VRP.   (KMT)
 */
/*package-private*/ class DistributionCarrierScheduler extends LSPResourceScheduler {

	private static final Logger log = LogManager.getLogger(DistributionCarrierScheduler.class);

	private Carrier carrier;
	private DistributionCarrierResource resource;
	private int carrierCnt = 1;
	private final Scenario scenario;


	/**
	 * Constructor for the DistributionCarrierScheduler.
	 * TODO: In the future, the scenario should come via injection(?) This here is only a (dirty) workaround. KMT'Aug'24
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
		ArrayList<LspShipment> copyOfShipmentsToSchedule = new ArrayList<>(lspShipmentsToSchedule);
		switch (CarrierSchedulerUtils.getVrpLogic(carrier)) {
			case serviceBased -> scheduleCarrierBasedOnServices(copyOfShipmentsToSchedule);
			case shipmentBased -> scheduleCarrierBasedOnShipments(copyOfShipmentsToSchedule);
			default -> throw new IllegalStateException("Unexpected value: " + CarrierSchedulerUtils.getVrpLogic(carrier));
		}
		updateShipments();
	}


	@Override
	protected void updateShipments() {
		for (LspShipment lspShipment : lspShipmentsToSchedule) {
			for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
				Tour tour = scheduledTour.getTour();

				switch (CarrierSchedulerUtils.getVrpLogic(carrier)) {
					case serviceBased -> {
						for (TourElement element : tour.getTourElements()) {
							if (element instanceof Tour.ServiceActivity serviceActivity) {
								if (Objects.equals(lspShipment.getId().toString(), serviceActivity.getService().getId().toString())) {
									addShipmentLoadElement(lspShipment, tour);
									addShipmentTransportElement(lspShipment, tour, serviceActivity);
									addShipmentUnloadElement(lspShipment, serviceActivity);
									addDistributionEventHandlers(serviceActivity, lspShipment, resource, tour);
								}
							}
						}
					}
					case shipmentBased -> {
						log.warn("Not sure, if everything of the shipment<load|transport|Unload> element is implemented properly at the moment for the shipment-based VRP.!");
						//TODO needs to get fixed. KMT'Aug'24
						for (TourElement element : tour.getTourElements()) {
							if (element instanceof Tour.Delivery deliveryActivity) {
								if (Objects.equals(lspShipment.getId().toString(), deliveryActivity.getShipment().getId().toString())) {
									//Todo: Nachdenken, was hier nun wie eingetragen werden muss. Nutzung Pickup und Delivery?
									// Und geht das dann nicht kürzer einfacher als dieses ganze selbst zurückrechnen aus den Services und den lspShipments??
									addShipmentLoadElement(lspShipment, tour);
									addShipmentTransportElement(lspShipment, tour, deliveryActivity);
									addShipmentUnloadElement(lspShipment, deliveryActivity);
									addDistributionEventHandlers(deliveryActivity, lspShipment, resource, tour);
								}
							}
						}
					}
					default -> throw new IllegalStateException("Unexpected value: " + CarrierSchedulerUtils.getVrpLogic(carrier));
				}
			}
		}
	}

	/**
	 * This method builds the tours based on the {@link CarrierService}s.
	 * So everytime a vehicle is full, it gets planned by jsprit - based on services.
	 * In the end all of these tours are just added unified, and added to the carrier
	 * <p>
	 * It is the design by Tilmann Matteis.
	 * @param shipmentsToSchedule List of all shipments that needs to get schedules.s
	 */
	private void scheduleCarrierBasedOnServices(ArrayList<LspShipment> shipmentsToSchedule) {
		int load = 0;
		double cumulatedLoadingTime = 0;
		double availabilityTimeOfLastShipment = 0;
		ArrayList<LspShipment> shipmentsInCurrentTour = new ArrayList<>();

		List<CarrierPlan> scheduledPlans = new LinkedList<>();
		for (LspShipment lspShipment : shipmentsToSchedule) {
			// TODO KMT: Verstehe es nur mäßig, was er hier mit den Fahrzeugtypen macht. Er nimmt einfach
			// das erste/nächste(?) und schaut ob es da rein passt... Aber was ist, wenn es mehrere
			// gibt???
			VehicleType vehicleType = ResourceImplementationUtils.getVehicleTypeCollection(carrier).iterator().next();
			if ((load + lspShipment.getSize()) > vehicleType.getCapacity().getOther().intValue()) {
				load = 0;
				Carrier auxiliaryCarrier = CarrierSchedulerUtils.solveVrpWithJsprit(
						createAuxiliaryCarrierServiceBased(shipmentsInCurrentTour, availabilityTimeOfLastShipment + cumulatedLoadingTime), scenario);
				scheduledPlans.add(auxiliaryCarrier.getSelectedPlan());
				carrier.getServices().putAll(auxiliaryCarrier.getServices());
				cumulatedLoadingTime = 0;
				shipmentsInCurrentTour.clear();
			}
			shipmentsInCurrentTour.add(lspShipment);
			load += lspShipment.getSize();
			cumulatedLoadingTime += lspShipment.getDeliveryServiceTime();
			availabilityTimeOfLastShipment = LspShipmentUtils.getTimeOfLspShipment(lspShipment);
		}
		//Restliche Sendungen in einem letzten Vrp planen
		if (!shipmentsInCurrentTour.isEmpty()) {
			Carrier auxiliaryCarrier = CarrierSchedulerUtils.solveVrpWithJsprit(
					createAuxiliaryCarrierServiceBased(shipmentsInCurrentTour, availabilityTimeOfLastShipment + cumulatedLoadingTime), scenario);
			scheduledPlans.add(auxiliaryCarrier.getSelectedPlan());
			carrier.getServices().putAll(auxiliaryCarrier.getServices());
			shipmentsInCurrentTour.clear();
		}

		List<ScheduledTour> scheduledToursUnified = new LinkedList<>();
		/*This method unifies the tourIds of the CollectionCarrier.
		 *
		 * <p>It is done because in the current setup, there is one (auxiliary) Carrier per Tour. ---> in
		 * each Carrier the Tour has the id 1. In a second step all of that tours were put together in one
		 * single carrier {@link #scheduleResource()} But now, this carrier can have several tours, all
		 * with the same id (1).
		 *
		 * <p>In this method all tours copied but with a new (unique) TourId.
		 */
		{
			int tourIdIndex = 1;
			for (CarrierPlan carrierPlan : scheduledPlans) {
				for (ScheduledTour scheduledTour : carrierPlan.getScheduledTours()) {
					Tour newTour = scheduledTour.getTour().duplicateWithNewId(Id.create("dist_" + tourIdIndex, Tour.class));
					tourIdIndex++;
					ScheduledTour newScheduledTour = ScheduledTour.newInstance(newTour, scheduledTour.getVehicle(), scheduledTour.getDeparture());
					scheduledToursUnified.add(newScheduledTour);
				}
			}
		}

		CarrierPlan plan = new CarrierPlan(scheduledToursUnified);
		plan.setScore(CarrierSchedulerUtils.sumUpScore(scheduledPlans));
		plan.setJspritScore(CarrierSchedulerUtils.sumUpJspritScore(scheduledPlans));
		carrier.addPlan(plan);
		carrier.setSelectedPlan(plan);
	}

	/**
	 * This method builds the tours based on the {@link CarrierShipment}s.
	 * This is the new (KMT) approach: Add all jobs and run one VRP.
	 * <p>
	 * @param shipmentsToSchedule List of all shipments that needs to get schedules.s
	 */
	private void scheduleCarrierBasedOnShipments(ArrayList<LspShipment> shipmentsToSchedule) {
		//Todo: implement it.
//		throw new NotImplementedException("This method is not implemented yet. KMT feb'25");

		Carrier auxCarrier = CarriersUtils.createCarrier(Id.create("auxCarrier", Carrier.class));
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance();
		ccBuilder.setFleetSize(FleetSize.INFINITE);

		//copy all vehicles from the original carrier to the auxiliary carrier.
		var startTime = 0.0; //Todo: set to something meaningful, e.g. arrivalTime of the first shipment.
		for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
			var cv = CarrierVehicle.Builder.newInstance(carrierVehicle.getId(), carrierVehicle.getLinkId(), carrierVehicle.getType())
				.setEarliestStart(startTime)
				.setLatestEnd(24 * 60 * 60)
				.build();
			ccBuilder.addVehicle(cv);
		}

		auxCarrier.setCarrierCapabilities(ccBuilder.build());

		//add all shipments.
		for (LspShipment lspShipment : shipmentsToSchedule) {
			CarrierShipment carrierShipment = convertToCarrierShipment(lspShipment);
			auxCarrier.getShipments().put(carrierShipment.getId(), carrierShipment);
			CarriersUtils.addShipment(auxCarrier, carrierShipment);
		}

		CarrierSchedulerUtils.solveVrpWithJsprit(auxCarrier, scenario);

		//Copy infos over to the Carrier.

		CarrierPlan plan = new CarrierPlan(auxCarrier.getSelectedPlan().getScheduledTours());
		plan.setScore(auxCarrier.getSelectedPlan().getScore());
   		plan.setJspritScore(auxCarrier.getSelectedPlan().getJspritScore());
		carrier.getShipments().putAll(auxCarrier.getShipments());
		carrier.addPlan(plan);
		carrier.setSelectedPlan(plan);
	}


	private CarrierService convertToCarrierService(LspShipment lspShipment) {
		Id<CarrierService> serviceId = Id.create(lspShipment.getId().toString(), CarrierService.class);
		CarrierService carrierService = CarrierService.Builder.newInstance(serviceId, lspShipment.getTo(), lspShipment.getSize())
			//TODO TimeWindows are not set. This seems to be a problem. KMT'Aug'24
			//If added here, we also need to decide what happens, if the vehicles StartTime (plus TT) is > TimeWindowEnd ....
			.setServiceDuration(lspShipment.getDeliveryServiceTime())
			.build();
		//ensure that the ids of the lspShipment and the carrierService are the same. This is needed for updating the LSPShipmentPlan
		if (! Objects.equals(lspShipment.getId().toString(), carrierService.getId().toString())) {
			log.error("Id of LspShipment: {} and CarrierService: {} do not match", lspShipment.getId().toString(), carrierService.getId().toString(),
				new IllegalStateException("Id of LspShipment and CarrierService do not match"));
		}
		return carrierService;
	}

	/**
	 * This method converts a LspShipment to a CarrierShipment.
	 * Please note: This method may get removed in the future, in case that the LSPShipment and the CarrierShipment are merged. KMT'Aug'24

	 * @param lspShipment the LspShipment to convert
	 * @return a CarrierShipment
	 */
	private CarrierShipment convertToCarrierShipment(LspShipment lspShipment) {
		Id<CarrierShipment> serviceId = Id.create(lspShipment.getId().toString(), CarrierShipment.class);
		Id<Link> fromLinkId = lspShipment.getFrom(); //Todo: needs to be the hub, if there is one. KMT mar'25
		CarrierShipment carrierShipment = CarrierShipment.Builder.newInstance(serviceId, fromLinkId, lspShipment.getTo(), lspShipment.getSize())
			//TODO TimeWindows are not set. This seems to be a problem. KMT'Aug'24
			//If added here, we also need to decide what happens, if the vehicles StartTime (plus TT) is > TimeWindowEnd ....
			.setDeliveryDuration(lspShipment.getDeliveryServiceTime())
			.build();
		//ensure that the ids of the lspShipment and the carrierShipment are the same. This is needed for updating the LSPShipmentPlan
		if (! Objects.equals(lspShipment.getId().toString(), carrierShipment.getId().toString())) {
			log.error("Id of LspShipment: {} and CarrierShipment: {} do not match", lspShipment.getId().toString(), carrierShipment.getId().toString(),
				new IllegalStateException("Id of LspShipment and CarrierShipment do not match"));
		}
		return carrierShipment;
	}


	private void addShipmentLoadElement(LspShipment lspShipment, Tour tour) {
		LspShipmentUtils.ScheduledShipmentLoadBuilder builder = LspShipmentUtils.ScheduledShipmentLoadBuilder.newInstance();
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
			if (element instanceof Tour.ServiceActivity activity) {
				cumulatedLoadingTime = cumulatedLoadingTime + activity.getDuration();
			}
		}
		builder.setStartTime(startTimeOfTransport - cumulatedLoadingTime);
		builder.setEndTime(startTimeOfTransport);

		LspShipmentPlanElement load = builder.build();
		String idString = load.getResourceId() + "" + load.getLogisticChainElement().getId() + load.getElementType();
		Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
		LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, lspShipment.getId()).addPlanElement(id, load);
	}

	private void addShipmentTransportElement(LspShipment lspShipment, Tour tour, Tour.TourActivity tourActivity) {

		LspShipmentUtils.ScheduledShipmentTransportBuilder builder = LspShipmentUtils.ScheduledShipmentTransportBuilder.newInstance();
		builder.setResourceId(resource.getId());

		for (LogisticChainElement element : resource.getClientElements()) {
			if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
				builder.setLogisticChainElement(element);
			}
		}

		int startIndex = tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
		final Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
		final int serviceIndex = tour.getTourElements().indexOf(tourActivity);
		final Leg legBeforeService = (Leg) tour.getTourElements().get(serviceIndex - 1);
		final double startTimeOfTransport = legAfterStart.getExpectedDepartureTime();
		final double endTimeOfTransport = legBeforeService.getExpectedTransportTime() + legBeforeService.getExpectedDepartureTime();
		Assert.isTrue(endTimeOfTransport >= startTimeOfTransport,
			"latest End must be later than earliest start. start: " + startTimeOfTransport + " ; end: " + endTimeOfTransport);

		builder.setStartTime(startTimeOfTransport);
		builder.setEndTime(endTimeOfTransport);
		builder.setCarrierId(carrier.getId());
		builder.setFromLinkId(tour.getStartLinkId());
		builder.setToLinkId(tourActivity.getLocation());
		switch( tourActivity ){
			case Tour.ServiceActivity serviceActivity -> builder.setCarrierService( serviceActivity.getService() );
			case Tour.ShipmentBasedActivity shipment -> builder.setCarrierShipment( shipment.getShipment() );
			default ->  throw new IllegalStateException("Unexpected value: " + tourActivity);
			// yyyy: At the jsprit level, it makes sense to have these different since services run about 10x faster than shipments.  However,
			// at the matsim level we could consider to either only have shipments (from depot to xx for what used to be services), or only have
			// services. See also MATSim issue #3510  kai/kai, oct'24
		}
		LspShipmentPlanElement transport = builder.build();
		String idString = transport.getResourceId() + "" + transport.getLogisticChainElement().getId() + transport.getElementType();
		Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
		LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, lspShipment.getId()).addPlanElement(id, transport);
	}

	private void addShipmentUnloadElement(LspShipment tuple, Tour.TourActivity tourActivity) {
		LspShipmentUtils.ScheduledShipmentUnloadBuilder builder = LspShipmentUtils.ScheduledShipmentUnloadBuilder.newInstance();
		builder.setResourceId(resource.getId());

		for (LogisticChainElement element : resource.getClientElements()) {
			if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
				builder.setLogisticsChainElement(element);
			}
		}

		final double startTime = tourActivity.getExpectedArrival();
		final double endTime = startTime + tourActivity.getDuration();
		//Todo: Check if it also works with shipmentBased activity, or if we in that case need the way with the switch-case and the data from the shipmentBasedActivity. KMT Oct'24

//    switch( tourActivity ){
//      case Tour.ServiceActivity serviceActivity -> {
//        startTime = tourActivity.getExpectedArrival();
//        endTime = startTime + tourActivity.getDuration();
//
////        startTime = serviceActivity.getExpectedArrival(); //Why is there also a arrivalTime in the Tour.ServiceActivity? Why do not take the date in TourActivity.getExpectedArrivalTime()? KMT Oct'24
////        endTime = startTime + serviceActivity.getDuration();
//      }
//      case Tour.ShipmentBasedActivity shipmentBasedActivity -> {
//        //Todo: Not tested ; maybe we need to take the data from the shipment itself (as is was originally done with the service: serviceActivity.getService() ,..... KMT Oct'24
//        startTime = shipmentBasedActivity.getExpectedArrival(); //Why is there also a arrivalTime in the Tour.ServiceActivity? Why do not take the date in TourActivity.getExpectedArrivalTime()? KMT Oct'24
//        endTime = startTime + shipmentBasedActivity.getDuration();
//
//      }
//      default -> {}
//    }

		Assert.isTrue(endTime >= startTime, "latest End must be later than earliest start. start: " + startTime + " ; end: " + endTime);

		builder.setStartTime(startTime);
		builder.setEndTime(endTime);

		LspShipmentPlanElement unload = builder.build();
		String idString = unload.getResourceId() + String.valueOf(unload.getLogisticChainElement().getId()) + unload.getElementType();
		Id<LspShipmentPlanElement> id = Id.create(idString, LspShipmentPlanElement.class);
		LspShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getId()).addPlanElement(id, unload);
	}

	private Carrier createAuxiliaryCarrierServiceBased(ArrayList<LspShipment> shipmentsInCurrentTour, double startTime) {
		final Id<Carrier> carrierId = Id.create(carrier.getId().toString() + carrierCnt, Carrier.class);
		carrierCnt++;

		CarrierVehicle carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
		CarrierVehicle cv = CarrierVehicle.Builder.newInstance(carrierVehicle.getId(), carrierVehicle.getLinkId(), carrierVehicle.getType())
			.setEarliestStart(startTime)
			.setLatestEnd(24 * 60 * 60)
			.build();
		Carrier auxiliaryCarrier = CarriersUtils.createCarrier(carrierId);
		auxiliaryCarrier.getCarrierCapabilities().getCarrierVehicles().put(cv.getId(), cv);
		auxiliaryCarrier.getCarrierCapabilities().setFleetSize(FleetSize.FINITE);

		for (LspShipment lspShipment : shipmentsInCurrentTour) {
			CarrierService carrierService = convertToCarrierService(lspShipment);
			auxiliaryCarrier.getServices().put(carrierService.getId(), carrierService);
		}

		return auxiliaryCarrier;
	}

	private void addDistributionEventHandlers(Tour.TourActivity tourActivity, LspShipment lspShipment, LSPCarrierResource resource, Tour tour) {
	    for (LogisticChainElement element : this.resource.getClientElements()) {
	        if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
	            DistributionServiceStartEventHandler serviceHandler;
	            LSPTourStartEventHandler tourHandler;
	            switch (tourActivity) {
	                case Tour.ServiceActivity serviceActivity -> {
	                    serviceHandler = new DistributionServiceStartEventHandler(serviceActivity.getService(), lspShipment, element, resource);
	                    tourHandler = new LSPTourStartEventHandler(lspShipment, serviceActivity.getService(), element, resource, tour);
	                }
	                case Tour.ShipmentBasedActivity shipmentBasedActivity -> {
	                    serviceHandler = new DistributionServiceStartEventHandler(shipmentBasedActivity.getShipment(), lspShipment, element, resource);
	                    tourHandler = new LSPTourStartEventHandler(lspShipment, shipmentBasedActivity.getShipment(), element, resource, tour);
	                }
	                default -> throw new IllegalStateException("Unexpected value: " + tourActivity);
	            }
				lspShipment.addSimulationTracker(tourHandler);
	            lspShipment.addSimulationTracker(serviceHandler);
	            break;
	        }
	    }
	}

}
