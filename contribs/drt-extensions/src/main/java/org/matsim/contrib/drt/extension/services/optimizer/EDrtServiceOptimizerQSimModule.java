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

import org.matsim.contrib.drt.extension.edrt.EDrtActionCreator;
import org.matsim.contrib.drt.extension.edrt.optimizer.EDrtOptimizer;
import org.matsim.contrib.drt.extension.edrt.optimizer.EDrtVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.edrt.scheduler.EmptyVehicleChargingScheduler;
import org.matsim.contrib.drt.extension.services.dispatcher.ServiceTaskDispatcher;
import org.matsim.contrib.drt.extension.services.schedule.EDrtServiceDynActionCreator;
import org.matsim.contrib.drt.extension.services.tasks.EDrtServiceTaskFactoryImpl;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.prebooking.PrebookingActionCreator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class EDrtServiceOptimizerQSimModule extends AbstractDvrpModeQSimModule {
	DrtConfigGroup drtConfigGroup;

	public EDrtServiceOptimizerQSimModule(DrtConfigGroup drtConfigGroup) {
		super(drtConfigGroup.getMode());
		this.drtConfigGroup = drtConfigGroup;
	}

	@Override
	protected void configureQSim() {

		bindModal(EDrtServiceDynActionCreator.class).toProvider(modalProvider(getter -> {
			VrpAgentLogic.DynActionCreator delegate = drtConfigGroup.getPrebookingParams().isPresent()
				? getter.getModal(PrebookingActionCreator.class)
				: getter.getModal(DrtActionCreator.class);

			return new EDrtServiceDynActionCreator(new EDrtActionCreator(delegate, getter.get(MobsimTimer.class)), getter.get(MobsimTimer.class));
		})).asEagerSingleton();

		bindModal(VrpAgentLogic.DynActionCreator.class).to(modalKey(EDrtServiceDynActionCreator.class));

		bindModal(DrtTaskFactory.class).toProvider(modalProvider(getter ->
			new EDrtServiceTaskFactoryImpl())).asEagerSingleton();

		bindModal(VehicleEntry.EntryFactory.class).toProvider(modalProvider(getter ->
			{
				// Reuse already bound EDrtVehicleDataEntryFactoryProvider
				EDrtVehicleDataEntryFactory delegate = getter.getModal(EDrtVehicleDataEntryFactory.class);
				return new DrtServiceEntryFactory(delegate);
			}

		)).asEagerSingleton();

		addModalComponent(DrtOptimizer.class, modalProvider(
			getter -> {
				var delegate =  new EDrtOptimizer(drtConfigGroup, getter.getModal(DefaultDrtOptimizer.class),
					getter.getModal(EmptyVehicleChargingScheduler.class));
				return new DrtServiceTaskOptimizer(getter.getModal(ServiceTaskDispatcher.class),
					delegate,
					getter.getModal(ScheduleTimingUpdater.class),
					getter.get(MobsimTimer.class));
			}));
	}
}
