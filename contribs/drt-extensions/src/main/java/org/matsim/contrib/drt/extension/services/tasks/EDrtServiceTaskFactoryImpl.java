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
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.services.schedule.DrtService;

/**
 * @author steffenaxer
 */
public class EDrtServiceTaskFactoryImpl extends EDrtTaskFactoryImpl implements ServiceTaskFactory {

	@Override
	public DrtServiceTask createServiceTask(Id<DrtService> drtServiceId, double beginTime, double endTime, Link link, OperationFacility operationFacility) {
		return new EDrtServiceTask(drtServiceId, beginTime,endTime,link,0, operationFacility);
	}

}
