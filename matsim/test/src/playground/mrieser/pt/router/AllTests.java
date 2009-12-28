/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.router;

import junit.framework.Test;
import junit.framework.TestSuite;

public abstract class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());

		suite.addTestSuite(MultiNodeDijkstraTest.class);
		suite.addTestSuite(TransitActsRemoverTest.class);
		suite.addTestSuite(TransitRouterNetworkTravelTimeCostTest.class);
		suite.addTestSuite(TransitRouterTest.class);
		suite.addTestSuite(PlansCalcTransitRouteTest.class);

		return suite;
	}

}
