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

package org.matsim.network;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {

		TestSuite suite = new TestSuite("Tests for org.matsim.network");
		//$JUnit-BEGIN$
		suite.addTestSuite(KmlNetworkWriterTest.class);
		suite.addTestSuite(LinkImplTest.class);
		suite.addTestSuite(NetworkChangeEventsParserWriterTest.class);
		suite.addTestSuite(NetworkFactoryTest.class);
		suite.addTestSuite(NetworkParserWriterTest.class);
		suite.addTestSuite(TimeVariantLinkImplTest.class);
		suite.addTest(org.matsim.network.algorithms.AllTests.suite());
		//$JUnit-END$
		return suite;
	}

}
