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
package org.matsim.contrib.drt.extension.services.services;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.services.services.triggers.*;
import org.matsim.contrib.drt.extension.services.services.params.*;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author steffenaxer
 */
public class DefaultTriggerFactoryImpl implements ServiceTriggerFactory {

	@Override
	public ServiceExecutionTrigger get(Id<DvrpVehicle> vehicleId, AbstractServiceTriggerParam param) {
		return switch (param) {
			case StopsReachedTriggerParam stopBasedConditionParam -> new StopsReachedTrigger(vehicleId, stopBasedConditionParam);
			case MileageReachedTriggerParam mileageBasedConditionParam -> new MileageReachedTrigger(vehicleId, mileageBasedConditionParam);
			case ChargingStartedTriggerParam chargingBasedConditionParam -> new ChargingStartedTrigger(vehicleId,chargingBasedConditionParam);
			case TimeOfDayReachedTriggerParam timeOfDayBasedConditionParam -> new TimeOfDayReachedTrigger(vehicleId, timeOfDayBasedConditionParam);
			default -> throw new IllegalStateException("JobConditionFactory missing for MaintenanceParam " + param.getName());
		};
	}
}
