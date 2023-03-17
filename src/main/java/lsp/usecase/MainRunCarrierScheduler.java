/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package lsp.usecase;

import lsp.*;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.VehicleType;

import java.util.*;

/**
 * In the case of the MainRunResource, the incoming LSPShipments are bundled
 * together until their total weight exceeds the capacity of the deployed vehicle
 * type. Then, this bundle of LSPShipments is converted to a scheduled tour
 * from the freight contrib of MATSim. The start of this tour is located at
 * the first TranshipmentHub and the end at the second one. All LSPShipments
 * are converted to services that take place at the end point of the tour.
 * <p>
 * Tour is routed by MATSim Network Router.
 * <p>
 * * The tour starts after the last shipment
 * * has arrived and the time necessary for loading all shipments into the vehicle
 * * has passed.
 */
/*package-private*/  class MainRunCarrierScheduler extends LSPResourceScheduler {

	private Carrier carrier;
	private MainRunCarrierResource resource;
	private ArrayList<LSPCarrierPair> pairs;

	/*package-private*/   MainRunCarrierScheduler() {
		this.pairs = new ArrayList<>();
	}
	
	@Override protected void initializeValues( LSPResource resource ) {
		this.pairs = new ArrayList<>();
		if (resource.getClass() == MainRunCarrierResource.class) {
			this.resource = (MainRunCarrierResource) resource;
			this.carrier = this.resource.getCarrier();
			this.carrier.getServices().clear();
			this.carrier.getShipments().clear();
			this.carrier.getPlans().clear();
		}
	}
	@Override protected void scheduleResource() {
		int load = 0;
		List<ShipmentWithTime> copyOfAssignedShipments = new ArrayList<>(shipments);
		copyOfAssignedShipments.sort(Comparator.comparingDouble(ShipmentWithTime::getTime));
		ArrayList<ShipmentWithTime> shipmentsInCurrentTour = new ArrayList<>();
//		ArrayList<ScheduledTour> scheduledTours = new ArrayList<>();
		List<CarrierPlan> scheduledPlans = new LinkedList<>();
		
		for (ShipmentWithTime tuple : copyOfAssignedShipments) {
			VehicleType vehicleType = UsecaseUtils.getVehicleTypeCollection(carrier).iterator().next();
			if ((load + tuple.getShipment().getSize()) <= vehicleType.getCapacity().getOther().intValue()) {
				shipmentsInCurrentTour.add(tuple);
				load = load + tuple.getShipment().getSize();
			} else {
				load = 0;
				CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
//				scheduledTours.addAll(plan.getScheduledTours());
				scheduledPlans.add(plan);
				shipmentsInCurrentTour.clear();
				shipmentsInCurrentTour.add(tuple);
				load = load + tuple.getShipment().getSize();
			}

		}
		if (!shipmentsInCurrentTour.isEmpty()) {
			CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
//			scheduledTours.addAll(plan.getScheduledTours());
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

	private int tourIdindex = 1; //Have unique TourIds for the MainRun.
	private CarrierPlan createPlan(Carrier carrier, List<ShipmentWithTime> tuples) {

		//TODO: Allgemein: Hier ist alles manuell zusammen gesetzt; es findet KEINE Tourenplanung statt!
		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(resource.getNetwork(), UsecaseUtils.getVehicleTypeCollection(resource.getCarrier()));
		NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
		Collection<ScheduledTour> tours = new ArrayList<>();

		Tour.Builder tourBuilder = Tour.Builder.newInstance(Id.create(tourIdindex, Tour.class));
		tourIdindex++;
		tourBuilder.scheduleStart(Id.create(resource.getStartLinkId(), Link.class));

		double totalLoadingTime = 0;
		double latestTupleTime = 0;

		for (ShipmentWithTime tuple : tuples) {
			totalLoadingTime = totalLoadingTime + tuple.getShipment().getDeliveryServiceTime();
			if (tuple.getTime() > latestTupleTime) {
				latestTupleTime = tuple.getTime();
			}
			tourBuilder.addLeg(new Leg());
			CarrierService carrierService = convertToCarrierService(tuple);
			tourBuilder.scheduleService(carrierService);
		}

		tourBuilder.addLeg(new Leg());
		switch (resource.getVehicleReturn()) {
			case returnToFromLink -> //The more "urban" behaviour: The vehicle returns to its origin (startLink).
					tourBuilder.scheduleEnd(Id.create(resource.getStartLinkId(), Link.class));
			case endAtToLink -> //The more "long-distance" behaviour: The vehicle ends at its destination (toLink).
					tourBuilder.scheduleEnd(Id.create(resource.getEndLinkId(), Link.class));
			default -> throw new IllegalStateException("Unexpected value: " + resource.getVehicleReturn());
		}
		org.matsim.contrib.freight.carrier.Tour vehicleTour = tourBuilder.build();
		CarrierVehicle vehicle = carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
		double tourStartTime = latestTupleTime + totalLoadingTime;
		ScheduledTour sTour = ScheduledTour.newInstance(vehicleTour, vehicle, tourStartTime);

		tours.add(sTour);
		CarrierPlan plan = new CarrierPlan(carrier, tours);
		NetworkRouter.routePlan(plan, netbasedTransportcosts);
		plan.setScore(scorePlanManually(plan));
		return plan;
	}

	/**
	 * For the main run, there is currently (nov'22) no jsprit planning.
	 * The plan is instead constructed manually. As a consequence, there is no score (from jsprit) for this plan available.
	 * To avoid issues in later scoring of the LSP, we would like to hava also a score for the MainRunCarrier.
	 * This is calculated here manually
	 * <p>
	 *  It bases on the
	 *  - vehicle's fixed costs
	 *  - distance dependent costs
	 *  - (expected) travel time dependent costs
	 *  NOT included is the calculation of activity times,... But this is currently also missing e.g. in the distributionCarrier, where the VRP setup
	 *  does not include this :(
	 *
	 * @param plan The carrierPlan, that should get scored.
	 * @return the calculated score
	 */
	private double scorePlanManually(CarrierPlan plan) {
		//score plan // Note: Activities are not scored, but they are also NOT scored for the Distribution carrier (as the VRP is currently set up) kmt nov'22
		double score = 0.;
		for (ScheduledTour scheduledTour : plan.getScheduledTours()) {
			//vehicle fixed costs
			score = score + scheduledTour.getVehicle().getType().getCostInformation().getFixedCosts();

			//distance
			double distance =  0.0;
			double time = 0.0;
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {
				if (tourElement instanceof Leg leg){
					//distance
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					for (Id<Link> linkId : route.getLinkIds()) {
						distance = distance + resource.getNetwork().getLinks().get(linkId).getLength();
					}
					if (route.getEndLinkId() != route.getStartLinkId()) { //Do not calculate any distance, if start and endpoint are identical
						distance = distance + resource.getNetwork().getLinks().get(route.getEndLinkId()).getLength();
					}

					//travel time (exp.)
					time = time + leg.getExpectedTransportTime();
				}
			}
			score = score + scheduledTour.getVehicle().getType().getCostInformation().getCostsPerMeter() * distance;
			score = score + scheduledTour.getVehicle().getType().getCostInformation().getCostsPerSecond() * time;
		}
		return (-score); //negative, because we are looking at "costs" instead of "utility"
	}

	private CarrierService convertToCarrierService(ShipmentWithTime tuple) {
		Id<CarrierService> serviceId = Id.create(tuple.getShipment().getId().toString(), CarrierService.class);
		CarrierService.Builder builder = CarrierService.Builder.newInstance(serviceId, resource.getEndLinkId());
		builder.setCapacityDemand(tuple.getShipment().getSize());
		builder.setServiceDuration(tuple.getShipment().getDeliveryServiceTime());
		CarrierService service = builder.build();
		pairs.add(new LSPCarrierPair(tuple, service));
		return service;
	}
	
	@Override protected void updateShipments() {
		for (ShipmentWithTime tuple : shipments) {
			updateSchedule(tuple);
		}
	}

	private void updateSchedule(ShipmentWithTime tuple) {
		//outerLoop:
		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			Tour tour = scheduledTour.getTour();
			for (TourElement element : tour.getTourElements()) {
				if (element instanceof Tour.ServiceActivity serviceActivity) {
					LSPCarrierPair carrierPair = new LSPCarrierPair(tuple, serviceActivity.getService());
					for (LSPCarrierPair pair : pairs) {
						if (pair.tuple == carrierPair.tuple && pair.service.getId() == carrierPair.service.getId()) {
							addShipmentLoadElement(tuple, tour, serviceActivity);
							addShipmentTransportElement(tuple, tour, serviceActivity);
							addShipmentUnloadElement(tuple, tour, serviceActivity);
							addMainTourRunStartEventHandler(pair.service, tuple, resource, tour);
							addMainRunTourEndEventHandler(pair.service, tuple, resource, tour);
							//break outerLoop;
						}
					}
				}
			}
		}
	}

	private void addShipmentLoadElement(ShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
		ShipmentUtils.ScheduledShipmentLoadBuilder builder = ShipmentUtils.ScheduledShipmentLoadBuilder.newInstance();
		builder.setResourceId(resource.getId());
		for (LogisticChainElement element : resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
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
		builder.setCarrierId(carrier.getId());
		builder.setLinkId(tour.getStartLinkId());
		builder.setCarrierService(serviceActivity.getService());
		ShipmentPlanElement load = builder.build();
		String idString = load.getResourceId() + "" + load.getLogisticChainElement().getId() + "" + load.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, load);
	}

	private void addShipmentTransportElement(ShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
		ShipmentUtils.ScheduledShipmentTransportBuilder builder = ShipmentUtils.ScheduledShipmentTransportBuilder.newInstance();
		builder.setResourceId(resource.getId());
		for (LogisticChainElement element : resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				builder.setLogisticChainElement(element);
			}
		}
		int startIndex = tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
		Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
		double startTimeOfTransport = legAfterStart.getExpectedDepartureTime();
		builder.setStartTime(startTimeOfTransport);
		builder.setEndTime(legAfterStart.getExpectedTransportTime() + startTimeOfTransport);
		builder.setCarrierId(carrier.getId());
		builder.setFromLinkId(tour.getStartLinkId());
		builder.setToLinkId(tour.getEndLinkId());
		builder.setCarrierService(serviceActivity.getService());
		ShipmentPlanElement transport = builder.build();
		String idString = transport.getResourceId() + "" + transport.getLogisticChainElement().getId() + "" + transport.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, transport);
	}

	private void addShipmentUnloadElement(ShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
		ShipmentUtils.ScheduledShipmentUnloadBuilder builder = ShipmentUtils.ScheduledShipmentUnloadBuilder.newInstance();
		builder.setResourceId(resource.getId());
		for (LogisticChainElement element : resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				builder.setLogisticsChainElement(element);
			}
		}
		double cumulatedLoadingTime = 0;
		for (TourElement element : tour.getTourElements()) {
			if (element instanceof Tour.ServiceActivity activity) {
				cumulatedLoadingTime = cumulatedLoadingTime + activity.getDuration();
			}
		}
		int startIndex = tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
		Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
		builder.setStartTime(legAfterStart.getExpectedDepartureTime() + legAfterStart.getExpectedTransportTime());
		builder.setEndTime(legAfterStart.getExpectedDepartureTime() + legAfterStart.getExpectedTransportTime() + cumulatedLoadingTime);
		builder.setCarrierId(carrier.getId());
		builder.setLinkId(tour.getEndLinkId());
		builder.setCarrierService(serviceActivity.getService());
		ShipmentPlanElement unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getLogisticChainElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, unload);
	}

	private void addMainTourRunStartEventHandler(CarrierService carrierService, ShipmentWithTime tuple, LSPCarrierResource resource, Tour tour) {
		for (LogisticChainElement element : this.resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				MainRunTourStartEventHandler handler = new MainRunTourStartEventHandler(tuple.getShipment(), carrierService, element, resource, tour);
				tuple.getShipment().addSimulationTracker(handler);
				break;
			}
		}
	}

	private void addMainRunTourEndEventHandler(CarrierService carrierService, ShipmentWithTime tuple, LSPCarrierResource resource, Tour tour) {
		for (LogisticChainElement element : this.resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				MainRunTourEndEventHandler handler = new MainRunTourEndEventHandler(tuple.getShipment(), carrierService, element, resource, tour);
				tuple.getShipment().addSimulationTracker(handler);
				break;
			}
		}
	}

	private record LSPCarrierPair(ShipmentWithTime tuple, CarrierService service) {
	}
}
