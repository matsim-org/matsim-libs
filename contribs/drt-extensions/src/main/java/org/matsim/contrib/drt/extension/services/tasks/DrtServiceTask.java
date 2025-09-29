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
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.extension.services.schedule.DrtService;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.contrib.evrp.ChargingTask;

import java.util.Optional;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;

/**
 * @author steffenaxer
 */
public class DrtServiceTask extends DefaultStayTask implements OperationalStop {

	public static final DrtTaskType TYPE = new DrtTaskType("SERVICE", STAY);
	Id<OperationFacility> facilityId;
	Id<DrtService> drtServiceId;
	final double intendedDuration;

	public DrtServiceTask(Id<DrtService> drtServiceId, double beginTime, double endTime, Link link, OperationFacility facility) {
		super(TYPE,beginTime, endTime, link);
		this.facilityId = facility.getId();
		this.drtServiceId = drtServiceId;
		this.intendedDuration = endTime-beginTime;
	}

	public Id<DrtService> getDrtServiceId() {
		return drtServiceId;
	}

	public double getIntendedDuration() {
		return intendedDuration;
	}

	@Override
	public Id<OperationFacility> getFacilityId() {
		return facilityId;
	}

	@Override
	public Optional<Id<ReservationManager.Reservation>> getReservationId() {
		return Optional.empty();
	}

	@Override
	public Optional<ChargingTask> getChargingTask() {
		return Optional.empty();
	}

	@Override
	public boolean addCharging(ChargingTask chargingTask) {
		return false;
	}
}

