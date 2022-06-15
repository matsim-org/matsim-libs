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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import lsp.controler.LSPSimulationTracker;
import lsp.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

import org.matsim.contrib.freight.events.LSPTourEndEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPTourEndEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.LSPResource;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;

/*package-private*/  class TranshipmentHubTourEndEventHandler implements LSPSimulationTracker<LSPResource>, LSPTourEndEventHandler {

	private final Collection<? extends EventHandler> eventHandlers = new ArrayList<>();
	@Override public void setEmbeddingContainer( LSPResource pointer ){
		throw new RuntimeException( "not implemented" );
	}
//	@Override public void reset(){
//	}
//	@Override public Collection<? extends EventHandler> getEventHandlers(){
//		return this.eventHandlers;
//	}
	@Override public void notifyAfterMobsim( AfterMobsimEvent event ){
	}
	static class TransshipmentHubEventHandlerPair {
		public final LSPShipment shipment;
		public final LogisticsSolutionElement element;
				
		public TransshipmentHubEventHandlerPair(LSPShipment shipment, LogisticsSolutionElement element){
			this.shipment = shipment;
			this.element = element;
		}	
	}
	
	
	private final HashMap<CarrierService, TransshipmentHubEventHandlerPair> servicesWaitedFor;
	private final TransshipmentHub transshipmentHub;
	private final Id<LSPResource> resourceId;
	private final Id<Link> linkId;
	
	TranshipmentHubTourEndEventHandler(TransshipmentHub transshipmentHub){
		this.transshipmentHub = transshipmentHub;
		this.linkId = transshipmentHub.getEndLinkId();
		this.resourceId = transshipmentHub.getId();
		this.servicesWaitedFor = new HashMap<>();
	}
	
	@Override
	public void reset(int iteration) {
		servicesWaitedFor.clear();
	}

	public void addShipment(LSPShipment shipment, LogisticsSolutionElement solutionElement){
		TransshipmentHubEventHandlerPair pair = new TransshipmentHubEventHandlerPair(shipment, solutionElement);
		
		for(ShipmentPlanElement planElement: shipment.getShipmentPlan().getPlanElements().values()){
			if(planElement instanceof ShipmentLeg){
				ShipmentLeg transport = (ShipmentLeg) planElement;
				if(transport.getSolutionElement().getNextElement() == solutionElement){
					servicesWaitedFor.put(transport.getCarrierService(), pair);
				}
			}
		}
	}	
	
	@Override
	public void handleEvent(LSPTourEndEvent event) {
		if((event.getTour().getEndLinkId() == this.linkId) && (shipmentsOfTourEndInPoint(event.getTour()))){
			
			for(TourElement tourElement : event.getTour().getTourElements()){
				if(tourElement instanceof ServiceActivity){
					ServiceActivity serviceActivity = (ServiceActivity) tourElement;
					if(serviceActivity.getLocation() == transshipmentHub.getStartLinkId()
							&& allServicesAreInOnePoint(event.getTour())
							&& (event.getTour().getStartLinkId() != transshipmentHub.getStartLinkId())) {
						logReloadAfterMainRun(serviceActivity.getService(), event);
					}
					else {
						logReloadAfterCollection(serviceActivity.getService(), event);
					}
				}
				
			}
		}
		
		

	}

	private boolean shipmentsOfTourEndInPoint(Tour tour){
		boolean shipmentsEndInPoint = true;
		for(TourElement tourElement : tour.getTourElements()){
			if(tourElement instanceof ServiceActivity){
				ServiceActivity serviceActivity = (ServiceActivity) tourElement;
				if(!servicesWaitedFor.containsKey(serviceActivity.getService())){
					return false;				
				}
			}
		}
		return shipmentsEndInPoint;
	}

	private void logReloadAfterCollection(CarrierService carrierService, LSPTourEndEvent event){
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
		if(!lspShipment.getLog().getPlanElements().containsKey(loadId)) {
			lspShipment.getLog().addPlanElement(loadId, loggedShipmentHandle);
		}	
	}
	
	private double getUnloadEndTime(Tour tour){
		double unloadEndTime = 0;
		for(TourElement element: tour.getTourElements()){
			if(element instanceof Tour.ServiceActivity){
				Tour.ServiceActivity serviceActivity = (Tour.ServiceActivity) element;
				unloadEndTime = unloadEndTime + serviceActivity.getDuration();
			}
		}
	
		
		return unloadEndTime;
	}

	private void logReloadAfterMainRun(CarrierService carrierService, LSPTourEndEvent event){
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
		if(!lspShipment.getLog().getPlanElements().containsKey(loadId)) {
			lspShipment.getLog().addPlanElement(loadId, handle);
		}	
	}

	private boolean allServicesAreInOnePoint(Tour tour) {
		for(TourElement element : tour.getTourElements()) {
			if(element instanceof ServiceActivity) {
				ServiceActivity activity = (ServiceActivity) element;
				if(activity.getLocation() != tour.getEndLinkId()) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	public HashMap<CarrierService, TransshipmentHubEventHandlerPair> getServicesWaitedFor() {
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
	
	
	
}
