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
package org.matsim.contrib.drt.extension.services.tasks;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.services.schedule.DrtService;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

/**
 * @author steffenaxer
 */
public class DrtServiceTaskFactoryImpl implements ServiceTaskFactory {
	private final DrtTaskFactory delegate;

	public DrtServiceTaskFactoryImpl(DrtTaskFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public DrtServiceTask createServiceTask(Id<DrtService> drtServiceId, double beginTime, double endTime, Link link, OperationFacility operationFacility) {
		return new DrtServiceTask(drtServiceId, beginTime, endTime, link, operationFacility);
	}

	@Override
	public DrtDriveTask createDriveTask(DvrpVehicle vehicle, VrpPathWithTravelData path, DrtTaskType drtTaskType) {
		return delegate.createDriveTask(vehicle, path, drtTaskType);
	}

	@Override
	public DrtStopTask createStopTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
		return delegate.createStopTask(vehicle, beginTime, endTime, link);
	}

	@Override
	public DrtStayTask createStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
		return delegate.createStayTask(vehicle, beginTime, endTime, link);
	}

	@Override
	public DefaultStayTask createInitialTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
		return delegate.createInitialTask(vehicle, beginTime, endTime, link);
	}
}
