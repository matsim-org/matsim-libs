/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.lightsignalsystems;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author dgrether
 */
public class AllTests {

	public static Test suite() {

		TestSuite suite = new TestSuite("Tests for org.matsim.lightsignalsystems");
		//$JUnit-BEGIN$
		suite.addTestSuite(LightSignalSystemsReaderTest.class);
		suite.addTestSuite(LightSignalSystemsConfigReaderTest.class);
		suite.addTestSuite(CalculateAngleTest.class);
		suite.addTestSuite(SignalSystemBasicsTest.class);
		suite.addTestSuite(TravelTimeTestOneWay.class);
		suite.addTestSuite(TravelTimeTestFourWays.class);
		//$JUnit-END$
		return suite;
	}

}
