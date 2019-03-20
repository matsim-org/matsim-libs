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

package org.matsim.contrib.dynagent;

/**
 * To simplify typical use cases, where all simulation happens only in the first and/or last step.
 * Additionally, it ensures that both methods are executed, which may not happen for typical zero-duration DynActivity
 *
 * @author Michal Maciejewski (michalm)
 */
public abstract class FirstLastSimStepDynActivity implements DynActivity {
	private final String activityType;
	private boolean beforeFirstStep = true;
	private boolean afterLastStep = false;

	public FirstLastSimStepDynActivity(String activityType) {
		this.activityType = activityType;
	}

	@Override
	public final String getActivityType() {
		return activityType;
	}

	@Override
	public final double getEndTime() {
		return afterLastStep ? END_ACTIVITY_NOW : END_ACTIVITY_LATER;
	}

	@Override
	public final void doSimStep(double now) {
		if (beforeFirstStep) {
			beforeFirstStep(now);
			beforeFirstStep = false;
		}

		simStep(now);

		if (isLastStep(now)) {
			afterLastStep(now);
			afterLastStep = true;
		}
	}

	protected abstract boolean isLastStep(double now);

	protected void beforeFirstStep(double now) {
	}

	protected void afterLastStep(double now) {
	}

	protected void simStep(double now) {
	}
}
