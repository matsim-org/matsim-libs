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

package org.matsim.contrib.etaxi.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.etaxi.ETaxiActionCreator;
import org.matsim.contrib.etaxi.ETaxiScheduler;
import org.matsim.contrib.etaxi.optimizer.ETaxiOptimizerProvider;
import org.matsim.contrib.etaxi.util.ETaxiStayTaskEndTimeCalculator;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructures;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

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

		addModalComponent(TaxiOptimizer.class, new ModalProviders.AbstractProvider<>(taxiCfg.getMode()) {
			@Inject
			private MobsimTimer timer;

			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;

			@Inject
			private EventsManager events;

			@Override
			public TaxiOptimizer get() {
				var fleet = getModalInstance(Fleet.class);
				var network = getModalInstance(Network.class);
				var eTaxiScheduler = getModalInstance(ETaxiScheduler.class);
				var travelDisutility = getModalInstance(TravelDisutilityFactory.class).createTravelDisutility(
						travelTime);
				var chargingInfrastructure = getModalInstance(ChargingInfrastructure.class);
				var scheduleTimingUpdater = getModalInstance(ScheduleTimingUpdater.class);
				return new ETaxiOptimizerProvider(events, taxiCfg, fleet, network, timer, travelTime, travelDisutility,
						eTaxiScheduler, scheduleTimingUpdater, chargingInfrastructure).get();
			}
		});

		bindModal(ChargingInfrastructure.class).toProvider(modalProvider(
				getter -> ChargingInfrastructures.createModalNetworkChargers(getter.get(ChargingInfrastructure.class),
						getter.getModal(Network.class), getMode()))).asEagerSingleton();

		bindModal(ETaxiScheduler.class).toProvider(new ModalProviders.AbstractProvider<>(taxiCfg.getMode()) {
			@Inject
			private MobsimTimer timer;

			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;

			@Override
			public ETaxiScheduler get() {
				Fleet fleet = getModalInstance(Fleet.class);
				TaxiScheduleInquiry taxiScheduleInquiry = new TaxiScheduleInquiry(taxiCfg, timer);
				Network network = getModalInstance(Network.class);
				TravelDisutility travelDisutility = getModalInstance(
						TravelDisutilityFactory.class).createTravelDisutility(travelTime);
				LeastCostPathCalculator router = new FastAStarLandmarksFactory(
						getConfig().global()).createPathCalculator(network, travelDisutility, travelTime);
				return new ETaxiScheduler(taxiCfg, fleet, taxiScheduleInquiry, travelTime, router);
			}
		}).asEagerSingleton();

		bindModal(ScheduleTimingUpdater.class).toProvider(modalProvider(
				getter -> new ScheduleTimingUpdater(getter.get(MobsimTimer.class),
						new ETaxiStayTaskEndTimeCalculator(taxiCfg)))).asEagerSingleton();

		bindModal(DynActionCreator.class).toProvider(
				new ModalProviders.AbstractProvider<ETaxiActionCreator>(taxiCfg.getMode()) {
					@Inject
					private MobsimTimer timer;

					@Inject
					private DvrpConfigGroup dvrpCfg;

					@Override
					public ETaxiActionCreator get() {
						return new ETaxiActionCreator(
								new TaxiActionCreator(getModalInstance(PassengerHandler.class), taxiCfg, timer,
										dvrpCfg));
					}
				}).asEagerSingleton();

		bindModal(PassengerRequestCreator.class).toProvider(modalProvider(
				getter -> new TaxiRequestCreator(getMode(), getter.getModal(SubmittedTaxiRequestsCollector.class))))
				.asEagerSingleton();

		bindModal(PassengerRequestValidator.class).to(DefaultPassengerRequestValidator.class).asEagerSingleton();

		bindModal(SubmittedTaxiRequestsCollector.class).to(SubmittedTaxiRequestsCollector.class).asEagerSingleton();

		addModalQSimComponentBinding().toProvider(modalProvider(
				getter -> new TaxiSimulationConsistencyChecker(getter.getModal(SubmittedTaxiRequestsCollector.class),
						taxiCfg)));

		if (taxiCfg.getTimeProfiles()) {
			addModalQSimComponentBinding().toProvider(modalProvider(
					getter -> new TaxiStatusTimeProfileCollectorProvider(getter.getModal(Fleet.class),
							getter.get(MatsimServices.class), getter.getModal(SubmittedTaxiRequestsCollector.class),
							taxiCfg).get()));
		}

		bindModal(VrpOptimizer.class).to(modalKey(TaxiOptimizer.class));
	}
}
