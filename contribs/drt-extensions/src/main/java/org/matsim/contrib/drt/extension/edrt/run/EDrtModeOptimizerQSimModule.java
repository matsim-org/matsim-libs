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

package org.matsim.contrib.drt.extension.edrt.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.edrt.EDrtActionCreator;
import org.matsim.contrib.drt.extension.edrt.optimizer.EDrtOptimizer;
import org.matsim.contrib.drt.extension.edrt.optimizer.EDrtVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.edrt.optimizer.depot.NearestChargerAsDepot;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.edrt.scheduler.EmptyVehicleChargingScheduler;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtModeOptimizerQSimModule;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.DefaultInsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryQueue;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.passenger.DrtOfferAcceptor;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.StopDurationEstimator;
import org.matsim.contrib.drt.scheduler.DefaultRequestInsertionScheduler;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructures;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EDrtModeOptimizerQSimModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtCfg;

	public EDrtModeOptimizerQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		addModalComponent(DrtOptimizer.class, modalProvider(
				getter -> new EDrtOptimizer(drtCfg, getter.getModal(DefaultDrtOptimizer.class),
						getter.getModal(EmptyVehicleChargingScheduler.class))));

		bindModal(DefaultDrtOptimizer.class).toProvider(modalProvider(
				getter -> new DefaultDrtOptimizer(drtCfg, getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
						getter.getModal(DepotFinder.class), getter.getModal(RebalancingStrategy.class),
						getter.getModal(DrtScheduleInquiry.class), getter.getModal(ScheduleTimingUpdater.class),
						getter.getModal(EmptyVehicleRelocator.class), getter.getModal(UnplannedRequestInserter.class),
						getter.getModal(DrtRequestInsertionRetryQueue.class)))).asEagerSingleton();

		bindModal(ChargingInfrastructure.class).toProvider(modalProvider(
				getter -> ChargingInfrastructures.createModalNetworkChargers(getter.get(ChargingInfrastructure.class),
						getter.getModal(Network.class), getMode()))).asEagerSingleton();

		// XXX if overridden to something else, make sure that the depots are equipped with chargers
		//  otherwise vehicles will not re-charge
		bindModal(DepotFinder.class).toProvider(
						modalProvider(getter -> new NearestChargerAsDepot(getter.getModal(ChargingInfrastructure.class))))
				.asEagerSingleton();

		bindModal(EmptyVehicleChargingScheduler.class).toProvider(
				new ModalProviders.AbstractProvider<>(drtCfg.getMode(), DvrpModes::mode) {
					@Inject
					private MobsimTimer timer;

					@Override
					public EmptyVehicleChargingScheduler get() {
						var taskFactory = getModalInstance(DrtTaskFactory.class);
						var chargingInfrastructure = getModalInstance(ChargingInfrastructure.class);
						return new EmptyVehicleChargingScheduler(timer, taskFactory, chargingInfrastructure);
					}
				}).asEagerSingleton();

		bindModal(DrtRequestInsertionRetryQueue.class).toInstance(new DrtRequestInsertionRetryQueue(
				drtCfg.getDrtRequestInsertionRetryParams().orElse(new DrtRequestInsertionRetryParams())));

		addModalComponent(QSimScopeForkJoinPoolHolder.class,
				() -> new QSimScopeForkJoinPoolHolder(drtCfg.numberOfThreads));

		bindModal(UnplannedRequestInserter.class).toProvider(modalProvider(
				getter -> new DefaultUnplannedRequestInserter(drtCfg, getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class), getter.get(EventsManager.class),
						getter.getModal(RequestInsertionScheduler.class),
						getter.getModal(VehicleEntry.EntryFactory.class), getter.getModal(DrtInsertionSearch.class),
						getter.getModal(DrtRequestInsertionRetryQueue.class), getter.getModal(DrtOfferAcceptor.class),
						getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool()))).asEagerSingleton();

		bindModal(InsertionCostCalculator.class).toProvider(modalProvider(
				getter -> new DefaultInsertionCostCalculator(getter.getModal(CostCalculationStrategy.class))));

		install(DrtModeOptimizerQSimModule.getInsertionSearchQSimModule(drtCfg));

		bindModal(VehicleEntry.EntryFactory.class).toProvider(
				EDrtVehicleDataEntryFactory.EDrtVehicleDataEntryFactoryProvider.class).asEagerSingleton();

		bindModal(CostCalculationStrategy.class).to(drtCfg.rejectRequestIfMaxWaitOrTravelTimeViolated ?
				CostCalculationStrategy.RejectSoftConstraintViolations.class :
				CostCalculationStrategy.DiscourageSoftConstraintViolations.class).asEagerSingleton();

		bindModal(DrtTaskFactory.class).toInstance(new EDrtTaskFactoryImpl());

		bindModal(EmptyVehicleRelocator.class).toProvider(
				new ModalProviders.AbstractProvider<>(drtCfg.getMode(), DvrpModes::mode) {
					@Inject
					private MobsimTimer timer;

					@Override
					public EmptyVehicleRelocator get() {
						var travelTime = getModalInstance(TravelTime.class);
						Network network = getModalInstance(Network.class);
						DrtTaskFactory taskFactory = getModalInstance(DrtTaskFactory.class);
						TravelDisutility travelDisutility = getModalInstance(
								TravelDisutilityFactory.class).createTravelDisutility(travelTime);
						return new EmptyVehicleRelocator(network, travelTime, travelDisutility, timer, taskFactory);
					}
				}).asEagerSingleton();

		bindModal(DrtScheduleInquiry.class).to(DrtScheduleInquiry.class).asEagerSingleton();

		bindModal(RequestInsertionScheduler.class).toProvider(modalProvider(
						getter -> new DefaultRequestInsertionScheduler(getter.getModal(Fleet.class),
								getter.get(MobsimTimer.class), getter.getModal(TravelTime.class),
								getter.getModal(ScheduleTimingUpdater.class), getter.getModal(DrtTaskFactory.class),
								getter.getModal(StopDurationEstimator.class))))
				.asEagerSingleton();

		bindModal(DrtOfferAcceptor.class).toInstance(DrtOfferAcceptor.DEFAULT_ACCEPTOR);

		bindModal(ScheduleTimingUpdater.class).toProvider(modalProvider(
				getter -> new ScheduleTimingUpdater(getter.get(MobsimTimer.class),
						new EDrtStayTaskEndTimeCalculator(getter.getModal(StopDurationEstimator.class))))).asEagerSingleton();

		bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(modalProvider(
				getter -> new EDrtActionCreator(getter.getModal(PassengerHandler.class), getter.get(MobsimTimer.class),
						getter.get(DvrpConfigGroup.class)))).asEagerSingleton();

		bindModal(VrpOptimizer.class).to(modalKey(DrtOptimizer.class));
	}
}
