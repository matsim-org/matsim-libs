/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityScoringFunctionTest.java
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

package playground.meisterk.org.matsim.scoring.ktiYear3;

import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.PersonImpl;
import org.matsim.scoring.ScoringFunction;
import org.matsim.testcases.MatsimTestCase;

public class ActivityScoringFunctionTest extends MatsimTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testGetTimePerforming() {

		Config config = super.loadConfig(this.getInputDirectory() + "config.xml");

		KTIYear3ScoringFunctionFactory factory = new KTIYear3ScoringFunctionFactory(config.charyparNagelScoring(), null);
		
		Person person = new PersonImpl(new IdImpl("123"));
		Plan plan = person.createPlan(true);
		
		ScoringFunction ktiYear3ScoringFunction = factory.getNewScoringFunction(plan);
		
	}
	
}
