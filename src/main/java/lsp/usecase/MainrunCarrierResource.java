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

import java.util.Collection;

import lsp.LSPDataObject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;

import lsp.LogisticsSolutionElement;
import lsp.LSPCarrierResource;
import lsp.LSPResource;

/*package-private*/ class MainRunCarrierResource extends LSPDataObject<LSPResource> implements LSPCarrierResource {

	private final Carrier carrier;
	private final Id<Link> fromLinkId;
	private final Id<Link> toLinkId;
	private final Collection<LogisticsSolutionElement> clientElements;
	private final MainRunCarrierScheduler mainRunScheduler;
	private final Network network;

	MainRunCarrierResource(UsecaseUtils.MainRunCarrierAdapterBuilder builder) {
		super(builder.getId());
		this.carrier = builder.getCarrier();
		this.fromLinkId = builder.getFromLinkId();
		this.toLinkId = builder.getToLinkId();
		this.clientElements = builder.getClientElements();
		this.mainRunScheduler = builder.getMainRunScheduler();
		this.network = builder.getNetwork();
	}

	@Override
	public Id<Link> getStartLinkId() {
		return fromLinkId;
	}

	@Override
	public Id<Link> getEndLinkId() {
		return toLinkId;
	}

	@Override
	public Collection<LogisticsSolutionElement> getClientElements() {
		return clientElements;
	}

	@Override
	public void schedule(int bufferTime) {
		mainRunScheduler.scheduleShipments(this, bufferTime);
	}

	public Carrier getCarrier() {
		return carrier;
	}

	public Network getNetwork() {
		return network;
	}

}
