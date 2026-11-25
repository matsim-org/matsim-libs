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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentLeg;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

import static org.matsim.freight.logistics.shipment.LspShipmentPlanElement.ElementType.TRANSPORT;

/**
 * Common base of the event handlers that log the plan elements (load, transport, unload) of one {@link LspShipment} at
 * one {@link LogisticChainElement} of a carrier resource into the shipment log of the shipment.
 */
/*package-private*/ abstract class AbstractLspShipmentEventHandler implements LSPSimulationTracker<LspShipment> {

	private final LogisticChainElement logisticChainElement;
	private final LSPCarrierResource resource;
	private LspShipment lspShipment;

	AbstractLspShipmentEventHandler(LspShipment lspShipment, LogisticChainElement logisticChainElement, LSPCarrierResource resource) {
		this.lspShipment = lspShipment;
		this.logisticChainElement = logisticChainElement;
		this.resource = resource;
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public final void setEmbeddingContainer(LspShipment pointer) {
		this.lspShipment = pointer;
	}

	public final LspShipment getLspShipment() {
		return lspShipment;
	}

	public final LogisticChainElement getLogisticChainElement() {
		return logisticChainElement;
	}

	public final Id<LSPResource> getResourceId() {
		return resource.getId();
	}

	final LSPCarrierResource getCarrierResource() {
		return resource;
	}

	/**
	 * @return {@code true} if the event refers to the carrier of the resource of this handler.
	 */
	final boolean isCarrierOfResource(Id<Carrier> carrierId) {
		return carrierId == resource.getCarrier().getId();
	}

	final void logLoad(Id<Carrier> carrierId, Id<Link> linkId, double startTime, double endTime) {
		LspShipmentUtils.LoggedShipmentLoadBuilder builder = LspShipmentUtils.LoggedShipmentLoadBuilder.newInstance();
		builder.setCarrierId(carrierId);
		builder.setLinkId(linkId);
		builder.setLogisticsChainElement(logisticChainElement);
		builder.setResourceId(resource.getId());
		builder.setStartTime(startTime);
		builder.setEndTime(endTime);
		addToShipmentLog(builder.build());
	}

	/**
	 * Logs the start of a transport. The end time (and possibly the end link) is set later by {@link #logTransportEnd}.
	 *
	 * @param toLinkId the end link of the transport, or {@code null} if it is not known yet
	 */
	final void logTransportStart(Id<Carrier> carrierId, Id<Link> fromLinkId, Id<Link> toLinkId, double startTime) {
		LspShipmentUtils.LoggedShipmentTransportBuilder builder = LspShipmentUtils.LoggedShipmentTransportBuilder.newInstance();
		builder.setCarrierId(carrierId);
		builder.setFromLinkId(fromLinkId);
		builder.setToLinkId(toLinkId);
		builder.setLogisticChainElement(logisticChainElement);
		builder.setResourceId(resource.getId());
		builder.setStartTime(startTime);
		addToShipmentLog(builder.build());
	}

	/**
	 * Sets the end of the transport that was logged by {@link #logTransportStart} for the resource and logistic chain
	 * element of this handler. Does nothing if no such transport is in the shipment log.
	 *
	 * @param toLinkId the end link of the transport, or {@code null} to leave it unchanged
	 */
	final void logTransportEnd(double endTime, Id<Link> toLinkId) {
		LspShipmentPlanElement planElement = lspShipment.getShipmentLog().getPlanElements()
			.get(LspShipmentUtils.createPlanElementId(resource.getId(), logisticChainElement.getId(), TRANSPORT));
		if (planElement instanceof LspShipmentLeg transport) {
			transport.setEndTime(endTime);
			if (toLinkId != null) {
				transport.setToLinkId(toLinkId);
			}
		}
	}

	/**
	 * @param linkId the link of the unloading, or {@code null} if it is not known
	 */
	final void logUnload(Id<Carrier> carrierId, Id<Link> linkId, double startTime, double endTime) {
		LspShipmentUtils.LoggedShipmentUnloadBuilder builder = LspShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
		builder.setCarrierId(carrierId);
		builder.setLinkId(linkId);
		builder.setLogisticChainElement(logisticChainElement);
		builder.setResourceId(resource.getId());
		builder.setStartTime(startTime);
		builder.setEndTime(endTime);
		addToShipmentLog(builder.build());
	}

	private void addToShipmentLog(LspShipmentPlanElement planElement) {
		lspShipment.getShipmentLog().addPlanElement(LspShipmentUtils.createPlanElementId(planElement), planElement);
	}
}
