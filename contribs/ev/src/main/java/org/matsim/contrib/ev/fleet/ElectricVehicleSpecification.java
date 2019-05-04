/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.data.ElectricVehicle;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface ElectricVehicleSpecification {
	Id<ElectricVehicle> getId();

	String getVehicleType();

	ImmutableList<String> getChargerTypes();

	//FIXME consider renaming to getInitialCharge -- SOC suggest [%] not [J]
	double getInitialSoc();//[J]

	double getBatteryCapacity();//[J]
}
