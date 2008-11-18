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

package org.matsim.events;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.matsim.events");

		suite.addTest(org.matsim.events.algorithms.AllTests.suite());
		suite.addTestSuite(ActEndEventTest.class);
		suite.addTestSuite(ActStartEventTest.class);
		suite.addTestSuite(AgentArrivalEventTest.class);
		suite.addTestSuite(AgentDepartureEventTest.class);
		suite.addTestSuite(AgentStuckEventTest.class);
		suite.addTestSuite(AgentMoneyEventTest.class);
		suite.addTestSuite(AgentWait2LinkEventTest.class);
		suite.addTestSuite(LinkEnterEventTest.class);
		suite.addTestSuite(LinkLeaveEventTest.class);
		suite.addTestSuite(EventsHandlerHierarchy.class);
		suite.addTestSuite(EventsReadersTest.class);

		return suite;
	}

}
