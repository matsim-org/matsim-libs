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

import java.util.Arrays;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatConfigGroupTest extends MatsimTestCase {

	public void testPlanomatConfigGroup() {
		
		super.loadConfig(this.getInputDirectory() + "empty_config.xml");
		
//		assertEquals( PlanomatConfigGroup.DEFAULT_OPTIMIZATION_TOOLBOX, Gbl.getConfig().planomat().getOptimizationToolbox() );
		assertEquals( 
				Integer.parseInt(PlanomatConfigGroup.PlanomatConfigParameter.POPSIZE.getDefaultValue()), 
				Gbl.getConfig().planomat().getPopSize() );
		assertEquals( 
				Integer.parseInt(PlanomatConfigGroup.PlanomatConfigParameter.JGAP_MAX_GENERATIONS.getDefaultValue()), 
				Gbl.getConfig().planomat().getJgapMaxGenerations() );
		assertTrue(
				Arrays.deepEquals(
						new BasicLeg.Mode[]{}, 
						Gbl.getConfig().planomat().getPossibleModes()));
		assertEquals( 
				PlanomatConfigGroup.PlanomatConfigParameter.LEG_TRAVEL_TIME_ESTIMATOR_NAME.getDefaultValue(), 
				Gbl.getConfig().planomat().getLegTravelTimeEstimatorName() );
		assertEquals( 
				Integer.parseInt(PlanomatConfigGroup.PlanomatConfigParameter.LEVEL_OF_TIME_RESOLUTION.getDefaultValue()), 
				Gbl.getConfig().planomat().getLevelOfTimeResolution());
		assertEquals( 
				Boolean.parseBoolean(PlanomatConfigGroup.PlanomatConfigParameter.DO_LOGGING.getDefaultValue()), 
				Gbl.getConfig().planomat().isDoLogging() );
	}

	public void testAddParam() {

		super.loadConfig(this.getInputDirectory() + "config.xml");

		assertEquals( 10, Gbl.getConfig().planomat().getPopSize() );
		assertTrue(Arrays.deepEquals(
				new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.pt}, 
				Gbl.getConfig().planomat().getPossibleModes()));
		assertEquals( PlanomatConfigGroup.CHARYPAR_ET_AL_COMPATIBLE, Gbl.getConfig().planomat().getLegTravelTimeEstimatorName() );
		assertEquals( 1000, Gbl.getConfig().planomat().getJgapMaxGenerations() );
		assertEquals( 6, Gbl.getConfig().planomat().getLevelOfTimeResolution() );
		assertEquals( true, Gbl.getConfig().planomat().isDoLogging() );
	}
	
}
