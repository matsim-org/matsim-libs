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

import com.google.inject.Inject;
import lsp.LSPSimulationTracker;
import lsp.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

import org.matsim.contrib.freight.events.FreightTourEndEvent;
import org.matsim.contrib.freight.events.eventhandler.FreightTourEndEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.LSPCarrierResource;
import lsp.LSPResource;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.EventHandler;

import java.util.ArrayList;
import java.util.Collection;

/*package-private*/ class CollectionTourEndEventHandler implements AfterMobsimListener, FreightTourEndEventHandler, LSPSimulationTracker<LSPShipment> {

//	@Inject Scenario scenario;
	private final CarrierService carrierService;
	private final LogisticsSolutionElement solutionElement;
	private final LSPCarrierResource resource;
	private final Collection<EventHandler> eventHandlers = new ArrayList<>();
	private LSPShipment lspShipment;
	private final Tour tour;

	CollectionTourEndEventHandler(CarrierService carrierService, LSPShipment lspShipment, LogisticsSolutionElement element, LSPCarrierResource resource, Tour tour) {
		this.carrierService = carrierService;
		this.lspShipment = lspShipment;
		this.solutionElement = element;
		this.resource = resource;
		this.tour = tour;
	}


	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(FreightTourEndEvent event) {
//		Tour tour = null;
//		//TODO: Does not work, because scenario is null -> Need help from KN :(
//		// In the CarrierModul there is already a CarrierProvider returning "return FreightUtils.getCarriers(scenario);" --> How can I access it???
//		// OR
//		// LSPModule -> provideCarriers ??
//		Carrier carrier = FreightUtils.getCarriers(scenario).getCarriers().get(event.getCarrierId());
//		Collection<ScheduledTour> scheduledTours = carrier.getSelectedPlan().getScheduledTours();
//		for (ScheduledTour scheduledTour : scheduledTours) {
//			if (scheduledTour.getVehicle().getId() == event.getVehicleId()) {
//				tour = scheduledTour.getTour();
//				break;
//			}
//		}
		for (TourElement element : tour.getTourElements()) {
			if (element instanceof ServiceActivity serviceActivity) {
				if (serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()) {
					logTransport(event, tour);
					logUnload(event, tour);
				}
			}
		}
	}

	private void logUnload(FreightTourEndEvent event, Tour tour) {
		ShipmentUtils.LoggedShipmentUnloadBuilder builder = ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
		builder.setStartTime(event.getTime());
		builder.setEndTime(event.getTime() + getTotalUnloadingTime(tour));
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		builder.setCarrierId(event.getCarrierId());
		ShipmentPlanElement unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().addPlanElement(unloadId, unload);
	}

	private void logTransport(FreightTourEndEvent event, Tour tour) {
		String idString = resource.getId() + "" + solutionElement.getId() + "" + "TRANSPORT";
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		ShipmentPlanElement abstractPlanElement = lspShipment.getLog().getPlanElements().get(id);
		if (abstractPlanElement instanceof ShipmentLeg transport) {
			//Auskommentiert, im Rahmen des reducing-public-footprint-Prozesses. Kein Test reagiert drauf. Was "sollte" hier geschehen? KMT(&kai) Jun'20
//			transport.setEndTime(event.getTime());
//			transport.setToLinkId(tour.getEndLinkId());
		}
	}

	private double getTotalUnloadingTime(Tour tour) {
		double totalTime = 0;
		for (TourElement element : tour.getTourElements()) {
			if (element instanceof ServiceActivity serviceActivity) {
				totalTime = totalTime + serviceActivity.getDuration();
			}
		}
		return totalTime;
	}


	public CarrierService getCarrierService() {
		return carrierService;
	}


	public LSPShipment getLspShipment() {
		return lspShipment;
	}


	public LogisticsSolutionElement getElement() {
		return solutionElement;
	}


	public Id<LSPResource> getResourceId() {
		return resource.getId();
	}


	@Override
	public void setEmbeddingContainer(LSPShipment pointer) {
		this.lspShipment = pointer;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
	}
}

	
	
	
	

