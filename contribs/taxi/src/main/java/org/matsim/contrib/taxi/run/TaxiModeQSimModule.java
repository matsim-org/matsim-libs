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

package org.matsim.contrib.taxi.run;

import java.util.function.Supplier;

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
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.modal.ModalProviders;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.taxi.analysis.TaxiEventSequenceCollector;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.taxi.scheduler.TaxiStayTaskEndTimeCalculator;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class TaxiModeQSimModule extends AbstractDvrpModeQSimModule {
	private final TaxiConfigGroup taxiCfg;

	public TaxiModeQSimModule(TaxiConfigGroup taxiCfg) {
		super(taxiCfg.getMode());
		this.taxiCfg = taxiCfg;
	}

	@Override
	protected void configureQSim() {
		install(new VrpAgentSourceQSimModule(getMode()));
		install(new PassengerEngineQSimModule(getMode()));

		addModalComponent(TaxiOptimizer.class,
				new ModalProviders.AbstractProvider<>(taxiCfg.getMode(), DvrpModes::mode) {
					@Inject
					private MobsimTimer timer;

					@Inject
					@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
					private TravelTime travelTime;

					@Inject
					private EventsManager events;

					@Override
					public TaxiOptimizer get() {
						Fleet fleet = getModalInstance(Fleet.class);
						Network network = getModalInstance(Network.class);
						TaxiScheduler taxiScheduler = getModalInstance(TaxiScheduler.class);
						TravelDisutility travelDisutility = getModalInstance(
								TravelDisutilityFactory.class).createTravelDisutility(travelTime);

						ScheduleTimingUpdater scheduleTimingUpdater = getModalInstance(ScheduleTimingUpdater.class);
						return new DefaultTaxiOptimizerProvider(events, taxiCfg, fleet, network, timer, travelTime,
								travelDisutility, taxiScheduler, scheduleTimingUpdater, getConfig().getContext()).get();
					}
				});

		addModalComponent(TaxiScheduler.class,
				new ModalProviders.AbstractProvider<>(taxiCfg.getMode(), DvrpModes::mode) {
					@Inject
					private MobsimTimer timer;

					@Inject
					@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
					private TravelTime travelTime;

					@Inject
					private EventsManager events;

					@Override
					public TaxiScheduler get() {
						Fleet fleet = getModalInstance(Fleet.class);
						TaxiScheduleInquiry taxiScheduleInquiry = new TaxiScheduleInquiry(taxiCfg, timer);
						Network network = getModalInstance(Network.class);
						TravelDisutility travelDisutility = getModalInstance(
								TravelDisutilityFactory.class).createTravelDisutility(travelTime);
						var speedyALTFactory = new SpeedyALTFactory();
						Supplier<LeastCostPathCalculator> routerCreator = () -> speedyALTFactory.createPathCalculator(
								network, travelDisutility, travelTime);
						return new TaxiScheduler(taxiCfg, fleet, taxiScheduleInquiry, travelTime, routerCreator, events,
								timer);
					}
				});

		bindModal(ScheduleTimingUpdater.class).toProvider(modalProvider(
				getter -> new ScheduleTimingUpdater(getter.get(MobsimTimer.class),
						new TaxiStayTaskEndTimeCalculator(taxiCfg)))).asEagerSingleton();

		bindModal(DynActionCreator.class).toProvider(
				new ModalProviders.AbstractProvider<>(taxiCfg.getMode(), DvrpModes::mode) {
					@Inject
					private MobsimTimer timer;

					@Inject
					private DvrpConfigGroup dvrpCfg;

					@Override
					public TaxiActionCreator get() {
						return new TaxiActionCreator(getModalInstance(PassengerHandler.class), taxiCfg, timer, dvrpCfg);
					}
				}).asEagerSingleton();

		bindModal(PassengerRequestCreator.class).toProvider(
				new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {
					@Inject
					private EventsManager events;

					@Override
					public TaxiRequestCreator get() {
						return new TaxiRequestCreator(getMode(), events);
					}
				}).asEagerSingleton();

		bindModal(PassengerRequestValidator.class).to(DefaultPassengerRequestValidator.class).asEagerSingleton();

		addModalQSimComponentBinding().toProvider(modalProvider(
				getter -> new TaxiSimulationConsistencyChecker(getter.getModal(TaxiEventSequenceCollector.class),
						taxiCfg)));

		bindModal(VrpOptimizer.class).to(modalKey(TaxiOptimizer.class));
	}
}
