/* *********************************************************************** *
 * project: org.matsim.*
 * MyLSATests
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
package playground.dgrether;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * @author dgrether
 *
 */
public class MyLSATests {
	public static Test suite() {
//		QueueNetwork.setSimulateAllNodes(true);
		TestSuite suite = new TestSuite("Tests for lsa implementation");

//		suite.addTestSuite(TravelTimeTestOneWay.class);
//		QueueNetwork.setSimulateAllNodes(true);
		//$JUnit-BEGIN$
		suite.addTest(org.matsim.mobsim.queuesim.AllTests.suite());
//		suite.addTest(org.matsim.integration.AllTests.suite());
		suite.addTestSuite(org.matsim.integration.EquilTwoAgentsTest.class);
//		suite.addTest(org.matsim.examples.AllTests.suite());
//		suite.addTest(org.matsim.replanning.AllTests.suite());

//		suite.addTestSuite(LightSignalSystemsReaderTest.class);
//		suite.addTestSuite(LightSignalSystemsConfigReaderTest.class);
//		suite.addTestSuite(CalculateAngleTest.class);
//		suite.addTestSuite(TravelTimeTestFourWays.class);
		//$JUnit-END$
		return suite;
	}

}
