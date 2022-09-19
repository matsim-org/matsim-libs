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

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Schedules the {@link CollectionCarrierResource}.
 * <p>
 * Converts the {@link LSPShipment}s into {@link CarrierService}s that are needed for the {@link Carrier}
 * from the freight contrib of MATSim and then routes the vehicles of this
 * {@link Carrier} through the network by calling the corresponding methods of jsprit
 */
/*package-private*/  class CollectionCarrierScheduler extends LSPResourceScheduler {

	private Carrier carrier;
	private CollectionCarrierResource resource;
	private ArrayList<LSPCarrierPair> pairs;

	CollectionCarrierScheduler() {
		this.pairs = new ArrayList<>();
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
			this.carrier.setSelectedPlan(null);
		}
	}

	@Override
	public void scheduleResource() {
		for (ShipmentWithTime tupleToBeAssigned : shipments) {
			CarrierService carrierService = convertToCarrierService(tupleToBeAssigned);
			carrier.getServices().put(carrierService.getId(), carrierService);
		}

		routeCarrier();
	}

	private CarrierService convertToCarrierService(ShipmentWithTime tuple) {
		Id<CarrierService> serviceId = Id.create(tuple.getShipment().getId().toString(), CarrierService.class);
		CarrierService.Builder builder = CarrierService.Builder.newInstance(serviceId, tuple.getShipment().getFrom());
		builder.setServiceStartTimeWindow(tuple.getShipment().getPickupTimeWindow());
		builder.setCapacityDemand(tuple.getShipment().getSize());
		builder.setServiceDuration(tuple.getShipment().getDeliveryServiceTime());
		CarrierService service = builder.build();
		pairs.add(new LSPCarrierPair(tuple, service));
		return service;
	}

	private void routeCarrier() {
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, resource.getNetwork());
		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(resource.getNetwork(), UsecaseUtils.getVehicleTypeCollection(carrier));
		NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
		vrpBuilder.setRoutingCost(netbasedTransportcosts);
		VehicleRoutingProblem vrp = vrpBuilder.build();

		VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(vrp);
		algorithm.setMaxIterations(1);
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

		VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);

		CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
		NetworkRouter.routePlan(plan, netbasedTransportcosts);
		carrier.setSelectedPlan(plan);
	}

	@Override
	protected void updateShipments() {
		for (ShipmentWithTime tuple : shipments) {
			updateShipment(tuple);
		}
	}

	private void updateShipment(ShipmentWithTime tuple) {

		//outerLoop:
		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			Tour tour = scheduledTour.getTour();
			for (TourElement element : tour.getTourElements()) {
				if (element instanceof Tour.ServiceActivity) {
					Tour.ServiceActivity serviceActivity = (Tour.ServiceActivity) element;
					LSPCarrierPair carrierPair = new LSPCarrierPair(tuple, serviceActivity.getService());
					for (LSPCarrierPair pair : pairs) {
						if (pair.tuple == carrierPair.tuple && pair.service.getId() == carrierPair.service.getId()) {
							addShipmentLoadElement(tuple, tour, serviceActivity);
							addShipmentTransportElement(tuple, tour, serviceActivity);
							addShipmentUnloadElement(tuple, tour, serviceActivity);
							addCollectionTourEndEventHandler(pair.service, tuple, resource, tour);
							addCollectionServiceEventHandler(pair.service, tuple, resource, tour);
							//				break outerLoop;
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
		int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
		Leg legBeforeService = (Leg) tour.getTourElements().get(serviceIndex - 1);
		double startTimeOfLoading = legBeforeService.getExpectedDepartureTime() + legBeforeService.getExpectedTransportTime();
		builder.setStartTime(startTimeOfLoading);
		builder.setEndTime(startTimeOfLoading + tuple.getShipment().getDeliveryServiceTime());
		builder.setCarrierId(carrier.getId());
		builder.setLinkId(serviceActivity.getLocation());
		builder.setCarrierService(serviceActivity.getService());
		ShipmentPlanElement load = builder.build();
		String idString = load.getResourceId() + "" + load.getSolutionElement().getId() + "" + load.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, load);
	}

	private void addCollectionServiceEventHandler(CarrierService carrierService, ShipmentWithTime tuple, LSPCarrierResource resource, Tour tour) {
		for (LogisticsSolutionElement element : this.resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				CollectionServiceEndEventHandler endHandler = new CollectionServiceEndEventHandler(carrierService, tuple.getShipment(), element, resource);
				tuple.getShipment().addSimulationTracker(endHandler);
				break;
			}
		}

	}

	private void addCollectionTourEndEventHandler(CarrierService carrierService, ShipmentWithTime tuple, LSPCarrierResource resource, Tour tour) {
		for (LogisticsSolutionElement element : this.resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				CollectionTourEndEventHandler handler = new CollectionTourEndEventHandler(carrierService, tuple.getShipment(), element, resource, tour);
				tuple.getShipment().addSimulationTracker(handler);
				break;
			}
		}

	}

	private void addShipmentTransportElement(ShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
		ShipmentUtils.ScheduledShipmentTransportBuilder builder = ShipmentUtils.ScheduledShipmentTransportBuilder.newInstance();
		builder.setResourceId(resource.getId());
		for (LogisticsSolutionElement element : resource.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				builder.setLogisticsSolutionElement(element);
			}
		}
		int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
		Leg legAfterService = (Leg) tour.getTourElements().get(serviceIndex + 1);
		double startTimeOfTransport = legAfterService.getExpectedDepartureTime();
		builder.setStartTime(startTimeOfTransport);
		Leg lastLeg = (Leg) tour.getTourElements().get(tour.getTourElements().size() - 1);
		double endTimeOfTransport = lastLeg.getExpectedDepartureTime() + lastLeg.getExpectedTransportTime();
		builder.setEndTime(endTimeOfTransport);
		builder.setCarrierId(carrier.getId());
		builder.setFromLinkId(serviceActivity.getLocation());
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
		Leg lastLeg = (Leg) tour.getTourElements().get(tour.getTourElements().size() - 1);
		double startTime = lastLeg.getExpectedDepartureTime() + lastLeg.getExpectedTransportTime();
		builder.setStartTime(startTime);
		builder.setEndTime(startTime + getUnloadEndTime(tour));
		builder.setCarrierId(carrier.getId());
		builder.setLinkId(tour.getEndLinkId());
		builder.setCarrierService(serviceActivity.getService());
		ShipmentPlanElement unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, unload);
	}

	private double getUnloadEndTime(Tour tour) {
		double unloadEndTime = 0;
		for (TourElement element : tour.getTourElements()) {
			if (element instanceof Tour.ServiceActivity) {
				Tour.ServiceActivity serviceActivity = (Tour.ServiceActivity) element;
				unloadEndTime = unloadEndTime + serviceActivity.getDuration();
			}
		}

		return unloadEndTime;
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
