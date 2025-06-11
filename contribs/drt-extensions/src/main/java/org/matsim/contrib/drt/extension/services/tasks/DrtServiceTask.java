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

import com.google.common.base.Verify;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.extension.services.schedule.DrtService;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;

/**
 * @author steffenaxer
 */
public class DrtServiceTask extends DefaultStayTask implements OperationalStop {

	public static final DrtTaskType TYPE = new DrtTaskType("SERVICE", STAY);
	OperationFacility operationFacility;
	Id<DrtService> drtServiceId;
	final double intendedDuration;

	public DrtServiceTask(Id<DrtService> drtServiceId, double beginTime, double endTime, Link link, OperationFacility operationFacility) {
		super(TYPE,beginTime, endTime, link);
		this.operationFacility = operationFacility;
		this.drtServiceId = drtServiceId;
		this.intendedDuration = endTime-beginTime;
		Verify.verify(link.getId().equals(operationFacility.getLinkId()));
	}

	@Override
	public OperationFacility getFacility() {
		return operationFacility;
	}

	public Id<DrtService> getDrtServiceId() {
		return drtServiceId;
	}

	public double getIntendedDuration() {
		return intendedDuration;
	}
}

