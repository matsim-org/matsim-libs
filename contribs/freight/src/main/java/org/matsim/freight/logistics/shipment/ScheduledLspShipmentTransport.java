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

package org.matsim.freight.logistics.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.logistics.LSPConstants;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LogisticChainElement;

final class ScheduledLspShipmentTransport implements LspShipmentLeg {

	private final double startTime;
	private final double endTime;
	private final LogisticChainElement element;
	private final Id<LSPResource> resourceId;
	private final Id<Carrier> carrierId;
	private final Id<Link> fromLinkId;
	private final Id<Link> toLinkId;
	private final CarrierService carrierService;
	private final CarrierShipment carrierShipment; //TODO: Put CarrierShipment and CarrieTask behind one interface and use that here (CarrierTask...)

	ScheduledLspShipmentTransport(LspShipmentUtils.ScheduledShipmentTransportBuilder builder) {
		this.startTime = builder.startTime;
		this.endTime = builder.endTime;
		this.element = builder.element;
		this.resourceId = builder.resourceId;
		this.carrierId = builder.carrierId;
		this.fromLinkId = builder.fromLinkId;
		this.toLinkId = builder.toLinkId;
		this.carrierService = builder.carrierService;
		this.carrierShipment = builder.carrierShipment;
	}

	/**
	 * @deprecated //see bloch item 23: Prefer class hierarchies to tagged classes.
	 * Mixing class tagging and class hierarchies is a bad idea.
	 * Getting the type is ok for writing it somewhere, but do NOT use it for specifying the type within a decision logic!
	 * So maybe better use something like getActivityType() -- analogous to e.g. PersonEntersVehicleEvent.class) kmt/kn jan'25
	 */
	@Override
	@Deprecated
	public String getElementType() {
		return LSPConstants.TRANSPORT;
	}

	@Override
	public double getStartTime() {
		return startTime;
	}

	@Override
	public double getEndTime() {
		return endTime;
	}

	@Override
	public void setEndTime(double time) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public LogisticChainElement getLogisticChainElement() {
		return element;
	}

	@Override
	public Id<LSPResource> getResourceId() {
		return resourceId;
	}

	@Override
	public Id<Link> getToLinkId() {
		return toLinkId;
	}

	@Override
	public void setToLinkId(Id<Link> endLinkId) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public Id<Carrier> getCarrierId() {
		return carrierId;
	}

	@Override
	public Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	@Override
	public CarrierService getCarrierService() {
		return carrierService;
	}

	public CarrierShipment getCarrierShipment() {
		return carrierShipment;
	}
}
