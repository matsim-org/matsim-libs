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
import lsp.LSPDataObject;
import lsp.LSPResource;
import lsp.LogisticsSolutionElement;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;

import java.util.Collection;

/*package-private*/ class DistributionCarrierResource extends LSPDataObject<LSPResource> implements LSPCarrierResource {

	private final Carrier carrier;
	private final Collection<LogisticsSolutionElement> clientElements;
	private final DistributionCarrierScheduler distributionHandler;
	private final Network network;

	DistributionCarrierResource(UsecaseUtils.DistributionCarrierResourceBuilder builder) {
		super(builder.id);
		this.distributionHandler = builder.distributionHandler;
		this.clientElements = builder.clientElements;
		this.carrier = builder.carrier;
		this.network = builder.network;
	}

	@Override
	public Id<Link> getStartLinkId() {
		Id<Link> depotLinkId = null;
		for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
			if (depotLinkId == null || depotLinkId == vehicle.getLinkId()) {
				depotLinkId = vehicle.getLinkId();
			}
		}

		return depotLinkId;
	}

	@Override
	public Id<Link> getEndLinkId() {
		Id<Link> depotLinkId = null;
		for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
			if (depotLinkId == null || depotLinkId == vehicle.getLinkId()) {
				depotLinkId = vehicle.getLinkId();
			}
		}

		return depotLinkId;
	}

	@Override
	public Collection<LogisticsSolutionElement> getClientElements() {
		return clientElements;
	}

	@Override
	public void schedule(int bufferTime) {
		distributionHandler.scheduleShipments(this, bufferTime);
	}

	public Network getNetwork() {
		return network;
	}

	public Carrier getCarrier() {
		return carrier;
	}

}
