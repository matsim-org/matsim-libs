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
package org.matsim.contrib.drt.extension.services.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.services.schedule.DrtService;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * @author steffenaxer
 */
public class DrtServiceScheduledEvent extends Event {

	private final double startTime;
	private final double endTime;
	private final String mode;
	private final String serviceType;
	private final Id<DvrpVehicle> vehicleId;
	private final Id<Link> linkId;
	private final Id<OperationFacility> operationFacilityId;
	private final Id<DrtService> drtServiceId;

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_OPERATION_FACILITY = "operationFacility";
	public static final String ATTRIBUTE_SERVICE_TYPE = "serviceType";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicle";
	public static final String ATTRIBUTE_START_TIME = "startTime";
	public static final String ATTRIBUTE_END_TIME = "endTime";
	public static final String EVENT_TYPE = "DRT service scheduled";


	public DrtServiceScheduledEvent(Id<DrtService> drtServiceId, double time, double startTime, double endTime, String mode, String serviceType, Id<DvrpVehicle> vehicleId,
									Id<Link> linkId, Id<OperationFacility> operationFacilityId) {
		super(time);
		this.drtServiceId = drtServiceId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.mode = mode;
		this.serviceType = serviceType;
		this.vehicleId = vehicleId;
		this.linkId = linkId;
		this.operationFacilityId = operationFacilityId;
	}

	public Id<DrtService> getDrtServiceId() {
		return drtServiceId;
	}

	public Id<DvrpVehicle> getVehicleId() {
		return vehicleId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public String getMode() {
		return mode;
	}

	public String getServiceType() {
		return serviceType;
	}

	public double getEndTime() {
		return endTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public Id<OperationFacility> getOperationFacilityId() {
		return operationFacilityId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE_ID, vehicleId + "");
		attr.put(ATTRIBUTE_LINK, linkId + "");
		attr.put(ATTRIBUTE_OPERATION_FACILITY, operationFacilityId + "");
		attr.put(ATTRIBUTE_SERVICE_TYPE, serviceType);
		attr.put(ATTRIBUTE_START_TIME,startTime + "");
		attr.put(ATTRIBUTE_END_TIME,endTime + "");
		return attr;
	}
}
