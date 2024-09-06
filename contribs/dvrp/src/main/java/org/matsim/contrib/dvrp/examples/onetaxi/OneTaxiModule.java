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
import org.matsim.contrib.dvrp.passenger.AdvanceRequestProvider;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule.PassengerEngineType;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingModule;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingNetworkModule;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyDijkstraFactory;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OneTaxiModule extends AbstractDvrpModeModule {
	private final URL fleetSpecificationUrl;
	private final PassengerEngineType passengerEngineType;

	public OneTaxiModule(URL fleetSpecificationUrl, PassengerEngineType passengerEngineType) {
		super(TransportMode.taxi);
		this.fleetSpecificationUrl = fleetSpecificationUrl;
		this.passengerEngineType = passengerEngineType;
	}

	@Override
	public void install() {
		DvrpModes.registerDvrpMode(binder(), getMode());
		install(new DvrpModeRoutingNetworkModule(getMode(), false));

		install(new DvrpModeRoutingModule(getMode(), new SpeedyDijkstraFactory()));
		bindModal(TravelTime.class).to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
		bindModal(TravelDisutilityFactory.class).toInstance(TimeAsTravelDisutility::new);

		install(new FleetModule(getMode(), fleetSpecificationUrl));
		bindModal(PassengerRequestValidator.class).to(DefaultPassengerRequestValidator.class);

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				install(new VrpAgentSourceQSimModule(getMode()));
				install(new PassengerEngineQSimModule(getMode(), passengerEngineType));

				// optimizer that dispatches taxis
				bindModal(VrpOptimizer.class).to(OneTaxiOptimizer.class).in(Singleton.class);

				// converts departures of the "taxi" mode into taxi requests
				bindModal(PassengerRequestCreator.class).to(OneTaxiRequest.OneTaxiRequestCreator.class)
						.in(Singleton.class);

				// converts scheduled tasks into simulated actions (legs and activities)
				bindModal(VrpAgentLogic.DynActionCreator.class).to(OneTaxiActionCreator.class).in(Singleton.class);
				
				// no advance request
				bindModal(AdvanceRequestProvider.class).toInstance(AdvanceRequestProvider.NONE);
			}
		});
	}
}
