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

/**
 *
 */
package org.matsim.contrib.drt.analysis;

import com.google.common.collect.ImmutableSet;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.util.stats.DrtVehicleOccupancyProfiles;
import org.matsim.contrib.drt.util.stats.DrtVehicleTaskProfiles;
import org.matsim.contrib.dvrp.analysis.ExecutedScheduleCollector;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileCalculator;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileWriter;
import org.matsim.contrib.util.stats.VehicleTaskProfileCalculator;
import org.matsim.contrib.util.stats.VehicleTaskProfileWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtModeAnalysisModule extends AbstractDvrpModeModule {
	private final DrtConfigGroup drtCfg;
	private ImmutableSet<Task.TaskType> passengerServingTaskTypes = ImmutableSet.of(DrtDriveTask.TYPE,
			DefaultDrtStopTask.TYPE);

	public DrtModeAnalysisModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	public DrtModeAnalysisModule(DrtConfigGroup drtCfg, ImmutableSet<Task.TaskType> passengerServingTaskTypes) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.passengerServingTaskTypes = passengerServingTaskTypes;
	}

	@Override
	public void install() {
		bindModal(ExecutedScheduleCollector.class).toProvider(
				modalProvider(getter -> new ExecutedScheduleCollector(getMode()))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(ExecutedScheduleCollector.class));

		bindModal(DrtVehicleDistanceStats.class).toProvider(modalProvider(
				getter -> new DrtVehicleDistanceStats(getter.get(Network.class), drtCfg,
						getter.getModal(FleetSpecification.class)))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(DrtVehicleDistanceStats.class));

		bindModal(DrtEventSequenceCollector.class).toProvider(
				modalProvider(getter -> new DrtEventSequenceCollector(drtCfg.getMode()))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(DrtEventSequenceCollector.class));

		bindModal(VehicleOccupancyProfileCalculator.class).toProvider(modalProvider(
				getter -> new VehicleOccupancyProfileCalculator(getMode(), getter.getModal(FleetSpecification.class),
						300, getter.get(QSimConfigGroup.class), passengerServingTaskTypes))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(VehicleOccupancyProfileCalculator.class));
		addControlerListenerBinding().to(modalKey(VehicleOccupancyProfileCalculator.class));

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> DrtVehicleOccupancyProfiles.createProfileWriter(getter.get(MatsimServices.class),
						drtCfg.getMode(), getter.getModal(VehicleOccupancyProfileCalculator.class))));

		bindModal(VehicleTaskProfileCalculator.class).toProvider(modalProvider(
				getter -> new VehicleTaskProfileCalculator(getMode(), getter.getModal(FleetSpecification.class), 300,
						getter.get(QSimConfigGroup.class)))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(VehicleTaskProfileCalculator.class));
		addControlerListenerBinding().to(modalKey(VehicleTaskProfileCalculator.class));

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> DrtVehicleTaskProfiles.createProfileWriter(getter.get(MatsimServices.class),
						drtCfg.getMode(), getter.getModal(VehicleTaskProfileCalculator.class))));

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> new DrtAnalysisControlerListener(getter.get(Config.class), drtCfg,
						getter.getModal(FleetSpecification.class), getter.getModal(DrtVehicleDistanceStats.class),
						getter.get(MatsimServices.class), getter.get(Network.class),
						getter.getModal(DrtEventSequenceCollector.class),
						getter.getModal(VehicleOccupancyProfileCalculator.class)))).asEagerSingleton();
	}
}
