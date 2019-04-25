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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.fleet.FleetModule;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.dynagent.run.DynRoutingModule;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OneTaxiModule extends AbstractDvrpModeModule {
	private final String taxisFile;

	public OneTaxiModule(String taxisFile) {
		super(TransportMode.taxi);
		this.taxisFile = taxisFile;
	}

	@Override
	public void install() {
		addRoutingModuleBinding(getMode()).toInstance(new DynRoutingModule(getMode()));
		install(new FleetModule(getMode(), taxisFile));
		bindModal(PassengerRequestValidator.class).to(DefaultPassengerRequestValidator.class);

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				install(new VrpAgentSourceQSimModule(getMode()));
				install(new PassengerEngineQSimModule(getMode()));

				// optimizer that dispatches taxis
				bindModal(VrpOptimizer.class).to(OneTaxiOptimizer.class).asEagerSingleton();
				// converts departures of the "taxi" mode into taxi requests
				bindModal(PassengerRequestCreator.class).to(OneTaxiRequest.OneTaxiRequestCreator.class)
						.asEagerSingleton();
				// converts scheduled tasks into simulated actions (legs and activities)
				bindModal(VrpAgentLogic.DynActionCreator.class).to(OneTaxiActionCreator.class).asEagerSingleton();
			}
		});
	}
}
