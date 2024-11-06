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
import org.matsim.contrib.evrp.ETask;

/**
 * @author steffenaxer
 */
public class EDrtServiceTask extends DrtServiceTask implements ETask {

	private final double consumedEnergy;

	public EDrtServiceTask(Id<DrtService> drtServiceId, double beginTime, double endTime, Link link, double consumedEnergy, OperationFacility operationFacility) {
		super(drtServiceId, beginTime, endTime, link, operationFacility);
		this.consumedEnergy = consumedEnergy;
	}

	@Override
	public double getTotalEnergy() {
		return consumedEnergy;
	}
}

