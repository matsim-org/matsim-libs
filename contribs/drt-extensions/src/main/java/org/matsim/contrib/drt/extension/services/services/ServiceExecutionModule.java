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
package org.matsim.contrib.drt.extension.services.services;


import org.matsim.contrib.drt.extension.services.tasks.DefaultStackableTasksImpl;
import org.matsim.contrib.drt.extension.services.tasks.StackableTasks;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

/**
 * @author steffenaxer
 */
public class ServiceExecutionModule extends AbstractDvrpModeModule {

	public ServiceExecutionModule(DrtConfigGroup drtConfigGroup) {
		super(drtConfigGroup.mode);
	}

	@Override
	public void install() {
		bindModal(ServiceTriggerFactory.class).toInstance(new DefaultTriggerFactoryImpl());
		bindModal(StackableTasks.class).toInstance(new DefaultStackableTasksImpl());
	}
}
