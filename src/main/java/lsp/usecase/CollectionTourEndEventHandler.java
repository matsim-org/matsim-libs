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

import org.matsim.contrib.freight.events.LSPTourEndEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPTourEndEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.LSPCarrierResource;
import lsp.LSPResource;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.EventHandler;

import java.util.ArrayList;
import java.util.Collection;

/*package-private*/ class CollectionTourEndEventHandler implements AfterMobsimListener, LSPTourEndEventHandler, LSPSimulationTracker<LSPShipment> {

	private final CarrierService carrierService;
	private LSPShipment lspShipment;
	private final LogisticsSolutionElement solutionElement;
	private final LSPCarrierResource resource;
	private final Collection<EventHandler> eventHandlers = new ArrayList<>();

	CollectionTourEndEventHandler(CarrierService carrierService, LSPShipment lspShipment, LogisticsSolutionElement element, LSPCarrierResource resource){
		this.carrierService = carrierService;
		this.lspShipment = lspShipment;
		this.solutionElement = element;
		this.resource = resource;
	}
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LSPTourEndEvent event) {
		Tour tour = event.getTour();
		for(TourElement element : tour.getTourElements()){
			if(element instanceof ServiceActivity){
				ServiceActivity serviceActivity = (ServiceActivity) element;
					if(serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()){
						logTransport(event, tour);	
						logUnload(event, tour);		
					}
			}
		}
	}

	private void logUnload(LSPTourEndEvent event, Tour tour){
		ShipmentUtils.LoggedShipmentUnloadBuilder builder  =  ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
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

	private void logTransport(LSPTourEndEvent event, Tour tour){
		String idString = resource.getId() + "" + solutionElement.getId() + "" + "TRANSPORT";
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		ShipmentPlanElement abstractPlanElement = lspShipment.getLog().getPlanElements().get(id);
		if(abstractPlanElement instanceof ShipmentLeg) {
			ShipmentLeg transport = (ShipmentLeg) abstractPlanElement;
			//Auskommentiert, im Rahmen des reducing-public-footprint-Prozesses. Kein Test reagiert drauf. Was "sollte" hier geschehen? KMT(&kai) Jun'20
//			transport.setEndTime(event.getTime());
//			transport.setToLinkId(tour.getEndLinkId());
		}		
	}

	private double getTotalUnloadingTime(Tour tour){
		double totalTime = 0;
		for(TourElement element : tour.getTourElements()){
			if(element instanceof ServiceActivity){
				ServiceActivity serviceActivity = (ServiceActivity) element;
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


	@Override public void setEmbeddingContainer( LSPShipment pointer ){
		this.lspShipment = pointer;
	}
//	@Override public Collection<EventHandler> getEventHandlers(){
//		return this.eventHandlers;
//	}
//	@Override public void reset(){
//		throw new RuntimeException( "not implemented" );
//	}
	@Override public void notifyAfterMobsim( AfterMobsimEvent event ){
		throw new RuntimeException( "not implemented" );
	}
}

	
	
	
	

