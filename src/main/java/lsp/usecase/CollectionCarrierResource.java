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

import lsp.LSPCarrierResource;
import lsp.LSPResource;
import lsp.LogisticsSolutionElement;
import lsp.controler.LSPSimulationTracker;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*package-private*/ class CollectionCarrierResource implements LSPCarrierResource {

	private final Attributes attributes = new Attributes();
	private final Id<LSPResource>id;
	private final Carrier carrier;
	private final List<LogisticsSolutionElement> clientElements;
	private final CollectionCarrierScheduler collectionScheduler;
	private final Network network;
	private final Collection<EventHandler> eventHandlers;
	private final Collection<LSPSimulationTracker<LSPResource>> trackers;

	CollectionCarrierResource( UsecaseUtils.CollectionCarrierAdapterBuilder builder ){
		this.id = builder.id;
		this.collectionScheduler = builder.collectionScheduler;
		this.clientElements = builder.clientElements;
		this.carrier = builder.carrier;
		this.network = builder.network;
		this.eventHandlers = new ArrayList<>();
		this.trackers = new ArrayList<>();
	}
	
	
//	@Override
//	public Class<? extends Carrier> getClassOfResource() {
//		return carrier.getClass();
//	}
//
	@Override
	public Id<Link> getStartLinkId() {
		Id<Link> depotLinkId = null;
		for(CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()){
			if(depotLinkId == null || depotLinkId == vehicle.getLocation()){
				depotLinkId = vehicle.getLocation();
			}
			
		}
		
		return depotLinkId;
		
	}

	@Override
	public Id<Link> getEndLinkId() {
		Id<Link> depotLinkId = null;
		for(CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()){
			if(depotLinkId == null || depotLinkId == vehicle.getLocation()){
				depotLinkId = vehicle.getLocation();
			}
			
		}
		
		return depotLinkId;
	
	}
		
	
	@Override
	public Collection<LogisticsSolutionElement> getClientElements() {
		return clientElements;
	}

	@Override
	public Id<LSPResource> getId() {
		return id;
	}

	@Override
	public void schedule(int bufferTime) {
		collectionScheduler.scheduleShipments(this, bufferTime);	
	}

	public Carrier getCarrier(){
		return carrier;
	}

	public Network getNetwork(){
		return network;
	}

//	public Collection <EventHandler> getSimulationTrackers(){
//		return eventHandlers;
//	}

	@Override
	public void addSimulationTracker( LSPSimulationTracker<LSPResource> tracker ) {
		this.trackers.add(tracker);
		this.eventHandlers.addAll(tracker.getEventHandlers() );
		this.eventHandlers.add( tracker );
//		this.infos.addAll(tracker.getAttributes() );
//		for( Map.Entry<String, Object> entry : tracker.getAttributes().getAsMap().entrySet() ){
//			this.attributes.putAttribute( entry.getKey(), entry.getValue() );
//		}
	}


	@Override
	public Collection<LSPSimulationTracker<LSPResource>> getSimulationTrackers() {
		return Collections.unmodifiableCollection( trackers );
	}
	@Override public void clearSimulationTrackers(){
		this.trackers.clear();
	}
	@Override public Attributes getAttributes(){
		return attributes;
	}

//	@Override
//	public void setEventsManager(EventsManager eventsManager) {
//	}

}
