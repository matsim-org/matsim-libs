/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.etaxi.optimizer;

import java.util.function.Supplier;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.etaxi.ETaxiActionCreator;
import org.matsim.contrib.etaxi.ETaxiScheduler;
import org.matsim.contrib.etaxi.util.ETaxiStayTaskEndTimeCalculator;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructures;
import org.matsim.contrib.taxi.analysis.TaxiEventSequenceCollector;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ETaxiModeOptimizerQSimModule extends AbstractDvrpModeQSimModule {

	private final TaxiConfigGroup taxiCfg;

	public ETaxiModeOptimizerQSimModule(TaxiConfigGroup taxiCfg) {
		super(taxiCfg.getMode());
		this.taxiCfg = taxiCfg;
	}

	@Override
	protected void configureQSim() {
		addModalComponent(TaxiOptimizer.class,
				new ModalProviders.AbstractProvider<>(taxiCfg.getMode(), DvrpModes::mode) {
					@Inject
					private MobsimTimer timer;

					@Inject
					private EventsManager events;

					@Override
					public TaxiOptimizer get() {
						var travelTime = getModalInstance(TravelTime.class);
						var fleet = getModalInstance(Fleet.class);
						var network = getModalInstance(Network.class);
						var eTaxiScheduler = getModalInstance(ETaxiScheduler.class);
						var travelDisutility = getModalInstance(TravelDisutilityFactory.class).createTravelDisutility(
								travelTime);
						var chargingInfrastructure = getModalInstance(ChargingInfrastructure.class);
						var scheduleTimingUpdater = getModalInstance(ScheduleTimingUpdater.class);
						return new ETaxiOptimizerProvider(events, taxiCfg, fleet, network, timer, travelTime,
								travelDisutility, eTaxiScheduler, scheduleTimingUpdater, chargingInfrastructure).get();
					}
				});

		bindModal(ChargingInfrastructure.class).toProvider(modalProvider(
				getter -> ChargingInfrastructures.createModalNetworkChargers(getter.get(ChargingInfrastructure.class),
						getter.getModal(Network.class), getMode()))).asEagerSingleton();

		addModalComponent(ETaxiScheduler.class,
				new ModalProviders.AbstractProvider<>(taxiCfg.getMode(), DvrpModes::mode) {
					@Inject
					private MobsimTimer timer;

					@Inject
					private EventsManager events;

					@Override
					public ETaxiScheduler get() {
						var travelTime = getModalInstance(TravelTime.class);
						Fleet fleet = getModalInstance(Fleet.class);
						TaxiScheduleInquiry taxiScheduleInquiry = new TaxiScheduleInquiry(taxiCfg, timer);
						Network network = getModalInstance(Network.class);
						TravelDisutility travelDisutility = getModalInstance(
								TravelDisutilityFactory.class).createTravelDisutility(travelTime);
						var speedyALTFactory = new SpeedyALTFactory();
						Supplier<LeastCostPathCalculator> routerCreator = () -> speedyALTFactory.createPathCalculator(
								network, travelDisutility, travelTime);
						return new ETaxiScheduler(taxiCfg, fleet, taxiScheduleInquiry, travelTime, routerCreator,
								events, timer);
					}
				});

		bindModal(ScheduleTimingUpdater.class).toProvider(modalProvider(
				getter -> new ScheduleTimingUpdater(getter.get(MobsimTimer.class),
						new ETaxiStayTaskEndTimeCalculator(taxiCfg)))).asEagerSingleton();

		bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(
				new ModalProviders.AbstractProvider<>(taxiCfg.getMode(), DvrpModes::mode) {
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

		addModalQSimComponentBinding().toProvider(modalProvider(
				getter -> new TaxiSimulationConsistencyChecker(getter.getModal(TaxiEventSequenceCollector.class),
						taxiCfg)));

		bindModal(VrpOptimizer.class).to(modalKey(TaxiOptimizer.class));
	}
}
