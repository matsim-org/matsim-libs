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

package playground.jjoubert.Utilities;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {
		
	public static Test suite(){
		TestSuite suite = new TestSuite("Tests for playground.jjoubert.Utilities");
		
		suite.addTestSuite(MyFileSamplerTest.class);
		suite.addTestSuite(MyLinkStatsReaderTest.class);
		suite.addTestSuite(MyOsmNetworkCleanerTest.class);
		suite.addTestSuite(MyVehicleIdentifierTest.class);
		suite.addTestSuite(MyXmlConverterTest.class);
		
		suite.addTest(playground.jjoubert.Utilities.matsim2urbansim.AllTests.suite());
		suite.addTest(playground.jjoubert.Utilities.roadpricing.AllTests.suite());
		
		return suite;
	}
}
