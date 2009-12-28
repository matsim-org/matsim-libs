/* *********************************************************************** *
 * project: org.matsim.*
 * ReadEvents.java
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
 * *********************************************************************** */package playground.wisinee.ipftest;

import junit.framework.TestCase;
import playground.wisinee.IPF.*;

public class RunIpfTest extends TestCase {

	private final static String testPropertyFile = "./test/scenarios/ipf/TestParameter.xml";	
	
	public void testRunIpfCal() {
		RunIPF ipftest = new RunIPF();
		ipftest.runIpfCal(testPropertyFile);
		assertEquals(2,ipftest.nz);
		assertEquals(3,ipftest.nx);
		assertEquals("Income",ipftest.heading);
	}

}
