/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author michalm
 * @author nkuehnel / MOIA, add elapsed and remaining idle times
 */
public interface ScheduleInquiry {

	/**
	 * Baseline notion of idle (no thresholds).
	 */
	boolean isIdle(DvrpVehicle vehicle);

	/**
	 * Idle with explicit elapsed/remaining thresholds.
	 */
	boolean isIdle(DvrpVehicle vehicle, IdleCriteria criteria);

	record IdleCriteria(double minIdleElapsed, double minIdleGapRemaining) {
		public static IdleCriteria none() { return new IdleCriteria(0, 0); }
	}
}
