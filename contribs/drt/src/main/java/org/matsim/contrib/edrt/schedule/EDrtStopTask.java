/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.edrt.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtStopTask;

/**
 * @author michalm
 */
public class EDrtStopTask extends DrtStopTask implements EDrtTask {
	private final double consumedEnergy;

	public EDrtStopTask(double beginTime, double endTime, Link link, double consumedEnergy) {
		super(beginTime, endTime, link);
		this.consumedEnergy = consumedEnergy;
	}

	@Override
	public double getTotalEnergy() {
		return consumedEnergy;
	}
}
