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

import org.matsim.contrib.freight.events.FreightTourEndEvent;
import org.matsim.contrib.freight.events.eventhandler.FreightTourEndEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.LSPCarrierResource;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.EventHandler;

import java.util.ArrayList;
import java.util.Collection;

/*package-private*/ class MainRunTourEndEventHandler implements AfterMobsimListener, FreightTourEndEventHandler, LSPSimulationTracker<LSPShipment> {

	private final CarrierService carrierService;
	private final LogisticsSolutionElement solutionElement;
	private final LSPCarrierResource resource;
	private final Collection<EventHandler> eventHandlers = new ArrayList<>();
	private LSPShipment lspShipment;


	MainRunTourEndEventHandler(LSPShipment lspShipment, CarrierService carrierService, LogisticsSolutionElement solutionElement, LSPCarrierResource resource) {
		this.lspShipment = lspShipment;
		this.carrierService = carrierService;
		this.solutionElement = solutionElement;
		this.resource = resource;
	}


	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}


	@Override
	public void handleEvent(FreightTourEndEvent event) {
		for (TourElement tourElement : event.getTour().getTourElements()) {
			if (tourElement instanceof ServiceActivity) {
				ServiceActivity serviceActivity = (ServiceActivity) tourElement;
				if (serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()) {
					logUnload(event);
					logTransport(event);
				}
			}
		}
	}

	private void logUnload(FreightTourEndEvent event) {
		ShipmentUtils.LoggedShipmentUnloadBuilder builder = ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
		builder.setStartTime(event.getTime() - getTotalUnloadingTime(event.getTour()));
		builder.setEndTime(event.getTime());
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		builder.setCarrierId(event.getCarrierId());
		ShipmentPlanElement unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().addPlanElement(unloadId, unload);
	}

	private void logTransport(FreightTourEndEvent event) {
		String idString = resource.getId() + "" + solutionElement.getId() + "" + "TRANSPORT";
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		ShipmentPlanElement abstractPlanElement = lspShipment.getLog().getPlanElements().get(id);
		if (abstractPlanElement instanceof ShipmentLeg) {
			ShipmentLeg transport = (ShipmentLeg) abstractPlanElement;
			transport.setEndTime(event.getTime() - getTotalUnloadingTime(event.getTour()));
			transport.setToLinkId(event.getTour().getEndLinkId());
		}
	}

	private double getTotalUnloadingTime(Tour tour) {
		double totalTime = 0;
		for (TourElement element : tour.getTourElements()) {
			if (element instanceof ServiceActivity) {
				ServiceActivity serviceActivity = (ServiceActivity) element;
				totalTime = totalTime + serviceActivity.getDuration();
			}
		}
		return totalTime;
	}


	public LSPShipment getLspShipment() {
		return lspShipment;
	}


	public CarrierService getCarrierService() {
		return carrierService;
	}


	public LogisticsSolutionElement getSolutionElement() {
		return solutionElement;
	}


	public LSPCarrierResource getResource() {
		return resource;
	}


	@Override public void setEmbeddingContainer( LSPShipment pointer ){
		this.lspShipment = pointer;
	}

	@Override public void notifyAfterMobsim( AfterMobsimEvent event ){
	}
}
