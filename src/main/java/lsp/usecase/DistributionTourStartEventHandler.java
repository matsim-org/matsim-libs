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

import lsp.controler.LSPSimulationTracker;
import lsp.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

import org.matsim.contrib.freight.events.FreightTourStartEvent;
import org.matsim.contrib.freight.events.eventhandler.FreightTourStartEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.LSPCarrierResource;
import org.matsim.core.events.handler.EventHandler;

import java.util.ArrayList;
import java.util.Collection;

/*package-private*/  class DistributionTourStartEventHandler implements FreightTourStartEventHandler, LSPSimulationTracker<LSPShipment> {

	private final CarrierService carrierService;
	private final LogisticsSolutionElement element;
	private final LSPCarrierResource resource;
	private final Collection<EventHandler> eventHandlers = new ArrayList<>();
	private LSPShipment lspShipment;

	DistributionTourStartEventHandler(CarrierService carrierService, LSPShipment lspShipment, LogisticsSolutionElement element, LSPCarrierResource resource) {
		this.carrierService = carrierService;
		this.lspShipment = lspShipment;
		this.element = element;
		this.resource = resource;
	}


	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(FreightTourStartEvent event) {
		for (TourElement tourElement : event.getTour().getTourElements()) {
			if (tourElement instanceof ServiceActivity serviceActivity) {
				if (serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()) {
					logLoad(event);
					logTransport(event);
				}
			}
		}
	}

	private void logLoad(FreightTourStartEvent event) {
		ShipmentUtils.LoggedShipmentLoadBuilder builder = ShipmentUtils.LoggedShipmentLoadBuilder.newInstance();
		builder.setCarrierId(event.getCarrierId());
		builder.setLinkId(event.getLinkId());
		builder.setLogisticsSolutionElement(element);
		builder.setResourceId(resource.getId());
		builder.setEndTime(event.getTime());
		builder.setStartTime(event.getTime() - getCumulatedLoadingTime(event.getTour()));
		ShipmentPlanElement loggedShipmentLoad = builder.build();
		String idString = loggedShipmentLoad.getResourceId() + "" + loggedShipmentLoad.getSolutionElement().getId() + "" + loggedShipmentLoad.getElementType();
		Id<ShipmentPlanElement> loadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().addPlanElement(loadId, loggedShipmentLoad);
	}

	private void logTransport(FreightTourStartEvent event) {
		ShipmentUtils.LoggedShipmentTransportBuilder builder = ShipmentUtils.LoggedShipmentTransportBuilder.newInstance();
		builder.setCarrierId(event.getCarrierId());
		builder.setFromLinkId(event.getLinkId());
		builder.setToLinkId(event.getTour().getEndLinkId());
		builder.setLogisticsSolutionElement(element);
		builder.setResourceId(resource.getId());
		builder.setStartTime(event.getTime());
		ShipmentPlanElement transport = builder.build();
		String idString = transport.getResourceId() + "" + transport.getSolutionElement().getId() + "" + transport.getElementType();
		Id<ShipmentPlanElement> transportId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().addPlanElement(transportId, transport);
	}

	private double getCumulatedLoadingTime(Tour tour) {
		double cumulatedLoadingTime = 0;
		for (TourElement tourElement : tour.getTourElements()) {
			if (tourElement instanceof ServiceActivity serviceActivity) {
				cumulatedLoadingTime = cumulatedLoadingTime + serviceActivity.getDuration();
			}
		}
		return cumulatedLoadingTime;
	}


	public CarrierService getCarrierService() {
		return carrierService;
	}


	public LSPShipment getLspShipment() {
		return lspShipment;
	}


	public LogisticsSolutionElement getElement() {
		return element;
	}


	public LSPCarrierResource getResource() {
		return resource;
	}


	@Override public void setEmbeddingContainer( LSPShipment pointer ){
		this.lspShipment = pointer;
	}

}
