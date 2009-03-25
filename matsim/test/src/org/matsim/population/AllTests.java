/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
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

package org.matsim.population;

import org.matsim.core.population.ActivityTest;
import org.matsim.core.population.PersonTest;
import org.matsim.core.population.PlanTest;
import org.matsim.core.population.PopulationReaderMatsimV4Test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for org.matsim.population");

		suite.addTestSuite(ActivityTest.class);
		suite.addTestSuite(DesiresTest.class);
		suite.addTestSuite(KnowledgeTest.class);
		suite.addTestSuite(PersonTest.class);
		suite.addTestSuite(PlanTest.class);
		suite.addTestSuite(PopulationReaderMatsimV4Test.class);
		suite.addTest(org.matsim.population.algorithms.AllTests.suite());
		suite.addTest(org.matsim.population.filters.AllTests.suite());
		suite.addTest(org.matsim.core.population.routes.AllTests.suite());

		return suite;
	}

}
