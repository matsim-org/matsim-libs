/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent;

import java.util.function.DoubleSupplier;

/**
 * endTime is not subject to change
 *
 * @author michalm
 */
public final class IdleDynActivity extends AbstractDynActivity {
	private final DoubleSupplier endTimeSupplier;

	public IdleDynActivity(String activityType, double endTime) {
		this(activityType, () -> endTime);
	}

	public IdleDynActivity(String activityType, DoubleSupplier endTimeSupplier) {
		super(activityType);
		this.endTimeSupplier = endTimeSupplier;
	}

	@Override
	public final double getEndTime() {
		return endTimeSupplier.getAsDouble();
	}

	@Override
	public final void doSimStep(double now) {
	}
}
