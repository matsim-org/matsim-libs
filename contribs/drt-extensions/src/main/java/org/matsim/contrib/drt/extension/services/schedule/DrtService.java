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
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;

/**
 * @author steffenaxer
 */
public class DrtService implements Identifiable<DrtService> {
	Id<DrtService> drtServiceId;
	double scheduledStartTime;
	double scheduledEndTime;
	String serviceType;
	Id<Link> linkId;
	Id<OperationFacility> operationFacilityId;
	boolean started = false;
	boolean ended = false;

	public DrtService(Id<DrtService> drtServiceId, double scheduledStartTime, double scheduledEndTime, String serviceType, Id<Link> linkId, Id<OperationFacility> operationFacilityId) {
		this.drtServiceId = drtServiceId;
		this.scheduledStartTime = scheduledStartTime;
		this.scheduledEndTime = scheduledEndTime;
		this.serviceType = serviceType;
		this.linkId = linkId;
		this.operationFacilityId = operationFacilityId;
	}

	public void start() {
		if (!started) {
			started = true;
		} else {
			throw new IllegalStateException("Service already started!");
		}
	}

	public void end() {
		if (!ended) {
			ended = true;
		} else {
			throw new IllegalStateException("Service already ended!");
		}
	}

	public double getScheduledStartTime() {
		return scheduledStartTime;
	}

	public double getScheduledEndTime() {
		return scheduledEndTime;
	}

	public String getServiceType() {
		return serviceType;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public Id<OperationFacility> getOperationFacilityId() {
		return operationFacilityId;
	}

	@Override
	public Id<DrtService> getId() {
		return drtServiceId;
	}
}
