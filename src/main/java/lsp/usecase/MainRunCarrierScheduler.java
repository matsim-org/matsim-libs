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
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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
			this.carrier.setSelectedPlan(null);
		}
	}
	@Override protected void scheduleResource() {
		int load = 0;
		List<ShipmentWithTime> copyOfAssignedShipments = new ArrayList<>(shipments);
		copyOfAssignedShipments.sort(Comparator.comparingDouble(ShipmentWithTime::getTime));
		ArrayList<ShipmentWithTime> shipmentsInCurrentTour = new ArrayList<>();
		ArrayList<ScheduledTour> scheduledTours = new ArrayList<>();

		for (ShipmentWithTime tuple : copyOfAssignedShipments) {
			VehicleType vehicleType = UsecaseUtils.getVehicleTypeCollection(carrier).iterator().next();
			if ((load + tuple.getShipment().getSize()) <= vehicleType.getCapacity().getOther().intValue()) {
				shipmentsInCurrentTour.add(tuple);
				load = load + tuple.getShipment().getSize();
			} else {
				load = 0;
				CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
				scheduledTours.addAll(plan.getScheduledTours());
				shipmentsInCurrentTour.clear();
				shipmentsInCurrentTour.add(tuple);
				load = load + tuple.getShipment().getSize();
			}

		}
		if (!shipmentsInCurrentTour.isEmpty()) {
			CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
			scheduledTours.addAll(plan.getScheduledTours());
			shipmentsInCurrentTour.clear();
		}
		CarrierPlan plan = new CarrierPlan(carrier, scheduledTours);
		carrier.setSelectedPlan(plan);
	}

	private int tourIdindex = 1; //Have unique TourIds for the MainRun.
	private CarrierPlan createPlan(Carrier carrier, List<ShipmentWithTime> tuples) {

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
		tourBuilder.scheduleEnd(Id.create(resource.getEndLinkId(), Link.class));
		org.matsim.contrib.freight.carrier.Tour vehicleTour = tourBuilder.build();
		CarrierVehicle vehicle = carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
		double tourStartTime = latestTupleTime + totalLoadingTime;
		ScheduledTour sTour = ScheduledTour.newInstance(vehicleTour, vehicle, tourStartTime);
		tours.add(sTour);
		CarrierPlan plan = new CarrierPlan(carrier, tours);
		NetworkRouter.routePlan(plan, netbasedTransportcosts);
		return plan;
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
		for (LogisticsSolutionElement element : resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				builder.setLogisticsSolutionElement(element);
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
		String idString = load.getResourceId() + "" + load.getSolutionElement().getId() + "" + load.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, load);
	}

	private void addShipmentTransportElement(ShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
		ShipmentUtils.ScheduledShipmentTransportBuilder builder = ShipmentUtils.ScheduledShipmentTransportBuilder.newInstance();
		builder.setResourceId(resource.getId());
		for (LogisticsSolutionElement element : resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				builder.setLogisticsSolutionElement(element);
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
		String idString = transport.getResourceId() + "" + transport.getSolutionElement().getId() + "" + transport.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, transport);
	}

	private void addShipmentUnloadElement(ShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
		ShipmentUtils.ScheduledShipmentUnloadBuilder builder = ShipmentUtils.ScheduledShipmentUnloadBuilder.newInstance();
		builder.setResourceId(resource.getId());
		for (LogisticsSolutionElement element : resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				builder.setLogisticsSolutionElement(element);
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
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, unload);
	}

	private void addMainTourRunStartEventHandler(CarrierService carrierService, ShipmentWithTime tuple, LSPCarrierResource resource, Tour tour) {
		for (LogisticsSolutionElement element : this.resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				MainRunTourStartEventHandler handler = new MainRunTourStartEventHandler(tuple.getShipment(), carrierService, element, resource, tour);
				tuple.getShipment().addSimulationTracker(handler);
				break;
			}
		}
	}

	private void addMainRunTourEndEventHandler(CarrierService carrierService, ShipmentWithTime tuple, LSPCarrierResource resource, Tour tour) {
		for (LogisticsSolutionElement element : this.resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				MainRunTourEndEventHandler handler = new MainRunTourEndEventHandler(tuple.getShipment(), carrierService, element, resource, tour);
				tuple.getShipment().addSimulationTracker(handler);
				break;
			}
		}

	}

	static class LSPCarrierPair {
		private final ShipmentWithTime tuple;
		private final CarrierService service;

		public LSPCarrierPair(ShipmentWithTime tuple, CarrierService service) {
			this.tuple = tuple;
			this.service = service;
		}

	}
}
