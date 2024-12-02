/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.extension.services.optimizer;

import org.matsim.contrib.drt.extension.services.dispatcher.ServiceTaskDispatcher;
import org.matsim.contrib.drt.extension.services.schedule.DrtServiceDynActionCreator;
import org.matsim.contrib.drt.extension.services.tasks.DrtServiceTaskFactoryImpl;
import org.matsim.contrib.drt.optimizer.*;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.prebooking.PrebookingActionCreator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class DrtServiceOptimizerQSimModule extends AbstractDvrpModeQSimModule {
	DrtConfigGroup drtConfigGroup;

	public DrtServiceOptimizerQSimModule(DrtConfigGroup drtConfigGroup) {
		super(drtConfigGroup.getMode());
		this.drtConfigGroup = drtConfigGroup;
	}

	@Override
	protected void configureQSim() {
		bindModal(DrtServiceDynActionCreator.class).toProvider(modalProvider(getter -> {
			VrpAgentLogic.DynActionCreator delegate = drtConfigGroup.getPrebookingParams().isPresent()
				? getter.getModal(PrebookingActionCreator.class)
				: getter.getModal(DrtActionCreator.class);

			return new DrtServiceDynActionCreator(delegate, getter.get(MobsimTimer.class));
		})).asEagerSingleton();

		bindModal(VrpAgentLogic.DynActionCreator.class).to(modalKey(DrtServiceDynActionCreator.class));


		bindModal(DrtTaskFactory.class).toProvider(modalProvider(getter ->
		{
			DrtTaskFactory delegate = new DrtTaskFactoryImpl();
			return new DrtServiceTaskFactoryImpl(delegate);
		})).asEagerSingleton();

		bindModal(VehicleEntry.EntryFactory.class).toProvider(modalProvider(getter ->
			new DrtServiceEntryFactory(new VehicleDataEntryFactoryImpl()))).asEagerSingleton();

		addModalComponent(DrtOptimizer.class, modalProvider(
			getter -> {
				var delegate = new DefaultDrtOptimizer(drtConfigGroup, getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
					getter.getModal(DepotFinder.class), getter.getModal(RebalancingStrategy.class),
					getter.getModal(DrtScheduleInquiry.class), getter.getModal(ScheduleTimingUpdater.class),
					getter.getModal(EmptyVehicleRelocator.class), getter.getModal(UnplannedRequestInserter.class),
					getter.getModal(DrtRequestInsertionRetryQueue.class));
				return new DrtServiceTaskOptimizer(getter.getModal(ServiceTaskDispatcher.class),
					delegate,
					getter.getModal(ScheduleTimingUpdater.class),
					getter.get(MobsimTimer.class));
			}));
	}
}
