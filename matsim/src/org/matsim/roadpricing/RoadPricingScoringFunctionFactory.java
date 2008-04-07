/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingScoringFunctionFactory.java
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

package org.matsim.roadpricing;

import org.matsim.plans.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

/**
 * Factory for {@link RoadPricingScoringFunction}s.
 *
 * @author mrieser
 */
public class RoadPricingScoringFunctionFactory implements ScoringFunctionFactory {

	private final CalcPaidToll paidToll;
	private final ScoringFunctionFactory factory;

	public RoadPricingScoringFunctionFactory(final CalcPaidToll paidToll, final ScoringFunctionFactory factory) {
		this.paidToll = paidToll;
		this.factory = factory;
	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return new RoadPricingScoringFunction(plan, this.paidToll, this.factory.getNewScoringFunction(plan));
	}

}
