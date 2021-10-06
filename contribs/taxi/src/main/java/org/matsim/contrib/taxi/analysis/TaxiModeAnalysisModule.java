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
package org.matsim.contrib.taxi.analysis;

import org.matsim.contrib.dvrp.analysis.ExecutedScheduleCollector;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiOccupiedDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.util.stats.TaxiVehicleOccupancyProfiles;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileCalculator;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;

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

		if (taxiCfg.getTimeProfiles()) {
			bindModal(VehicleOccupancyProfileCalculator.class).toProvider(modalProvider(
					getter -> new VehicleOccupancyProfileCalculator(getMode(),
							getter.getModal(FleetSpecification.class), 300, getter.get(QSimConfigGroup.class),
							passengerServingTaskTypes))).asEagerSingleton();
			addEventHandlerBinding().to(modalKey(VehicleOccupancyProfileCalculator.class));
			addControlerListenerBinding().to(modalKey(VehicleOccupancyProfileCalculator.class));

			addControlerListenerBinding().toProvider(modalProvider(
					getter -> TaxiVehicleOccupancyProfiles.createProfileWriter(getter.get(MatsimServices.class),
							getMode(), getter.getModal(VehicleOccupancyProfileCalculator.class))));
		}
	}
}
