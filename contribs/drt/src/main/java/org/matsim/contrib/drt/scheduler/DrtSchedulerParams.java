/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.scheduler;

import org.matsim.contrib.drt.run.DrtConfigGroup;

/**
 * @author michalm
 */
public class DrtSchedulerParams {
	public final double stopDuration;

	public DrtSchedulerParams(DrtConfigGroup drtCfg) {
		this.stopDuration = drtCfg.getStopDuration();
	}

	public DrtSchedulerParams(double stopDuration) {
		this.stopDuration = stopDuration;
	}
}
