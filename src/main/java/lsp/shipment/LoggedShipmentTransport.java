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

package lsp.shipment;

import lsp.LSPResource;
import lsp.LogisticChainElement;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;

final class LoggedShipmentTransport implements ShipmentLeg {

	private final double startTime;
	private final LogisticChainElement element;
	private final Id<LSPResource> resourceId;
	private final Id<Link> fromLinkId;
	private double endTime;
	private Id<Link> toLinkId;

	LoggedShipmentTransport(ShipmentUtils.LoggedShipmentTransportBuilder builder) {
		this.startTime = builder.getStartTime();
		this.element = builder.getElement();
		this.resourceId = builder.getResourceId();
		this.fromLinkId = builder.getFromLinkId();
		this.toLinkId = builder.getToLinkId();
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
	public String getElementType() {
		return "TRANSPORT";
	}

	@Override
	public double getStartTime() {
		return startTime;
	}

	@Override
	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}


	public Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	@Override
	public CarrierService getCarrierService() {
		throw new RuntimeException("not implemented");
	}


	public Id<Link> getToLinkId() {
		return toLinkId;
	}

	public void setToLinkId(Id<Link> toLinkId) {
		this.toLinkId = toLinkId;
	}

	@Override
	public Id<Carrier> getCarrierId() {
		throw new RuntimeException("not implemented");
	}


}
