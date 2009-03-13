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

package org.matsim.signalsystems;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author dgrether
 */
public class AllTests {

	public static Test suite() {

		TestSuite suite = new TestSuite("Tests for org.matsim.signalsystems");
		//$JUnit-BEGIN$
		suite.addTestSuite(SignalSystemsReaderWriterTest.class);
		suite.addTestSuite(SignalSystemsConfigReaderTest.class);
		suite.addTestSuite(CalculateAngleTest.class);
		suite.addTestSuite(SignalSystemsOneAgentTest.class);
		suite.addTestSuite(TravelTimeOneWayTest.class);
		suite.addTestSuite(TravelTimeFourWaysTest.class);
		//$JUnit-END$
		return suite;
	}

}
