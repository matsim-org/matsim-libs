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

package org.matsim.contrib.edrt.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.PrecalculablePathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.passenger.DrtRequestCreator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.DrtScheduleTimingUpdater;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.edrt.EDrtActionCreator;
import org.matsim.contrib.edrt.optimizer.EDrtOptimizer;
import org.matsim.contrib.edrt.optimizer.EDrtVehicleDataEntryFactory;
import org.matsim.contrib.edrt.optimizer.depot.NearestChargerAsDepot;
import org.matsim.contrib.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.edrt.scheduler.EmptyVehicleChargingScheduler;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EDrtModeQSimModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtCfg;

	public EDrtModeQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		install(new VrpAgentSourceQSimModule(getMode()));
		install(new PassengerEngineQSimModule(getMode()));

		addModalComponent(DrtOptimizer.class, modalProvider(
				getter -> new EDrtOptimizer(drtCfg, getter.getModal(DefaultDrtOptimizer.class),
						getter.getModal(EmptyVehicleChargingScheduler.class))));

		bindModal(DefaultDrtOptimizer.class).toProvider(modalProvider(
				getter -> new DefaultDrtOptimizer(drtCfg, getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
						getter.getModal(DepotFinder.class), getter.getModal(RebalancingStrategy.class),
						getter.getModal(DrtScheduleInquiry.class), getter.getModal(DrtScheduleTimingUpdater.class),
						getter.getModal(EmptyVehicleRelocator.class), getter.getModal(UnplannedRequestInserter.class))))
				.asEagerSingleton();

		// XXX if overridden to something else, make sure that the depots are equipped with chargers
		//  otherwise vehicles will not re-charge
		bindModal(DepotFinder.class).to(NearestChargerAsDepot.class);

		bindModal(PassengerRequestValidator.class).to(DefaultPassengerRequestValidator.class).asEagerSingleton();

		bindModal(EmptyVehicleChargingScheduler.class).toProvider(
				new ModalProviders.AbstractProvider<EmptyVehicleChargingScheduler>(drtCfg.getMode()) {
					@Inject
					private MobsimTimer timer;

					@Inject
					private ChargingInfrastructure chargingInfrastructure;

					@Override
					public EmptyVehicleChargingScheduler get() {
						DrtTaskFactory taskFactory = getModalInstance(DrtTaskFactory.class);
						return new EmptyVehicleChargingScheduler(timer, taskFactory, chargingInfrastructure);
					}
				}).asEagerSingleton();

		addModalComponent(DefaultUnplannedRequestInserter.class, modalProvider(
				getter -> new DefaultUnplannedRequestInserter(drtCfg, getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class), getter.get(EventsManager.class),
						getter.getModal(RequestInsertionScheduler.class),
						getter.getModal(VehicleData.EntryFactory.class),
						getter.getModal(PrecalculablePathDataProvider.class),
						getter.getModal(InsertionCostCalculator.PenaltyCalculator.class))));
		bindModal(UnplannedRequestInserter.class).to(modalKey(DefaultUnplannedRequestInserter.class));

		bindModal(VehicleData.EntryFactory.class).toProvider(
				EDrtVehicleDataEntryFactory.EDrtVehicleDataEntryFactoryProvider.class).asEagerSingleton();

		bindModal(InsertionCostCalculator.PenaltyCalculator.class).to(drtCfg.isRequestRejection() ?
				InsertionCostCalculator.RejectSoftConstraintViolations.class :
				InsertionCostCalculator.DiscourageSoftConstraintViolations.class).asEagerSingleton();

		bindModal(DrtTaskFactory.class).toInstance(new EDrtTaskFactoryImpl());

		bindModal(EmptyVehicleRelocator.class).toProvider(
				new ModalProviders.AbstractProvider<EmptyVehicleRelocator>(drtCfg.getMode()) {
					@Inject
					@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
					private TravelTime travelTime;

					@Inject
					private MobsimTimer timer;

					@Override
					public EmptyVehicleRelocator get() {
						Network network = getModalInstance(Network.class);
						DrtTaskFactory taskFactory = getModalInstance(DrtTaskFactory.class);
						TravelDisutility travelDisutility = getModalInstance(
								TravelDisutilityFactory.class).createTravelDisutility(travelTime);
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

		addModalComponent(ParallelPathDataProvider.class,
				new ModalProviders.AbstractProvider<ParallelPathDataProvider>(getMode()) {
					@Inject
					@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
					private TravelTime travelTime;

					@Override
					public ParallelPathDataProvider get() {
						Network network = getModalInstance(Network.class);
						TravelDisutility travelDisutility = getModalInstance(
								TravelDisutilityFactory.class).createTravelDisutility(travelTime);
						return new ParallelPathDataProvider(network, travelTime, travelDisutility, drtCfg);
					}
				});
		bindModal(PrecalculablePathDataProvider.class).to(modalKey(ParallelPathDataProvider.class));

		bindModal(VrpAgentLogic.DynActionCreator.class).
				toProvider(modalProvider(getter -> new EDrtActionCreator(getter.getModal(PassengerEngine.class),
						getter.get(MobsimTimer.class), getter.get(DvrpConfigGroup.class)))).
				asEagerSingleton();

		bindModal(PassengerRequestCreator.class).toProvider(new Provider<DrtRequestCreator>() {
			@Inject
			private EventsManager events;
			@Inject
			private MobsimTimer timer;

			@Override
			public DrtRequestCreator get() {
				return new DrtRequestCreator(getMode(), events, timer);
			}
		}).asEagerSingleton();

		bindModal(VrpOptimizer.class).to(modalKey(DrtOptimizer.class));
	}
}
