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
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

/**
 * @author michalm
 */
public class DrtTaskFactoryImpl implements DrtTaskFactory {
	@Override
	public DrtDriveTask createDriveTask(Vehicle vehicle, VrpPathWithTravelData path) {
		return new DrtDriveTask(path);
	}

	@Override
	public DrtStopTask createStopTask(Vehicle vehicle, double beginTime, double endTime, Link link) {
		return new DrtStopTask(beginTime, endTime, link);
	}

	@Override
	public DrtStayTask createStayTask(Vehicle vehicle, double beginTime, double endTime, Link link) {
		return new DrtStayTask(beginTime, endTime, link);
	}
}
