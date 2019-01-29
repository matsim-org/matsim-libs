/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.vsp.edvrp.etaxi.run;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.ev.ev.data.ChargingInfrastructure;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vsp.edvrp.etaxi.ETaxiActionCreator;
import org.matsim.vsp.edvrp.etaxi.ETaxiScheduler;
import org.matsim.vsp.edvrp.etaxi.optimizer.ETaxiOptimizerProvider;

public class ETaxiModeQSimModule extends AbstractDvrpModeQSimModule {

	private final TaxiConfigGroup taxiCfg;

	public ETaxiModeQSimModule(TaxiConfigGroup taxiCfg) {
		super(taxiCfg.getMode());
		this.taxiCfg = taxiCfg;
	}

	@Override
	protected void configureQSim() {
		install(new VrpAgentSourceQSimModule(getMode()));
		install(new PassengerEngineQSimModule(getMode()));

		addModalComponent(TaxiOptimizer.class, new ModalProviders.AbstractProvider<TaxiOptimizer>(taxiCfg.getMode()) {
			@Inject
			@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
			private Network network;

			@Inject
			private MobsimTimer timer;

			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;

			@Inject
			private EventsManager events;

			@Inject
			private ChargingInfrastructure chargingInfrastructure;

			@Override
			public TaxiOptimizer get() {
				Fleet fleet = getModalInstance(Fleet.class);
				ETaxiScheduler eTaxiScheduler = getModalInstance(ETaxiScheduler.class);
				TravelDisutility travelDisutility = getModalInstance(
						TravelDisutilityFactory.class).createTravelDisutility(travelTime);
				return new ETaxiOptimizerProvider(taxiCfg, fleet, network, timer, travelTime, travelDisutility,
						eTaxiScheduler, chargingInfrastructure).get();
			}
		});

		bindModal(ETaxiScheduler.class).toProvider(
				new ModalProviders.AbstractProvider<ETaxiScheduler>(taxiCfg.getMode()) {
					@Inject
					@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
					private Network network;

					@Inject
					private MobsimTimer timer;

					@Inject
					@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
					private TravelTime travelTime;

					@Override
					public ETaxiScheduler get() {
						Fleet fleet = getModalInstance(Fleet.class);
						TravelDisutility travelDisutility = getModalInstance(
								TravelDisutilityFactory.class).createTravelDisutility(travelTime);
						return new ETaxiScheduler(taxiCfg, fleet, network, timer, travelTime, travelDisutility);
					}
				}).asEagerSingleton();

		bindModal(DynActionCreator.class).toProvider(
				new ModalProviders.AbstractProvider<ETaxiActionCreator>(taxiCfg.getMode()) {
					@Inject
					private MobsimTimer timer;

					@Inject
					private DvrpConfigGroup dvrpCfg;

					@Override
					public ETaxiActionCreator get() {
						PassengerEngine passengerEngine = getModalInstance(PassengerEngine.class);
						return new ETaxiActionCreator(new TaxiActionCreator(passengerEngine, taxiCfg, timer, dvrpCfg));
					}
				}).asEagerSingleton();

		bindModal(PassengerRequestCreator.class).toProvider(modalProvider(
				getter -> new TaxiRequestCreator(getMode(), getter.getModal(SubmittedTaxiRequestsCollector.class))))
				.asEagerSingleton();

		bindModal(VrpOptimizer.class).to(modalKey(TaxiOptimizer.class));
	}
}
