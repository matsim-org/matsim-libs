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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.util.stats.DrtVehicleOccupancyProfileCalculator;
import org.matsim.contrib.drt.util.stats.DrtVehicleOccupancyProfileWriter;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;

import com.google.common.collect.ImmutableSet;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtModeAnalysisModule extends AbstractDvrpModeModule {
	private final DrtConfigGroup drtCfg;
	private final ImmutableSet<Task.TaskType> nonPassengerServingTaskTypes;

	public DrtModeAnalysisModule(DrtConfigGroup drtCfg) {
		this(drtCfg, ImmutableSet.of(DrtDriveTask.TYPE, DrtStopTask.TYPE));
	}

	public DrtModeAnalysisModule(DrtConfigGroup drtCfg, ImmutableSet<Task.TaskType> nonPassengerServingTaskTypes) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.nonPassengerServingTaskTypes = nonPassengerServingTaskTypes;
	}

	@Override
	public void install() {
		bindModal(DrtVehicleDistanceStats.class).toProvider(modalProvider(
				getter -> new DrtVehicleDistanceStats(getter.get(Network.class), drtCfg,
						getter.getModal(FleetSpecification.class)))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(DrtVehicleDistanceStats.class));

		bindModal(DrtEventSequenceCollector.class).toProvider(
				modalProvider(getter -> new DrtEventSequenceCollector(drtCfg.getMode()))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(DrtEventSequenceCollector.class));

		bindModal(DrtVehicleOccupancyProfileCalculator.class).toProvider(modalProvider(
				getter -> new DrtVehicleOccupancyProfileCalculator(getMode(), getter.getModal(FleetSpecification.class),
						300, getter.get(QSimConfigGroup.class), nonPassengerServingTaskTypes))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(DrtVehicleOccupancyProfileCalculator.class));
		addControlerListenerBinding().to(modalKey(DrtVehicleOccupancyProfileCalculator.class));

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> new DrtVehicleOccupancyProfileWriter(getter.get(MatsimServices.class), drtCfg,
						getter.getModal(DrtVehicleOccupancyProfileCalculator.class))));

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> new DrtAnalysisControlerListener(getter.get(Config.class), drtCfg,
						getter.getModal(FleetSpecification.class), getter.getModal(DrtVehicleDistanceStats.class),
						getter.get(MatsimServices.class), getter.get(Network.class),
						getter.getModal(DrtEventSequenceCollector.class),
						getter.getModal(DrtVehicleOccupancyProfileCalculator.class)))).asEagerSingleton();
	}
}
