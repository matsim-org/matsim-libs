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
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LogisticChainElement;

/**
 * Common base of the {@link LspShipmentPlanElement}s that take place at a single link: load, unload, and handling,
 * both scheduled and logged. Transports are {@link LspShipmentLeg}s and do not extend this class.
 */
abstract class AbstractLspShipmentActivity implements LspShipmentPlanElement {

	private final double startTime;
	private final double endTime;
	private final LogisticChainElement element;
	private final Id<LSPResource> resourceId;
	private final Id<Link> linkId;

	AbstractLspShipmentActivity(double startTime, double endTime, LogisticChainElement element, Id<LSPResource> resourceId, Id<Link> linkId) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.element = element;
		this.resourceId = resourceId;
		this.linkId = linkId;
	}

	@Override
	public final double getStartTime() {
		return startTime;
	}

	@Override
	public final double getEndTime() {
		return endTime;
	}

	@Override
	public final LogisticChainElement getLogisticChainElement() {
		return element;
	}

	@Override
	public final Id<LSPResource> getResourceId() {
		return resourceId;
	}

	@Override
	public final Id<Link> getLinkId() {
		return linkId;
	}
}
