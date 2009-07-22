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

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatConfigGroupTest extends MatsimTestCase {

	private static final Logger logger = Logger.getLogger(PlanomatConfigGroupTest.class);

	public void testPlanomatConfigGroup() {
		
		PlanomatConfigGroup expectedConfig = super.loadConfig(this.getInputDirectory() + "empty_config.xml").planomat();
		
//		assertEquals( PlanomatConfigGroup.DEFAULT_OPTIMIZATION_TOOLBOX, Gbl.getConfig().planomat().getOptimizationToolbox() );
		assertEquals( 
				Integer.parseInt(PlanomatConfigGroup.PlanomatConfigParameter.POPSIZE.getDefaultValue()), 
				expectedConfig.getPopSize() );
		assertEquals( 
				Integer.parseInt(PlanomatConfigGroup.PlanomatConfigParameter.JGAP_MAX_GENERATIONS.getDefaultValue()), 
				expectedConfig.getJgapMaxGenerations() );
		assertEquals(0, expectedConfig.getPossibleModes().toArray().length);
		assertEquals( 
				PlanomatConfigGroup.PlanomatConfigParameter.SIM_LEG_INTERPRETATION.getDefaultValue(), 
				expectedConfig.getSimLegInterpretation().toString() );
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

		PlanomatConfigGroup expectedConfig = super.loadConfig(this.getInputDirectory() + "config.xml").planomat();

		assertEquals( 10, expectedConfig.getPopSize() );
		assertEquals( 2, expectedConfig.getPossibleModes().toArray().length );
		assertEquals( TransportMode.car, expectedConfig.getPossibleModes().toArray()[0] );
		assertEquals( TransportMode.pt, expectedConfig.getPossibleModes().toArray()[1] );
		assertEquals( PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible, expectedConfig.getSimLegInterpretation() );
		assertEquals( PlanomatConfigGroup.RoutingCapability.linearInterpolation, expectedConfig.getRoutingCapability() );
		assertEquals( 1000, expectedConfig.getJgapMaxGenerations() );
		assertEquals( 6, expectedConfig.getLevelOfTimeResolution() );
		assertEquals( true, expectedConfig.isDoLogging() );
		assertEquals( PlanomatConfigGroup.TripStructureAnalysisLayerOption.link, expectedConfig.getTripStructureAnalysisLayer());
	}
	
}
