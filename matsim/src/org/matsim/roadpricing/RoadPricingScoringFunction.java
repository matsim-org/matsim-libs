/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingScoringFunction.java
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
import org.matsim.scoring.CharyparNagelScoringFunction;

public class RoadPricingScoringFunction extends CharyparNagelScoringFunction {

	CalcPaidToll paidToll = null;

	public RoadPricingScoringFunction(final Plan plan, final CalcPaidToll paidToll) {
		super(plan);
		this.paidToll = paidToll;
	}

	@Override
	public void finish() {
		double toll = this.paidToll.getAgentToll(this.person.getId().toString());
		this.score -= toll;

		super.finish();
	}

}
