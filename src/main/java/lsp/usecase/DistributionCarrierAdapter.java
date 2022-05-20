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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.LSPInfo;
import lsp.LogisticsSolutionElement;
import lsp.LSPCarrierResource;
import lsp.LSPResource;
import lsp.controler.LSPSimulationTracker;

/*package-private*/ class DistributionCarrierAdapter implements LSPCarrierResource {

	private final Id<LSPResource>id;
	private final Carrier carrier;
	private final ArrayList<LogisticsSolutionElement> clientElements;
	private final DistributionCarrierScheduler distributionHandler;
	private final Network network;
	private final Collection<EventHandler> eventHandlers;
	private final Collection<LSPInfo> infos;
	private final Collection<LSPSimulationTracker> trackers;

	DistributionCarrierAdapter(UsecaseUtils.DistributionCarrierAdapterBuilder builder){
			this.id = builder.id;
		Id<Link> locationLinkId = builder.locationLinkId;
			this.distributionHandler = builder.distributionHandler;
			this.clientElements = builder.clientElements;
			this.carrier = builder.carrier;
			this.network = builder.network;
			this.eventHandlers = new ArrayList<>();
			this.infos = new ArrayList<>();
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
	public Id<LSPResource> getId() {
		return id;
	}
	
	@Override
	public Collection<LogisticsSolutionElement> getClientElements() {
		return clientElements;
	}

	@Override
	public void schedule(int bufferTime) {
		distributionHandler.scheduleShipments(this, bufferTime);
		
	}

	public Network getNetwork(){
		return network;
	}
	
	public Carrier getCarrier(){
		return carrier;
	}

	@Override public Collection <EventHandler> getEventHandlers(){
		return eventHandlers;
	}

	@Override
	public Collection<LSPInfo> getInfos() {
		return infos;
	}
	
	@Override
	public void addSimulationTracker( LSPSimulationTracker tracker ) {
		this.trackers.add(tracker);
		this.eventHandlers.addAll(tracker.getEventHandlers());
		this.infos.addAll(tracker.getInfos());	
	}

	@Override
	public Collection<LSPSimulationTracker> getSimulationTrackers() {
		return trackers;
	}

//	@Override
//	public void setEventsManager(EventsManager eventsManager) {
//	}

}
