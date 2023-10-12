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

package lsp.resourceImplementations.collectionCarrier;

import lsp.LSPCarrierResource;
import lsp.LSPResource;
import lsp.LSPSimulationTracker;
import lsp.LogisticChainElement;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentLeg;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.eventhandler.FreightTourEndEventHandler;

public class CollectionTourEndEventHandler implements AfterMobsimListener, FreightTourEndEventHandler, LSPSimulationTracker<LSPShipment> {
// Todo: I have made it (temporarily) public because of junit tests :( -- need to find another way to do the junit testing. kmt jun'23

	private final CarrierService carrierService;
	private final LogisticChainElement logisticChainElement;
	private final LSPCarrierResource resource;
	private LSPShipment lspShipment;
	private final Tour tour;

	CollectionTourEndEventHandler(CarrierService carrierService, LSPShipment lspShipment, LogisticChainElement element, LSPCarrierResource resource, Tour tour) {
		this.carrierService = carrierService;
		this.lspShipment = lspShipment;
		this.logisticChainElement = element;
		this.resource = resource;
		this.tour = tour;
	}


	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(CarrierTourEndEvent event) {
		if (event.getTourId().equals(tour.getId())){
			for (TourElement element : tour.getTourElements()) {
				if (element instanceof ServiceActivity serviceActivity) {
					if (serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()) {
						logTransport(event, tour);
						logUnload(event, tour);
					}
				}
			}
		}
	}

	private void logUnload(CarrierTourEndEvent event, Tour tour) {
		ShipmentUtils.LoggedShipmentUnloadBuilder builder = ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
		builder.setStartTime(event.getTime());
		builder.setEndTime(event.getTime() + getTotalUnloadingTime(tour));
		builder.setLogisticChainElement(logisticChainElement);
		builder.setResourceId(resource.getId());
		builder.setCarrierId(event.getCarrierId());
		ShipmentPlanElement unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getLogisticChainElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getShipmentLog().addPlanElement(unloadId, unload);
	}

	private void logTransport(CarrierTourEndEvent event, Tour tour) {
		String idString = resource.getId() + "" + logisticChainElement.getId() + "" + "TRANSPORT";
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		ShipmentPlanElement abstractPlanElement = lspShipment.getShipmentLog().getPlanElements().get(id);
		if (abstractPlanElement instanceof ShipmentLeg transport) {
			transport.setEndTime(event.getTime());
			transport.setToLinkId(tour.getEndLinkId());
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


	public LogisticChainElement getElement() {
		return logisticChainElement;
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

	
	
	
	

