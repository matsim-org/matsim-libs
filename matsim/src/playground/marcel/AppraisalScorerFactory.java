/* *********************************************************************** *
 * project: org.matsim.*
 * AppraisalScorerFactory.java
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

package playground.marcel;

import java.util.LinkedList;

import org.matsim.interfaces.core.v01.Plan;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

public class AppraisalScorerFactory implements ScoringFunctionFactory {

	public LinkedList<AppraisalScorer> scorers = new LinkedList<AppraisalScorer>();
	private final CalcPaidToll tollCalc;
	private final RoadPricingScheme toll;

	public AppraisalScorerFactory(final CalcPaidToll tollCalc, final RoadPricingScheme toll) {
		this.tollCalc = tollCalc;
		this.toll = toll;
	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
		AppraisalScorer scorer = new AppraisalScorer(plan, this.tollCalc, this.toll);
		this.scorers.add(scorer);
		return scorer;
	}

}
