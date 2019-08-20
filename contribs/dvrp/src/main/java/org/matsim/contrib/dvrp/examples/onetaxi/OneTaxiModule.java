/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.examples.onetaxi;

import java.net.URL;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.fleet.FleetModule;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.dynagent.run.DynRoutingModule;

import com.google.inject.Singleton;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OneTaxiModule extends AbstractDvrpModeModule {
	private final URL fleetSpecificationUrl;

	public OneTaxiModule(URL fleetSpecificationUrl) {
		super(TransportMode.taxi);
		this.fleetSpecificationUrl = fleetSpecificationUrl;
	}

	@Override
	public void install() {
		DvrpModes.registerDvrpMode(binder(), getMode());
		install(DvrpRoutingNetworkProvider.createDvrpModeRoutingNetworkModule(getMode(), false));
		addRoutingModuleBinding(getMode()).toInstance(new DynRoutingModule(getMode()));

		install(new FleetModule(getMode(), fleetSpecificationUrl));
		bindModal(PassengerRequestValidator.class).to(DefaultPassengerRequestValidator.class);

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				install(new VrpAgentSourceQSimModule(getMode()));
				install(new PassengerEngineQSimModule(getMode()));

				// optimizer that dispatches taxis
				bindModal(VrpOptimizer.class).to(OneTaxiOptimizer.class).in(Singleton.class);

				// converts departures of the "taxi" mode into taxi requests
				bindModal(PassengerRequestCreator.class).to(OneTaxiRequest.OneTaxiRequestCreator.class)
						.in(Singleton.class);

				// converts scheduled tasks into simulated actions (legs and activities)
				bindModal(VrpAgentLogic.DynActionCreator.class).to(OneTaxiActionCreator.class).in(Singleton.class);
			}
		});
	}
}
