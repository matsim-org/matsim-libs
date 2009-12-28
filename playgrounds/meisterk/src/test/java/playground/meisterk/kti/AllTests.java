/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
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

package playground.meisterk.kti;

import junit.framework.Test;
import junit.framework.TestSuite;
import playground.meisterk.kti.config.KtiConfigGroupTest;
import playground.meisterk.kti.router.KtiPtRouteTest;
import playground.meisterk.kti.router.PlansCalcRouteKtiTest;
import playground.meisterk.kti.scoring.ActivityScoringFunctionTest;
import playground.meisterk.kti.scoring.LegScoringFunctionTest;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.meisterk.kti");
		//$JUnit-BEGIN$
		suite.addTestSuite(KtiConfigGroupTest.class);
//		suite.addTestSuite(KtiControlerTest.class);
		suite.addTestSuite(ActivityScoringFunctionTest.class);
		suite.addTestSuite(LegScoringFunctionTest.class);
		suite.addTestSuite(PlansCalcRouteKtiTest.class);
		suite.addTestSuite(KtiPtRouteTest.class);
		//$JUnit-END$
		return suite;
	}


}
