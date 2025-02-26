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

package org.matsim.contrib.drt.optimizer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.NearestStartLinkAsDepot;
import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.DefaultInsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.RequestFleetFilter;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchQSimModule;
import org.matsim.contrib.drt.optimizer.insertion.repeatedselective.RepeatedSelectiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.repeatedselective.RepeatedSelectiveInsertionSearchQSimModule;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchQSimModule;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.passenger.DefaultOfferAcceptor;
import org.matsim.contrib.drt.passenger.DrtOfferAcceptor;
import org.matsim.contrib.drt.prebooking.PrebookingActionCreator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtRoutingDriveTaskUpdater;
import org.matsim.contrib.drt.schedule.DrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.scheduler.DefaultRequestInsertionScheduler;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.schedule.DriveTaskUpdater;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.tracker.OnlineTrackerListener;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtModeOptimizerQSimModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtCfg;

	public DrtModeOptimizerQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		addModalComponent(DrtOptimizer.class, modalProvider(
				getter -> {
					return new DefaultDrtOptimizer(drtCfg, getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
						getter.getModal(DepotFinder.class), getter.getModal(RebalancingStrategy.class),
						getter.getModal(DrtScheduleInquiry.class), getter.getModal(ScheduleTimingUpdater.class),
						getter.getModal(EmptyVehicleRelocator.class), getter.getModal(UnplannedRequestInserter.class),
						getter.getModal(DrtRequestInsertionRetryQueue.class));
					}));

		bindModal(DepotFinder.class).toProvider(
				modalProvider(getter -> new NearestStartLinkAsDepot(getter.getModal(Fleet.class)))).asEagerSingleton();

		bindModal(DrtRequestInsertionRetryQueue.class).toInstance(new DrtRequestInsertionRetryQueue(
				drtCfg.getDrtRequestInsertionRetryParams().orElse(new DrtRequestInsertionRetryParams())));

		addModalComponent(QSimScopeForkJoinPoolHolder.class,
				() -> new QSimScopeForkJoinPoolHolder(drtCfg.numberOfThreads));

		bindModal(RequestFleetFilter.class).toProvider(modalProvider(getter -> RequestFleetFilter.none));

		bindModal(UnplannedRequestInserter.class).toProvider(modalProvider(
				getter -> new DefaultUnplannedRequestInserter(drtCfg, getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class), getter.get(EventsManager.class),
						getter.getModal(RequestInsertionScheduler.class),
						getter.getModal(VehicleEntry.EntryFactory.class), getter.getModal(DrtInsertionSearch.class),
						getter.getModal(DrtRequestInsertionRetryQueue.class), getter.getModal(DrtOfferAcceptor.class),
						getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool(),
						getter.getModal(PassengerStopDurationProvider.class), getter.getModal(RequestFleetFilter.class)))).asEagerSingleton();

		bindModal(InsertionCostCalculator.class).toProvider(modalProvider(
				getter -> new DefaultInsertionCostCalculator(getter.getModal(CostCalculationStrategy.class),
						drtCfg.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet())));

		install(getInsertionSearchQSimModule(drtCfg));

		bindModal(VehicleDataEntryFactoryImpl.class).toProvider(modalProvider(getter -> {
			DvrpLoadType loadType = getter.getModal(DvrpLoadType.class);
			return new VehicleDataEntryFactoryImpl(loadType);
		}));

		bindModal(VehicleEntry.EntryFactory.class).to(modalKey(VehicleDataEntryFactoryImpl.class)).in(Singleton.class);

		DrtOptimizationConstraintsSet defaultOptimizationConstraintsSet = drtCfg.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();
		bindModal(CostCalculationStrategy.class)
				.to(defaultOptimizationConstraintsSet.rejectRequestIfMaxWaitOrTravelTimeViolated
						?
				CostCalculationStrategy.RejectSoftConstraintViolations.class :
				CostCalculationStrategy.DiscourageSoftConstraintViolations.class).asEagerSingleton();

		bindModal(DrtTaskFactory.class).toInstance(new DrtTaskFactoryImpl());

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

		boolean scheduleWaitBeforeDrive = drtCfg.getPrebookingParams().map(p -> p.scheduleWaitBeforeDrive).orElse(false);
		bindModal(RequestInsertionScheduler.class).toProvider(modalProvider(
						getter -> new DefaultRequestInsertionScheduler(getter.getModal(Fleet.class),
								getter.get(MobsimTimer.class), getter.getModal(TravelTime.class),
								getter.getModal(ScheduleTimingUpdater.class), getter.getModal(DrtTaskFactory.class),
								getter.getModal(StopTimeCalculator.class), scheduleWaitBeforeDrive)))
				.asEagerSingleton();

		bindModal(DefaultOfferAcceptor.class).toProvider(modalProvider(getter -> new DefaultOfferAcceptor(
				defaultOptimizationConstraintsSet.maxAllowedPickupDelay)));
		bindModal(DrtOfferAcceptor.class).to(modalKey(DefaultOfferAcceptor.class));

		if (!drtCfg.updateRoutes) {
			bindModal(DriveTaskUpdater.class).toInstance(DriveTaskUpdater.NOOP);
		} else {
			bindModal(DriveTaskUpdater.class).toProvider(modalProvider(getter -> {
				TravelTime travelTime = getter.getModal(TravelTime.class);
				Network network = getter.getModal(Network.class);
				DrtTaskFactory taskFactory = getter.getModal(DrtTaskFactory.class);
				TravelDisutility travelDisutility = getter.getModal(
						TravelDisutilityFactory.class).createTravelDisutility(travelTime);

				LeastCostPathCalculator lcpc = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
				return new DrtRoutingDriveTaskUpdater(taskFactory, lcpc, travelTime);
			})).in(Singleton.class);
		}

		bindModal(ScheduleTimingUpdater.class).toProvider(modalProvider(
				getter -> new ScheduleTimingUpdater(getter.get(MobsimTimer.class),
						new DrtStayTaskEndTimeCalculator(getter.getModal(StopTimeCalculator.class)),
						getter.getModal(DriveTaskUpdater.class)))).asEagerSingleton();

		bindModal(VrpLegFactory.class).toProvider(modalProvider(getter -> {
			DvrpConfigGroup dvrpCfg = getter.get(DvrpConfigGroup.class);
			MobsimTimer timer = getter.get(MobsimTimer.class);

			return v -> VrpLegFactory.createWithOnlineTracker(dvrpCfg.mobsimMode, v, OnlineTrackerListener.NO_LISTENER,
					timer);
		})).in(Singleton.class);

		if (drtCfg.getPrebookingParams().isEmpty()) {
			bindModal(VrpAgentLogic.DynActionCreator.class).to(modalKey(DrtActionCreator.class));
		} else {
			bindModal(VrpAgentLogic.DynActionCreator.class).to(modalKey(PrebookingActionCreator.class));
		}

		bindModal(VrpOptimizer.class).to(modalKey(DrtOptimizer.class));
	}

	public static AbstractDvrpModeQSimModule getInsertionSearchQSimModule(DrtConfigGroup drtCfg) {
		switch (drtCfg.getDrtInsertionSearchParams().getName()) {
			case ExtensiveInsertionSearchParams.SET_NAME:
				return new ExtensiveInsertionSearchQSimModule(drtCfg);

			case SelectiveInsertionSearchParams.SET_NAME:
				return new SelectiveInsertionSearchQSimModule(drtCfg);

			case RepeatedSelectiveInsertionSearchParams.SET_NAME:
				return new RepeatedSelectiveInsertionSearchQSimModule(drtCfg);

			default:
				throw new RuntimeException(
						"Unsupported DRT insertion search type: " + drtCfg.getDrtInsertionSearchParams().getName());
		}
	}
}
