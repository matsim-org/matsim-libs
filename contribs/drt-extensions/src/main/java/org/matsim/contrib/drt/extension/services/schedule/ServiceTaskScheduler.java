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
package org.matsim.contrib.drt.extension.services.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.services.services.params.DrtServiceParams;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;

/**
 * @author steffenaxer
 */
public interface ServiceTaskScheduler {
	DrtTaskType RELOCATE_SERVICE_TASK_TYPE = new DrtTaskType("RELOCATE_SERVICE", DRIVE);
	void scheduleServiceTask(DvrpVehicle vehicle, OperationFacility operationFacility, DrtServiceParams drtServiceParams, boolean enableTaskStacking);
	Map<Id<DrtService>,DrtService> getScheduledServices(Id<DvrpVehicle> dvrpVehicleId);
	Map<Id<DrtService>,DrtService> getStartedServices(Id<DvrpVehicle> dvrpVehicleId);
	void startService(Id<DvrpVehicle> dvrpVehicleId, Id<DrtService> drtServiceId);
	void stopService(Id<DvrpVehicle> dvrpVehicleId, Id<DrtService> drtServiceId);
}
