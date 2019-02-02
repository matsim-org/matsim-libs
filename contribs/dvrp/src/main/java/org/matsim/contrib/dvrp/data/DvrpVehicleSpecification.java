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

package org.matsim.contrib.dvrp.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * DvrpVehicleSpecification is assumed to be immutable.
 * Changing a vehicle specification (e.g. setting a different startLinkId)
 * should be done only "between" iterations by passing a new instance to FleetSpecification.
 *
 * @author Michal Maciejewski (michalm)
 */
public interface DvrpVehicleSpecification {
	Id<DvrpVehicle> getId();

	Id<Link> getStartLinkId();

	int getCapacity();

	double getServiceBeginTime();

	double getServiceEndTime();
}
