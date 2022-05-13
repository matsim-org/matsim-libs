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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LogisticsSolutionElement;
import lsp.LSPResource;



/*package*/ class LoggedShipmentHandle implements ShipmentPlanElement {

	private final double startTime;
	private final double endTime;
	private final LogisticsSolutionElement element;
	private final Id<LSPResource> resourceId;
	private final Id<Link> linkId;

	LoggedShipmentHandle(ShipmentUtils.LoggedShipmentHandleBuilder builder){
		this.startTime = builder.getStartTime();
		this.endTime = builder.getEndTime();
		this.element = builder.getElement();
		this.resourceId = builder.getResourceId();
		this.linkId = builder.getLinkId();
	}
	
	@Override
	public LogisticsSolutionElement getSolutionElement() {
		return element;
	}

	@Override
	public Id<LSPResource> getResourceId() {
		return resourceId;
	}

	@Override
	public String getElementType() {
		String type = "HANDLE";
		return type;
	}

	@Override
	public double getStartTime() {
		return startTime;
	}

	@Override
	public double getEndTime() {
		return endTime;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

}
