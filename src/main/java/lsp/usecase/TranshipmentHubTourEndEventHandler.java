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

import java.util.HashMap;
import java.util.Map;

import lsp.LSPSimulationTracker;
import lsp.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

import org.matsim.contrib.freight.events.FreightTourEndEvent;
import org.matsim.contrib.freight.events.eventhandler.FreightTourEndEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.LSPResource;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

/*package-private*/  class TranshipmentHubTourEndEventHandler implements AfterMobsimListener, LSPSimulationTracker<LSPResource>, FreightTourEndEventHandler {

	private final HashMap<CarrierService, TransshipmentHubEventHandlerPair> servicesWaitedFor;
	private final TransshipmentHub transshipmentHub;
	private final Id<LSPResource> resourceId;
	private final Id<Link> linkId;

	TranshipmentHubTourEndEventHandler(TransshipmentHub transshipmentHub) {
		this.transshipmentHub = transshipmentHub;
		this.linkId = transshipmentHub.getEndLinkId();
		this.resourceId = transshipmentHub.getId();
		this.servicesWaitedFor = new HashMap<>();
	}

	@Override
	public void setEmbeddingContainer(LSPResource pointer) {
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
	}

	@Override
	public void reset(int iteration) {
		servicesWaitedFor.clear();
	}

	public void addShipment(LSPShipment shipment, LogisticsSolutionElement solutionElement) {
		TransshipmentHubEventHandlerPair pair = new TransshipmentHubEventHandlerPair(shipment, solutionElement);

		for (ShipmentPlanElement planElement : shipment.getShipmentPlan().getPlanElements().values()) {
			if (planElement instanceof ShipmentLeg transport) {
				if (transport.getSolutionElement().getNextElement() == solutionElement) {
					servicesWaitedFor.put(transport.getCarrierService(), pair);
				}
			}
		}
	}

	@Override
	public void handleEvent(FreightTourEndEvent event) {
		if ((event.getLinkId() == this.linkId) && (shipmentsOfTourEndInPoint(event.getTour()))) {

			for (TourElement tourElement : event.getTour().getTourElements()) {
				if (tourElement instanceof ServiceActivity serviceActivity) {
					if (serviceActivity.getLocation() == transshipmentHub.getStartLinkId()
							&& allServicesAreInOnePoint(event.getTour())
							&& (event.getTour().getStartLinkId() != transshipmentHub.getStartLinkId())) {
						logReloadAfterMainRun(serviceActivity.getService(), event);
					} else {
						logReloadAfterCollection(serviceActivity.getService(), event);
					}
				}

			}
		}


	}

	private boolean shipmentsOfTourEndInPoint(Tour tour) {
		boolean shipmentsEndInPoint = true;
		for (TourElement tourElement : tour.getTourElements()) {
			if (tourElement instanceof ServiceActivity serviceActivity) {
				if (!servicesWaitedFor.containsKey(serviceActivity.getService())) {
					return false;
				}
			}
		}
		return shipmentsEndInPoint;
	}

	private void logReloadAfterCollection(CarrierService carrierService, FreightTourEndEvent event) {
		LSPShipment lspShipment = servicesWaitedFor.get(carrierService).shipment;
		ShipmentUtils.LoggedShipmentHandleBuilder builder = ShipmentUtils.LoggedShipmentHandleBuilder.newInstance();
		builder.setLinkId(linkId);
		builder.setResourceId(resourceId);
		double startTime = event.getTime() + getUnloadEndTime(event.getTour());
		builder.setStartTime(startTime);
		double handlingTime = transshipmentHub.getCapacityNeedFixed() + transshipmentHub.getCapacityNeedLinear() * lspShipment.getSize();
		builder.setEndTime(startTime + handlingTime);
		builder.setLogisticsSolutionElement(servicesWaitedFor.get(carrierService).element);
		ShipmentPlanElement loggedShipmentHandle = builder.build();
		String idString = loggedShipmentHandle.getResourceId() + "" + loggedShipmentHandle.getSolutionElement().getId() + "" + loggedShipmentHandle.getElementType();
		Id<ShipmentPlanElement> loadId = Id.create(idString, ShipmentPlanElement.class);
		if (!lspShipment.getLog().getPlanElements().containsKey(loadId)) {
			lspShipment.getLog().addPlanElement(loadId, loggedShipmentHandle);
		}
	}

	private double getUnloadEndTime(Tour tour) {
		double unloadEndTime = 0;
		for (TourElement element : tour.getTourElements()) {
			if (element instanceof ServiceActivity serviceActivity) {
				unloadEndTime = unloadEndTime + serviceActivity.getDuration();
			}
		}


		return unloadEndTime;
	}

	private void logReloadAfterMainRun(CarrierService carrierService, FreightTourEndEvent event) {
		LSPShipment lspShipment = servicesWaitedFor.get(carrierService).shipment;
		ShipmentUtils.LoggedShipmentHandleBuilder builder = ShipmentUtils.LoggedShipmentHandleBuilder.newInstance();
		builder.setLinkId(linkId);
		builder.setResourceId(resourceId);
		double startTime = event.getTime();
		builder.setStartTime(startTime);
		double handlingTime = transshipmentHub.getCapacityNeedFixed() + transshipmentHub.getCapacityNeedLinear() * lspShipment.getSize();
		builder.setEndTime(startTime + handlingTime);
		builder.setLogisticsSolutionElement(servicesWaitedFor.get(carrierService).element);
		ShipmentPlanElement handle = builder.build();
		String idString = handle.getResourceId() + "" + handle.getSolutionElement().getId() + "" + handle.getElementType();
		Id<ShipmentPlanElement> loadId = Id.create(idString, ShipmentPlanElement.class);
		if (!lspShipment.getLog().getPlanElements().containsKey(loadId)) {
			lspShipment.getLog().addPlanElement(loadId, handle);
		}
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

	public TransshipmentHub getTranshipmentHub() {
		return transshipmentHub;
	}

	public Id<LSPResource> getResourceId() {
		return resourceId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	static class TransshipmentHubEventHandlerPair {
		public final LSPShipment shipment;
		public final LogisticsSolutionElement element;

		public TransshipmentHubEventHandlerPair(LSPShipment shipment, LogisticsSolutionElement element) {
			this.shipment = shipment;
			this.element = element;
		}
	}


}
