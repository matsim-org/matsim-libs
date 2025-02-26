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

package org.matsim.contrib.drt.extension.preplanned.optimizer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtRoutingDriveTaskUpdater;
import org.matsim.contrib.drt.schedule.DrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.DriveTaskUpdater;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.tracker.OnlineTrackerListener;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;

/**
 * @author Michal Maciejewski (michalm)
 */
public class PreplannedDrtModeOptimizerQSimModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtCfg;

	public PreplannedDrtModeOptimizerQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		addModalComponent(DrtOptimizer.class, modalProvider(getter -> new PreplannedDrtOptimizer(drtCfg,
				getter.getModal(PreplannedDrtOptimizer.PreplannedSchedules.class), getter.getModal(Network.class),
				getter.getModal(TravelTime.class), getter.getModal(TravelDisutilityFactory.class)
				.createTravelDisutility(getter.getModal(TravelTime.class)), getter.get(MobsimTimer.class),
				getter.getModal(DrtTaskFactory.class), getter.get(EventsManager.class), getter.getModal(Fleet.class),
				getter.getModal(ScheduleTimingUpdater.class))));

		bindModal(DrtTaskFactory.class).toInstance(new DrtTaskFactoryImpl());
		
		bindModal(VrpLegFactory.class).toProvider(modalProvider(getter -> {
			DvrpConfigGroup dvrpCfg = getter.get(DvrpConfigGroup.class);
			MobsimTimer timer = getter.get(MobsimTimer.class);

			return v -> VrpLegFactory.createWithOnlineTracker(dvrpCfg.mobsimMode, v, OnlineTrackerListener.NO_LISTENER,
					timer);
		})).in(Singleton.class);

		Preconditions.checkState(drtCfg.getPrebookingParams().isEmpty(), "cannot use preplanned schedules with prebooking");
		bindModal(VrpAgentLogic.DynActionCreator.class).to(modalKey(DrtActionCreator.class));

		bindModal(VrpOptimizer.class).to(modalKey(DrtOptimizer.class));

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
	}
}
