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
package org.matsim.contrib.drt.extension.services.services.tracker;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.MatsimServices;

/**
 * @author steffenaxer
 */
public class ServiceExecutionTrackingModule extends AbstractDvrpModeModule {
	DrtConfigGroup drtConfigGroup;

	public ServiceExecutionTrackingModule(DrtConfigGroup drtConfigGroup) {
		super(drtConfigGroup.mode);
		this.drtConfigGroup = drtConfigGroup;
	}

	@Override
	public void install() {

		bindModal(ServiceExecutionTrackers.class).toProvider(modalProvider(getter -> new ServiceExecutionTrackersImpl(
			getter.getModal(FleetSpecification.class),
			drtConfigGroup,
			getter.get(MatsimServices.class)
		))).asEagerSingleton();

		addEventHandlerBinding().to(modalKey(ServiceExecutionTrackers.class));
		addControlerListenerBinding().to(modalKey(ServiceExecutionTrackers.class));
	}
}
