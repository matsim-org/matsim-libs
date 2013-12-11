/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimesTest.java
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

package playground.vsp.zzArchive.bvwpOld;


import org.matsim.testcases.MatsimTestCase;

import playground.vsp.zzArchive.bvwpOld.EconomicValues;
import playground.vsp.zzArchive.bvwpOld.ScenarioForEvalData;
import playground.vsp.zzArchive.bvwpOld.ScenarioForTest1;
import playground.vsp.zzArchive.bvwpOld.UtilityChangesRuleOfHalf;
import playground.vsp.zzArchive.bvwpOld.Values;



public class BvwpTest extends MatsimTestCase {
	
	public void testOne() {
		
		Values economicValues = EconomicValues.createEconomicValuesForTest1();
		
		ScenarioForEvalData nullfall = ScenarioForTest1.createNullfallForTest();
		
		ScenarioForEvalData planfall = ScenarioForTest1.createPlanfallForTest(nullfall);
		
		new UtilityChangesRuleOfHalf().computeAndPrintResults(economicValues, nullfall, planfall) ;
		
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
