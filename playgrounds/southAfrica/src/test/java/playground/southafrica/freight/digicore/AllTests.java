package playground.southAfrica.freight.digicore;
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

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {
	
	public static Test suite(){
		TestSuite suite = new TestSuite("All tests for playground.southAfrica.freight.digicore");
		
		suite.addTest(playground.southAfrica.freight.digicore.algorithms.AllTests.suite());
		suite.addTest(playground.southAfrica.freight.digicore.analysis.AllTests.suite());
		suite.addTest(playground.southAfrica.freight.digicore.containers.AllTests.suite());
		suite.addTest(playground.southAfrica.freight.digicore.io.AllTests.suite());
		suite.addTest(playground.southAfrica.freight.digicore.utils.AllTests.suite());

		return suite;
	}

}
