/*
 *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       :  (C) 2022 by the members listed in the COPYING,       *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * ***********************************************************************
 */

package org.matsim.freight.logistics.resourceImplementations;

import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.events.CarrierServiceEndEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceEndEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * An {@link LSPSimulationTracker} (and {@link CarrierServiceEndEventHandler}) that, on the end of a
 * carrier's service, logs the {@link LspShipment}'s load and transport legs into the shipment's log.
 *
 * <p>It is attached to each {@link LspShipment} as a simulation tracker during scheduling.
 */
/*package-private*/ class CollectionJobEventHandler extends AbstractLspShipmentEventHandler implements CarrierServiceEndEventHandler {

	private final CarrierService carrierService;

	public CollectionJobEventHandler(CarrierService carrierService, LspShipment lspShipment, LogisticChainElement element, LSPCarrierResource resource) {
		super(lspShipment, element, resource);
		this.carrierService = carrierService;
	}

	@Override
	public void handleEvent(CarrierServiceEndEvent event) {
		if (event.getServiceId() == carrierService.getId() && isCarrierOfResource(event.getCarrierId())) {
			logLoad(event.getCarrierId(), event.getLinkId(), event.getTime() - event.getServiceDuration(), event.getTime());
			logTransportStart(event.getCarrierId(), event.getLinkId(), null, event.getTime());
		}
	}

	public CarrierService getCarrierService() {
		return carrierService;
	}

	public LogisticChainElement getElement() {
		return getLogisticChainElement();
	}
}
