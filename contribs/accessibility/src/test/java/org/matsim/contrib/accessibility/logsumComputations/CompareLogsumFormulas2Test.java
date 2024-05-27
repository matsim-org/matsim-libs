/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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

/**
 *
 */
package org.matsim.contrib.accessibility.logsumComputations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thomas
 *
 */
public class CompareLogsumFormulas2Test {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testLogsumFormulas(){

		double walkTravelTime2Network = 2.; // 2min
		double travelTimeOnNetwork = 6.;	// 6min

		double gapOpportunityA = 3.;		// 3min
		double gapOpportunityB = 5.;		// 5min
		double gapOpportunityC = 1.;		// 1min

		double betaScale = -2.;
		double betaWalk = -12;
		double betaModeX = -20;

		///////////////////

		double cik1 = betaWalk * walkTravelTime2Network + betaModeX * travelTimeOnNetwork + betaWalk * gapOpportunityA;
		double cik2 = betaWalk * walkTravelTime2Network + betaModeX * travelTimeOnNetwork + betaWalk * gapOpportunityB;
		double cik3 = betaWalk * walkTravelTime2Network + betaModeX * travelTimeOnNetwork + betaWalk * gapOpportunityC;

		double Sum1 = Math.exp(betaScale * cik1 ) + Math.exp(betaScale * cik2 ) + Math.exp(betaScale * cik3 );
		System.out.println(Sum1);
		///////////////////

		double PreFactor = Math.exp(betaScale * (betaWalk * walkTravelTime2Network + betaModeX * travelTimeOnNetwork) );
		double AggregationSum = Math.exp(betaScale * betaWalk * gapOpportunityA) + Math.exp(betaScale * betaWalk * gapOpportunityB) + Math.exp(betaScale * betaWalk * gapOpportunityC);
		double Sum2 =PreFactor * AggregationSum;
		System.out.println(Sum2);

		Assertions.assertTrue( Sum1 == Sum2 );
	}

}
