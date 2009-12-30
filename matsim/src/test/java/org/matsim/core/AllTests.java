/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
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

package org.matsim.core;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());
		suite.addTest(org.matsim.core.basic.v01.AllTests.suite());
		suite.addTest(org.matsim.core.config.AllTests.suite());
		suite.addTest(org.matsim.core.controler.AllTests.suite());
		suite.addTest(org.matsim.core.events.AllTests.suite());
		suite.addTest(org.matsim.core.facilities.AllTests.suite());
		suite.addTest(org.matsim.core.gbl.AllTests.suite());
		suite.addTest(org.matsim.core.mobsim.AllTests.suite());
		suite.addTest(org.matsim.core.network.AllTests.suite());
		suite.addTest(org.matsim.core.population.AllTests.suite());
		suite.addTest(org.matsim.core.replanning.AllTests.suite());
		suite.addTest(org.matsim.core.router.AllTests.suite());
		suite.addTest(org.matsim.core.scoring.AllTests.suite());
		suite.addTest(org.matsim.core.trafficmonitoring.AllTests.suite());
		suite.addTest(org.matsim.core.utils.AllTests.suite());
		
		return suite;
	}

}
