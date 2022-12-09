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

final class ScheduledShipmentTransport implements ShipmentLeg {

	private final double startTime;
	private final double endTime;
	private final LogisticChainElement element;
	private final Id<LSPResource> resourceId;
	private final Id<Link> fromLinkId;

	ScheduledShipmentTransport(ShipmentUtils.ScheduledShipmentTransportBuilder builder) {
		this.startTime = builder.startTime;
		this.endTime = builder.endTime;
		this.element = builder.element;
		this.resourceId = builder.resourceId;
		this.fromLinkId = builder.fromLinkId;
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

	@Override
	public LogisticChainElement getLogisticChainElement() {
		return element;
	}

	@Override
	public Id<LSPResource> getResourceId() {
		return resourceId;
	}

	@Override public Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	@Override public void setEndTime( double time ){
		throw new RuntimeException( "not implemented" );
	}

}
