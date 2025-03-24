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
package org.matsim.contrib.drt.extension.services.analysis;


import org.matsim.contrib.common.timeprofile.ProfileWriter;
import org.matsim.contrib.drt.extension.services.analysis.DrtServiceProfileCalculator;
import org.matsim.contrib.drt.extension.services.analysis.DrtServiceProfileView;
import org.matsim.contrib.drt.extension.services.tasks.DefaultStackableTasksImpl;
import org.matsim.contrib.drt.extension.services.tasks.StackableTasks;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;

/**
 * @author steffenaxer
 */
public class ServiceAnalysisModule extends AbstractDvrpModeModule {
	private static final int TIME_RESOLUTION = 5; // High resolution req. to visualize short tasks
	DrtConfigGroup drtConfigGroup;

	public ServiceAnalysisModule(DrtConfigGroup drtConfigGroup) {
		super(drtConfigGroup.mode);
		this.drtConfigGroup = drtConfigGroup;
	}

	@Override
	public void install() {

		bindModal(DrtServiceProfileCalculator.class).toProvider(modalProvider(
			getter -> new DrtServiceProfileCalculator(getMode(), getter.getModal(FleetSpecification.class), TIME_RESOLUTION,
				getter.get(QSimConfigGroup.class)))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(DrtServiceProfileCalculator.class));

		addControlerListenerBinding().toProvider(modalProvider(getter -> {
			MatsimServices matsimServices = getter.get(MatsimServices.class);
			String mode = drtConfigGroup.getMode();
			var profileView = new DrtServiceProfileView(getter.getModal(DrtServiceProfileCalculator.class));
			return new ProfileWriter(matsimServices, mode, profileView, "service_time_profiles");
		}));
	}
}
