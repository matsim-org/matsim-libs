/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

/**
 * @author michalm
 */
public interface DrtTaskFactory {
	DrtDriveTask createDriveTask(DvrpVehicle vehicle, VrpPathWithTravelData path, DrtTaskType drtTaskType);

	DrtStopTask createStopTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link);

	DrtStayTask createStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link);

	DefaultStayTask createInitialTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link);
}
