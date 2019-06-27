/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.fleet;

import java.net.URL;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.run.QSimScopeObjectListenerModule;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Michal Maciejewski (michalm)
 */
public class FleetModule extends AbstractDvrpModeModule {
	private final URL fleetSpecificationUrl;
	private final boolean updateVehicleStartLinkToLastLink;

	public FleetModule(String mode, URL fleetSpecificationUrl) {
		this(mode, fleetSpecificationUrl, false);
	}

	public FleetModule(String mode, URL fleetSpecificationUrl, boolean updateVehicleStartLinkToLastLink) {
		super(mode);
		this.fleetSpecificationUrl = fleetSpecificationUrl;
		this.updateVehicleStartLinkToLastLink = updateVehicleStartLinkToLastLink;
	}

	@Override
	public void install() {
		bindModal(FleetSpecification.class).toProvider(() -> {
			FleetSpecification fleetSpecification = new FleetSpecificationImpl();
			new FleetReader(fleetSpecification).parse(fleetSpecificationUrl);
			return fleetSpecification;
		}).asEagerSingleton();

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(Fleet.class).toProvider(new ModalProviders.AbstractProvider<Fleet>(getMode()) {
					@Inject
					@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
					private Network network;

					@Override
					public Fleet get() {
						FleetSpecification fleetSpecification = getModalInstance(FleetSpecification.class);
						return Fleets.createDefaultFleet(fleetSpecification, network.getLinks()::get);
					}
				}).asEagerSingleton();
			}
		});

		if (updateVehicleStartLinkToLastLink) {
			install(QSimScopeObjectListenerModule.builder(VehicleStartLinkToLastLinkUpdater.class)
					.mode(getMode())
					.objectClass(Fleet.class)
					.listenerCreator(
							getter -> new VehicleStartLinkToLastLinkUpdater(getter.getModal(FleetSpecification.class)))
					.build());
		}
	}
}
