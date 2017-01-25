/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.algorithm.scheduler;

import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;

/**
 * @author  jbischoff
 *
 */
public class TaxibusSchedulerParams extends TaxiSchedulerParams {

	public TaxibusSchedulerParams(double pickupDuration,
			double dropoffDuration) {
		super(true, false, pickupDuration, dropoffDuration, 1.0);
		//		We assume we a) know where we are heading to and b) do not allow diversions once a bus is running
		
	}

}
