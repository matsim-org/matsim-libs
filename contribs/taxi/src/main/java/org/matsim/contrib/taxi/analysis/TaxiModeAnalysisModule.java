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

package org.matsim.contrib.taxi.analysis;

import java.awt.Color;
import java.util.Comparator;

import org.matsim.contrib.dvrp.analysis.ExecutedScheduleCollector;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiOccupiedDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.util.stats.TaxiStatsDumper;
import org.matsim.contrib.taxi.util.stats.TaxiVehicleOccupancyProfiles;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileCalculator;
import org.matsim.contrib.util.stats.VehicleTaskProfileCalculator;
import org.matsim.contrib.util.stats.VehicleTaskProfileWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author michalm (Michal Maciejewski)
 */
public class TaxiModeAnalysisModule extends AbstractDvrpModeModule {

	private final ImmutableSet<Task.TaskType> passengerServingTaskTypes = ImmutableSet.of(TaxiEmptyDriveTask.TYPE,
			TaxiPickupTask.TYPE, TaxiOccupiedDriveTask.TYPE, TaxiDropoffTask.TYPE);

	private final TaxiConfigGroup taxiCfg;

	public TaxiModeAnalysisModule(TaxiConfigGroup taxiCfg) {
		super(taxiCfg.getMode());
		this.taxiCfg = taxiCfg;
	}

	@Override
	public void install() {
		bindModal(TaxiEventSequenceCollector.class).toProvider(
				modalProvider(getter -> new TaxiEventSequenceCollector(getMode()))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(TaxiEventSequenceCollector.class));

		bindModal(ExecutedScheduleCollector.class).toProvider(
				modalProvider(getter -> new ExecutedScheduleCollector(getMode()))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(ExecutedScheduleCollector.class));

		bindModal(TaxiStatsDumper.class).toProvider(modalProvider(
				getter -> new TaxiStatsDumper(taxiCfg, getter.get(OutputDirectoryHierarchy.class),
						getter.get(IterationCounter.class), getter.getModal(ExecutedScheduleCollector.class),
						getter.getModal(TaxiEventSequenceCollector.class)))).asEagerSingleton();
		addControlerListenerBinding().to(modalKey(TaxiStatsDumper.class));

		if (taxiCfg.timeProfiles) {
			bindModal(VehicleOccupancyProfileCalculator.class).toProvider(modalProvider(
					getter -> new VehicleOccupancyProfileCalculator(getMode(),
							getter.getModal(FleetSpecification.class), 300, getter.get(QSimConfigGroup.class),
							passengerServingTaskTypes))).asEagerSingleton();
			addEventHandlerBinding().to(modalKey(VehicleOccupancyProfileCalculator.class));
			addControlerListenerBinding().to(modalKey(VehicleOccupancyProfileCalculator.class));

			addControlerListenerBinding().toProvider(modalProvider(
					getter -> TaxiVehicleOccupancyProfiles.createProfileWriter(getter.get(MatsimServices.class),
							getMode(), getter.getModal(VehicleOccupancyProfileCalculator.class))));

			bindModal(VehicleTaskProfileCalculator.class).toProvider(modalProvider(
					getter -> new VehicleTaskProfileCalculator(getMode(), getter.getModal(FleetSpecification.class),
							300, getter.get(QSimConfigGroup.class)))).asEagerSingleton();
			addEventHandlerBinding().to(modalKey(VehicleTaskProfileCalculator.class));
			addControlerListenerBinding().to(modalKey(VehicleTaskProfileCalculator.class));

			addControlerListenerBinding().toProvider(modalProvider(
					getter -> new VehicleTaskProfileWriter(getter.get(MatsimServices.class), getMode(),
							getter.getModal(VehicleTaskProfileCalculator.class),
							Comparator.comparing(Task.TaskType::name),
							ImmutableMap.of(TaxiStayTask.TYPE, Color.LIGHT_GRAY))));
		}
	}
}
