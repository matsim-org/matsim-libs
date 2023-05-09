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

import lsp.LSPResource;
import lsp.LSPSimulationTracker;
import lsp.LogisticChainElement;
import lsp.events.HandlingInHubEndsEvent;
import lsp.events.HandlingInHubStartsEvent;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentLeg;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.contrib.freight.events.FreightTourEndEvent;
import org.matsim.contrib.freight.events.eventhandler.FreightTourEndEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/*package-private*/  class TransshipmentHubTourEndEventHandler implements BeforeMobsimListener, AfterMobsimListener, LSPSimulationTracker<LSPResource>, FreightTourEndEventHandler {

//	@Inject Scenario scenario;
	private final Scenario scenario;
	private final HashMap<CarrierService, TransshipmentHubEventHandlerPair> servicesWaitedFor;
	private final TransshipmentHub transshipmentHub;
	private final Id<LSPResource> resourceId;
	private final Id<Link> linkId;
	private EventsManager eventsManager;

	/**
	 * What is a TranshipmentHubTour ??? KMT, Sep 22
	 *
	 * @param transshipmentHub
	 * @param scenario
	 */
	TransshipmentHubTourEndEventHandler(TransshipmentHub transshipmentHub, Scenario scenario) {
		this.transshipmentHub = transshipmentHub;
		this.linkId = transshipmentHub.getEndLinkId();
		this.resourceId = transshipmentHub.getId();
		this.scenario = scenario;
		this.servicesWaitedFor = new HashMap<>();
		this.transshipmentHub.addSimulationTracker(this);
	}

	@Override
	public void setEmbeddingContainer(LSPResource pointer) {
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		servicesWaitedFor.clear(); // cleanup after Mobsim ends (instead of doing it in reset() = before Mobsim starts.) kmt oct'22
	}

	@Override
	public void reset(int iteration) {
		// not implemented; cleanup is done after Mobsim ends, because the internal state is (re)set before Mobsim starts.
		// --> cleaning up here is too late.
		// This is maybe not ideal, but works; kmt oct'22
	}

	public void addShipment(LSPShipment shipment, LogisticChainElement solutionElement) {
		TransshipmentHubEventHandlerPair pair = new TransshipmentHubEventHandlerPair(shipment, solutionElement);

		for (ShipmentPlanElement planElement : shipment.getShipmentPlan().getPlanElements().values()) {
			if (planElement instanceof ShipmentLeg transport) {
				if (transport.getLogisticChainElement().getNextElement() == solutionElement) {
					servicesWaitedFor.put(transport.getCarrierService(), pair);
				}
			}
		}
	}

	@Override
	public void handleEvent(FreightTourEndEvent event) {
		Tour tour = null;
		Carrier carrier = FreightUtils.getCarriers(scenario).getCarriers().get(event.getCarrierId());
		Collection<ScheduledTour> scheduledTours = carrier.getSelectedPlan().getScheduledTours();
		for (ScheduledTour scheduledTour : scheduledTours) {
			if (scheduledTour.getTour().getId() == event.getTourId()) {
				tour = scheduledTour.getTour();
				break;
			}
		}
		if ((event.getLinkId() == this.linkId)) {
			assert tour != null;
			if (allShipmentsOfTourEndInOnePoint(tour)) {
				for (TourElement tourElement : tour.getTourElements()) {
					if (tourElement instanceof ServiceActivity serviceActivity) {
						if (serviceActivity.getLocation() == transshipmentHub.getStartLinkId()
								&& allServicesAreInOnePoint(tour)
								&& (tour.getStartLinkId() != transshipmentHub.getStartLinkId())) {
							logHandlingInHub(serviceActivity.getService(), event.getTime());
						} else {
							logHandlingInHub(serviceActivity.getService(), event.getTime() + getUnloadEndTime(tour));
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

	private void logHandlingInHub(CarrierService carrierService, double startTime) {
		LSPShipment lspShipment = servicesWaitedFor.get(carrierService).shipment;
		ShipmentUtils.LoggedShipmentHandleBuilder builder = ShipmentUtils.LoggedShipmentHandleBuilder.newInstance();
		builder.setLinkId(linkId);
		builder.setResourceId(resourceId);
		builder.setStartTime(startTime);
		double handlingTime = transshipmentHub.getCapacityNeedFixed() + transshipmentHub.getCapacityNeedLinear() * lspShipment.getSize();
		builder.setEndTime(startTime + handlingTime);
		builder.setLogisticsChainElement(servicesWaitedFor.get(carrierService).element);
		ShipmentPlanElement handle = builder.build();
		String idString = handle.getResourceId() + "" + handle.getLogisticChainElement().getId() + "" + handle.getElementType();
		Id<ShipmentPlanElement> loadId = Id.create(idString, ShipmentPlanElement.class);
		if (!lspShipment.getLog().getPlanElements().containsKey(loadId)) {
			lspShipment.getLog().addPlanElement(loadId, handle);
		}
		eventsManager.processEvent(new HandlingInHubStartsEvent(startTime, linkId, lspShipment.getId(), resourceId));
		eventsManager.processEvent(new HandlingInHubEndsEvent(startTime + handlingTime, linkId, lspShipment.getId(), resourceId));

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

	@Override public void notifyBeforeMobsim(BeforeMobsimEvent beforeMobsimEvent) {
		eventsManager = beforeMobsimEvent.getServices().getEvents();
	}

	static class TransshipmentHubEventHandlerPair {
		public final LSPShipment shipment;
		public final LogisticChainElement element;

		public TransshipmentHubEventHandlerPair(LSPShipment shipment, LogisticChainElement element) {
			this.shipment = shipment;
			this.element = element;
		}
	}


}
