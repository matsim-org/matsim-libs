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

package org.matsim.contrib.drt.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.PrecalculablePathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.passenger.DrtRequestCreator;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.DrtScheduleTimingUpdater;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractMultiModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MultiModeDrtQSimModule extends AbstractMultiModeQSimModule {
	private final DrtConfigGroup drtCfg;

	public MultiModeDrtQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		bindModal(DrtOptimizer.class).toProvider(modalProvider(
				getter -> new DefaultDrtOptimizer(drtCfg, getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
						getter.getModal(DepotFinder.class), getter.getModal(RebalancingStrategy.class),
						getter.getModal(DrtScheduleInquiry.class), getter.getModal(DrtScheduleTimingUpdater.class),
						getter.getModal(EmptyVehicleRelocator.class), getter.getModal(UnplannedRequestInserter.class))))
				.asEagerSingleton();

		bindModal(DefaultUnplannedRequestInserter.class).toProvider(modalProvider(
				getter -> new DefaultUnplannedRequestInserter(drtCfg, getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class), getter.get(EventsManager.class),
						getter.getModal(RequestInsertionScheduler.class),
						getter.getModal(VehicleData.EntryFactory.class),
						getter.getModal(PrecalculablePathDataProvider.class),
						getter.getModal(InsertionCostCalculator.PenaltyCalculator.class)))).asEagerSingleton();
		bindModal(UnplannedRequestInserter.class).to(modalKey(DefaultUnplannedRequestInserter.class));

		bindModal(VehicleData.EntryFactory.class).toInstance(new VehicleDataEntryFactoryImpl(drtCfg));

		bindModal(DrtTaskFactory.class).toInstance(new DrtTaskFactoryImpl());

		bindModal(EmptyVehicleRelocator.class).toProvider(
				new ModalProviders.AbstractProvider<EmptyVehicleRelocator>(drtCfg.getMode()) {
					@Inject
					@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
					private Network network;

					@Inject
					@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
					private TravelTime travelTime;

					@Inject
					@Drt
					private TravelDisutility travelDisutility;

					@Inject
					private MobsimTimer timer;

					@Override
					public EmptyVehicleRelocator get() {
						DrtTaskFactory taskFactory = getModalInstance(DrtTaskFactory.class);
						return new EmptyVehicleRelocator(network, travelTime, travelDisutility, timer, taskFactory);
					}
				}).asEagerSingleton();

		bindModal(DrtScheduleInquiry.class).to(DrtScheduleInquiry.class).asEagerSingleton();

		bindModal(RequestInsertionScheduler.class).toProvider(modalProvider(
				getter -> new RequestInsertionScheduler(drtCfg, getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class),
						getter.getNamed(TravelTime.class, DvrpTravelTimeModule.DVRP_ESTIMATED),
						getter.getModal(DrtScheduleTimingUpdater.class), getter.getModal(DrtTaskFactory.class))))
				.asEagerSingleton();

		bindModal(DrtScheduleTimingUpdater.class).toProvider(new Provider<DrtScheduleTimingUpdater>() {
			@Inject
			private MobsimTimer timer;

			@Override
			public DrtScheduleTimingUpdater get() {
				return new DrtScheduleTimingUpdater(drtCfg, timer);
			}
		}).asEagerSingleton();

		bindModal(ParallelPathDataProvider.class).toProvider(new Provider<ParallelPathDataProvider>() {
			@Inject
			@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
			private Network network;
			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;
			@Inject
			@Drt
			private TravelDisutility travelDisutility;

			@Override
			public ParallelPathDataProvider get() {
				return new ParallelPathDataProvider(network, travelTime, travelDisutility, drtCfg);
			}
		}).asEagerSingleton();
		bindModal(PrecalculablePathDataProvider.class).to(modalKey(ParallelPathDataProvider.class));

		bindModal(VrpAgentLogic.DynActionCreator.class).
				toProvider(modalProvider(getter -> new DrtActionCreator(getter.getModal(PassengerEngine.class),
						getter.getModal(DrtOptimizer.class), getter.get(MobsimTimer.class),
						getter.get(DvrpConfigGroup.class)))).
				asEagerSingleton();

		bindModal(PassengerRequestCreator.class).toProvider(new Provider<DrtRequestCreator>() {
			@Inject
			private EventsManager events;
			@Inject
			private MobsimTimer timer;

			@Override
			public DrtRequestCreator get() {
				return new DrtRequestCreator(drtCfg, events, timer);
			}
		}).asEagerSingleton();

		bindModal(VrpOptimizer.class).to(modalKey(DrtOptimizer.class));
	}
}
