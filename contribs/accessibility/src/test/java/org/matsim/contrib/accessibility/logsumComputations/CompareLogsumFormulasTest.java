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
public class CompareLogsumFormulasTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * underlying network
	 * 						  cjk1
	 * 						 /
	 * 						/
	 * 		i--------------j--cjk2
	 * 					    \
	 * 						 \
	 *						  cjk3
	 */

	@Test
	void testLogsumFormulas(){
		double betaWalkTT = -2.;
		double betaWalkTD = -1.;

		// travel time costs (min)
		double cijTT = 10.; 	// time to reach j from i
		double cjk1TT= 5.;		// time to reach cjk1 from j
		double cjk2TT= 2.;		// time to reach cjk2 from j
		double cjk3TT= 4.;		// time to reach cjk3 from j
		double cik1TT= cijTT + cjk1TT;
		double cik2TT= cijTT + cjk2TT;
		double cik3TT= cijTT + cjk3TT;

		// travel distance costs (m)
		double cijTD = 500.;	// distance to reach j from i
		double cjk1TD= 80.;		// distance to reach cjk1 from j
		double cjk2TD= 30.;		// distance to reach cjk2 from j
		double cjk3TD= 50.;		// distance to reach cjk3 from j
		double cik1TD= cijTD + cjk1TD;
		double cik2TD= cijTD + cjk2TD;
		double cik3TD= cijTD + cjk3TD;

		double Ai = computeLogsum(betaWalkTT, betaWalkTD, cik1TT, cik2TT, cik3TT, cik1TD, cik2TD, cik3TD);
		double Ai2 =computeTransformedLogsum(betaWalkTT, betaWalkTD, cijTT, cjk1TT, cjk2TT, cjk3TT, cijTD, cjk1TD, cjk2TD, cjk3TD);

		Assertions.assertTrue( Ai == Ai2 );
	}

	/**
	 * Now the logsum is computed in 2 steps:
	 * 1) sum the disutilities of traveling (e^(Vjk)) from the destination node j to all opportunities k (that are attached to j)
	 * 2) compute the disutility of traveling to get from the origin i to the destination node j (e^(Vij))
	 * 3) multiply 1) and 2) and take the logarithm.
	 *
	 * @param betaWalkTT
	 * @param betaWalkTD
	 * @param cijTT
	 * @param cjk1TT
	 * @param cjk2TT
	 * @param cjk3TT
	 * @param cijTD
	 * @param cjk1TD
	 * @param cjk2TD
	 * @param cjk3TD
	 */
	private double computeTransformedLogsum(double betaWalkTT,
			double betaWalkTD, double cijTT, double cjk1TT, double cjk2TT,
			double cjk3TT, double cijTD, double cjk1TD, double cjk2TD,
			double cjk3TD) {
		// transformed Logsum
		double Vij = betaWalkTT * cijTT + betaWalkTD * cijTD;
		double Vjk1= betaWalkTT * cjk1TT + betaWalkTD * cjk1TD;
		double Vjk2= betaWalkTT * cjk2TT + betaWalkTD * cjk2TD;
		double Vjk3= betaWalkTT * cjk3TT + betaWalkTD * cjk3TD;
		double Sumjk= Math.exp(Vjk1) + Math.exp(Vjk2) + Math.exp(Vjk3);

		double Ai2= Math.log( Math.exp(Vij) * Sumjk);
		System.out.println(Ai2);
		return Ai2;
	}

	/**
	 * Calculates the logsum by taking log sum of the disutilities of traveling from the origin i to each opportunity k
	 *
	 * @param betaWalkTT
	 * @param betaWalkTD
	 * @param cik1TT
	 * @param cik2TT
	 * @param cik3TT
	 * @param cik1TD
	 * @param cik2TD
	 * @param cik3TD
	 */
	private double computeLogsum(double betaWalkTT, double betaWalkTD,
			double cik1TT, double cik2TT, double cik3TT, double cik1TD,
			double cik2TD, double cik3TD) {
		// Logsum
		double Vik1 = betaWalkTT * cik1TT + betaWalkTD * cik1TD;
		double Vik2 = betaWalkTT * cik2TT + betaWalkTD * cik2TD;
		double Vik3 = betaWalkTT * cik3TT + betaWalkTD * cik3TD;

		double Ai = Math.log( Math.exp(Vik1) + Math.exp(Vik2) + Math.exp(Vik3));
		System.out.println(Ai);
		return Ai;
	}
}
