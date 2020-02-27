/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.etaxi.util;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.etaxi.ETaxiChargingTask;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiStayTaskEndTimeUpdater;

public class ETaxiStayTaskEndTimeUpdater extends TaxiStayTaskEndTimeUpdater {
	public ETaxiStayTaskEndTimeUpdater(TaxiConfigGroup taxiConfigGroup) {
		super(taxiConfigGroup);
	}

	@Override
	public double calcNewEndTime(DvrpVehicle vehicle, Task task, double newBeginTime) {
		if (task instanceof ETaxiChargingTask) {
			// FIXME underestimated due to the ongoing AUX/drive consumption
			double duration = task.getEndTime() - task.getBeginTime();
			return newBeginTime + duration;
		} else {
			return super.calcNewEndTime(vehicle, task, newBeginTime);
		}
	}

}
