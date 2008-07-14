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

package playground.wrashid.test;

import playground.wrashid.test.test1.Test1;
import playground.wrashid.test.test2.Test2;
import playground.wrashid.test.test4.Test4;
import playground.wrashid.test.test5.Test5;
import playground.wrashid.test.test6.Test6;
import playground.wrashid.test.test6.Test6_1;
import playground.wrashid.test.test8.Test8;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.playground.wrashid.DES");
		//$JUnit-BEGIN$
		suite.addTestSuite(Test1.class);
		suite.addTestSuite(Test2.class);
		suite.addTestSuite(Test4.class);
		suite.addTestSuite(Test5.class);
		suite.addTestSuite(Test6.class);
		suite.addTestSuite(Test8.class);
		//$JUnit-END$
		return suite;
	}

}
