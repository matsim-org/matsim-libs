/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatConfigGroupTest.java
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

package org.matsim.config.groups;

import org.matsim.gbl.Gbl;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatConfigGroupTest extends MatsimTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testPlanomatConfigGroup() {
		
		super.loadConfig(this.getInputDirectory() + "empty_config.xml");
		
		assertEquals( PlanomatConfigGroup.DEFAULT_OPTIMIZATION_TOOLBOX, Gbl.getConfig().planomat().getOptimizationToolbox() );
		assertEquals( CharyparNagelScoringFunctionFactory.class, Gbl.getConfig().planomat().getScoringFunctionFactory().getClass() );
		assertEquals( PlanomatConfigGroup.DEFAULT_POPSIZE, Gbl.getConfig().planomat().getPopSize() );
		assertEquals( PlanomatConfigGroup.DEFAULT_JGAP_MAX_GENERATIONS, Gbl.getConfig().planomat().getJgapMaxGenerations() );
		String actualPossibleModesString = "";
		for (String expectedPossibleMode : Gbl.getConfig().planomat().getPossibleModes()) {
			actualPossibleModesString += expectedPossibleMode;
			actualPossibleModesString += " ";
		}
		actualPossibleModesString = actualPossibleModesString.substring(0, actualPossibleModesString.length() - 1);
		assertEquals( PlanomatConfigGroup.DEFAULT_POSSIBLE_MODES, actualPossibleModesString );
		assertEquals( PlanomatConfigGroup.DEFAULT_LINK_TRAVEL_TIME_ESTIMATOR, Gbl.getConfig().planomat().getLinkTravelTimeEstimatorName());
		assertEquals( PlanomatConfigGroup.DEFAULT_LEG_TRAVEL_TIME_ESTIMATOR, Gbl.getConfig().planomat().getLegTravelTimeEstimatorName() );
	}

}
