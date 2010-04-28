/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractLegTravelTimeEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.planomat.costestimators;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;

public abstract class AbstractLegTravelTimeEstimator implements
		LegTravelTimeEstimator {

	protected Plan plan;

	public AbstractLegTravelTimeEstimator(Plan plan) {
		super();
		this.plan = plan;
	}

	@Override
	public abstract double getLegTravelTimeEstimation(Id personId, double departureTime,
			Activity actOrigin, Activity actDestination,
			Leg legIntermediate, boolean doModifyLeg);

	@Override
	public abstract LegImpl getNewLeg(
			TransportMode mode,
			Activity actOrigin,
			Activity actDestination,
			int legPlanElementIndex,
			double departureTime);

//	public abstract void initPlanSpecificInformation(PlanImpl plan);
//
//	public abstract void resetPlanSpecificInformation();
}
