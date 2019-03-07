/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.fleet;

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

/**
 * 1. Collects information on fleet vehicles on MobsimBeforeCleanupEvent.
 * 2. Updates vehicle fleet specifications on IterationEndsEvent.
 *
 * @author Michal Maciejewski (michalm)
 */
public class VehicleStartLinkToLastLinkUpdater
		implements QSimScopeObjectListener<Fleet>, MobsimBeforeCleanupListener, IterationEndsListener {
	private final FleetSpecification fleetSpecification;
	private List<DvrpVehicleSpecification> updatedVehSpecifications;
	private Fleet fleet;

	public VehicleStartLinkToLastLinkUpdater(FleetSpecification fleetSpecification) {
		this.fleetSpecification = fleetSpecification;
	}

	@Override
	public void objectCreated(Fleet fleet) {
		this.fleet = fleet;
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		updatedVehSpecifications = fleet.getVehicles()
				.values()
				.stream()
				.map(v -> ImmutableDvrpVehicleSpecification.newBuilder()
						.id(v.getId())
						.startLinkId(Schedules.getLastLinkInSchedule(v).getId())
						.capacity(v.getCapacity())
						.serviceBeginTime(v.getServiceBeginTime())
						.serviceEndTime(v.getServiceEndTime())
						.build())
				.collect(Collectors.toList());
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		updatedVehSpecifications.forEach(fleetSpecification::replaceVehicleSpecification);
	}
}
