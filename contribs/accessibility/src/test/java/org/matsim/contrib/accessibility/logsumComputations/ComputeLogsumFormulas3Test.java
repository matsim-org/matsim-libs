/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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
public class ComputeLogsumFormulas3Test {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * underlying network
	 * 						  cjk1
	 * 	  origin_h			 /
	 * 	   \				/
	 * 		i--------------j--cjk2
	 * 					    \
	 * 						 \
	 *						  cjk3
	 */
	@SuppressWarnings("static-method")

	@Test
	void testLogsumFormulas(){
		double betaWalkTT = -10. / 3600.0;	// [util/sec]
		double betaWalkTD = -10.;			// [util/meter]

		double betaCarTT  = -12 / 3600.0;	// [util/sec]
		double betaCarTD  = -12;			// [util/meter]
		double betaCarTMC = -1;				// [util/money]

		// travel time costs (h)
		double cijTT = 1.5 / 60.; 		// time to reach j from i
		double chiTT = 1.2 / 60.;		// time to reach network from origin
//		double cjk1TT= 1.5 / 60.;		// time to reach cjk1 from j
//		double cjk2TT= 1.2 / 60.;		// time to reach cjk2 from j
//		double cjk3TT= 1.4 / 60.;		// time to reach cjk3 from j

		// travel distance (m)
		double cijTD = 500.;	// distance to reach j from i
		double chiTD = 20.;		// distance to reach network from origin
//		double cjk1TD= 80.;		// distance to reach cjk1 from j
//		double cjk2TD= 30.;		// distance to reach cjk2 from j
//		double cjk3TD= 50.;		// distance to reach cjk3 from j

		// travel monetary cost / toll (money)
		double cijTMC= 10;		// toll to get from i to j

		///////
		// opportunities
		///////
//		double sumExpVjk = Math.exp( (cjk1TT * betaWalkTT) + (cjk1TD * betaWalkTD) );
//		sumExpVjk 		+= Math.exp( (cjk2TT * betaWalkTT) + (cjk2TD * betaWalkTD) );
//		sumExpVjk 		+= Math.exp( (cjk3TT * betaWalkTT) + (cjk3TD * betaWalkTD) );

		///////
		// OLD
		///////
		double VhjOldTT = (cijTT * betaCarTT) + (chiTT * betaWalkTT);
		double VhjOldTD = (cijTD * betaCarTD) + (chiTD * betaWalkTD);
		double VhjOldTMC= cijTMC * betaCarTMC + 0;

		double VhjOld = VhjOldTT + VhjOldTD + VhjOldTMC;
//		double expOldVhj = Math.exp( VhjOld );
//		double expOldVhk = expOldVhj * sumExpVjk;

		///////
		// NEW
		///////
		double VijCar = (cijTT * betaCarTT) + (cijTD * betaCarTD) + (cijTMC * betaCarTMC);
		double VhiWalk= (chiTT * betaWalkTT) + (chiTD * betaWalkTD);
		double VhjNew = VijCar + VhiWalk;
//		double expNewVhj= Math.exp( VhjNew );
//		double expNewVhk= expNewVhj * sumExpVjk;

		Assertions.assertTrue(VhjOld == VhjNew);	// old accessibility computation == new accessibility computation

		///////
		// NEW
		///////

		double dummyVijCar = -0.9123;
		double dummyVhiWalk= -0.023;

		double dummyExp1 = Math.exp( dummyVijCar + dummyVhiWalk );
		double dummyExp2 = Math.exp( dummyVijCar ) * Math.exp( dummyVhiWalk );

		Assertions.assertEquals(dummyExp1,dummyExp2,1.e-10);	// exp(VijCar + VijWalk) == exp(VijCar) * exp(VijWalk)
	}

}
