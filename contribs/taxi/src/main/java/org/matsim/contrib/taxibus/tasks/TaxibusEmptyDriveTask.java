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

package org.matsim.contrib.taxibus.tasks;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTaskImpl;

/**
 * Task for driving w/o pax
 * 
 * @author jbischoff
 */
public class TaxibusEmptyDriveTask extends DriveTaskImpl implements TaxibusTask {
	public TaxibusEmptyDriveTask(VrpPathWithTravelData path) {
		super(path);
	}

	@Override
	public TaxibusTaskType getTaxibusTaskType() {
		return TaxibusTaskType.DRIVE_EMPTY;
	}

	@Override
	protected String commonToString() {
		return "[" + getTaxibusTaskType().name() + "]" + super.commonToString();
	}
}
