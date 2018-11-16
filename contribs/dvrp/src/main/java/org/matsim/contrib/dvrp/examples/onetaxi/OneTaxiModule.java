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
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OneTaxiModule extends AbstractModule {
	private final String taxisFile;

	public OneTaxiModule(String taxisFile) {
		this.taxisFile = taxisFile;
	}

	@Override
	public void install() {
		addRoutingModuleBinding(TransportMode.taxi).toInstance(new DynRoutingModule(TransportMode.taxi));
		bind(DvrpModes.key(Fleet.class, TransportMode.taxi)).toProvider(new FleetProvider(taxisFile))
				.asEagerSingleton();

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				DvrpMode dvrpMode = DvrpModes.mode(TransportMode.taxi);
				// optimizer that dispatches taxis
				bind(VrpOptimizer.class).annotatedWith(dvrpMode).to(OneTaxiOptimizer.class).asEagerSingleton();
				// converts departures of the "taxi" mode into taxi requests
				bind(PassengerRequestCreator.class).annotatedWith(dvrpMode)
						.to(OneTaxiRequestCreator.class)
						.asEagerSingleton();
				// converts scheduled tasks into simulated actions (legs and activities)
				bind(VrpAgentLogic.DynActionCreator.class).annotatedWith(dvrpMode)
						.to(OneTaxiActionCreator.class)
						.asEagerSingleton();
			}
		});
	}
}
