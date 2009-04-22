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

package org.matsim.core.config.groups;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatConfigGroupTest extends MatsimTestCase {

	public void testPlanomatConfigGroup() {
		
		super.loadConfig(this.getInputDirectory() + "empty_config.xml");
		
		PlanomatConfigGroup expectedConfig = Gbl.getConfig().planomat();
		
//		assertEquals( PlanomatConfigGroup.DEFAULT_OPTIMIZATION_TOOLBOX, Gbl.getConfig().planomat().getOptimizationToolbox() );
		assertEquals( 
				Integer.parseInt(PlanomatConfigGroup.PlanomatConfigParameter.POPSIZE.getDefaultValue()), 
				expectedConfig.getPopSize() );
		assertEquals( 
				Integer.parseInt(PlanomatConfigGroup.PlanomatConfigParameter.JGAP_MAX_GENERATIONS.getDefaultValue()), 
				expectedConfig.getJgapMaxGenerations() );
		assertEquals(0, expectedConfig.getPossibleModes().toArray().length);
		assertEquals( 
				PlanomatConfigGroup.PlanomatConfigParameter.LEG_TRAVEL_TIME_ESTIMATOR_NAME.getDefaultValue(), 
				expectedConfig.getLegTravelTimeEstimatorName() );
		assertEquals( 
				Integer.parseInt(PlanomatConfigGroup.PlanomatConfigParameter.LEVEL_OF_TIME_RESOLUTION.getDefaultValue()), 
				expectedConfig.getLevelOfTimeResolution());
		assertEquals( 
				Boolean.parseBoolean(PlanomatConfigGroup.PlanomatConfigParameter.DO_LOGGING.getDefaultValue()), 
				expectedConfig.isDoLogging() );
		assertEquals(
				PlanomatConfigGroup.TripStructureAnalysisLayerOption.valueOf(PlanomatConfigGroup.PlanomatConfigParameter.TRIP_STRUCTURE_ANALYSIS_LAYER.getDefaultValue()),
				expectedConfig.getTripStructureAnalysisLayer());
	}

	public void testAddParam() {

		super.loadConfig(this.getInputDirectory() + "config.xml");

		PlanomatConfigGroup expectedConfig = Gbl.getConfig().planomat();

		assertEquals( 10, expectedConfig.getPopSize() );
		
		assertEquals(2, expectedConfig.getPossibleModes().toArray().length);
		assertEquals(TransportMode.car, expectedConfig.getPossibleModes().toArray()[0]);
		assertEquals(TransportMode.pt, expectedConfig.getPossibleModes().toArray()[1]);
//		
//		assertTrue(Arrays.deepEquals(
//				new TransportMode[]{TransportMode.car, TransportMode.pt}, 
//				Gbl.getConfig().planomat().getPossibleModes()));
		assertEquals( PlanomatConfigGroup.CHARYPAR_ET_AL_COMPATIBLE, expectedConfig.getLegTravelTimeEstimatorName() );
		assertEquals( 1000, expectedConfig.getJgapMaxGenerations() );
		assertEquals( 6, expectedConfig.getLevelOfTimeResolution() );
		assertEquals( true, expectedConfig.isDoLogging() );
		assertEquals( PlanomatConfigGroup.TripStructureAnalysisLayerOption.link, expectedConfig.getTripStructureAnalysisLayer());
	}
	
}
