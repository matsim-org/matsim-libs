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
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.services.schedule.DrtService;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * @author steffenaxer
 */
public abstract class AbstractServiceEvent extends Event {
	private final Id<DrtService> drtServiceId;
	private final String mode;
	private final String serviceType;
	private final Id<DvrpVehicle> vehicleId;
	private final Id<Link> linkId;
	private final Id<OperationFacility> operationFacilityId;

	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_SERVICE_TYPE = "serviceType";
	public static final String ATTRIBUTE_LINK_ID = "linkId";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	public static final String ATTRIBUTE_OPERATION_FACILITY_ID = "facilityId";

	public AbstractServiceEvent(Id<DrtService> drtServiceId,double time, String mode, String serviceType, Id<DvrpVehicle> vehicleId, Id<Link> linkId, Id<OperationFacility> operationFacilityId) {
        super(time);
		this.drtServiceId = drtServiceId;
		this.mode = mode;
		this.serviceType = serviceType;
		this.vehicleId = vehicleId;
		this.linkId = linkId;
		this.operationFacilityId = operationFacilityId;
	}

    public String getMode() {
        return mode;
    }

	public String getServiceType() {
		return serviceType;
	}

	public Id<DrtService> getDrtServiceId() {
		return drtServiceId;
	}

	public Id<OperationFacility> getOperationFacilityId() {
		return operationFacilityId;
	}

	public Id<DvrpVehicle> getVehicleId() {
		return vehicleId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put(ATTRIBUTE_MODE, mode);
        attr.put(ATTRIBUTE_LINK_ID, linkId.toString());
		attr.put(ATTRIBUTE_VEHICLE_ID, vehicleId.toString());
		attr.put(ATTRIBUTE_SERVICE_TYPE, serviceType);
		attr.put(ATTRIBUTE_OPERATION_FACILITY_ID, operationFacilityId.toString());
        return attr;
    }
}
