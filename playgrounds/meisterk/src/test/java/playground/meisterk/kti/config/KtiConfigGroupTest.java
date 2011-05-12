/* *********************************************************************** *
 * project: org.matsim.*
 * KtiConfigGroupTest.java
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

package playground.meisterk.kti.config;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.testcases.MatsimTestCase;

public class KtiConfigGroupTest extends MatsimTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testKtiConfigGroup() {
		
		KtiConfigGroup testee = new KtiConfigGroup();
		
		assertEquals(
				Double.parseDouble(KtiConfigGroup.KtiConfigParameter.CONST_BIKE.getDefaultValue()), 
				testee.getConstBike());
		assertEquals(
				Double.parseDouble(KtiConfigGroup.KtiConfigParameter.CONST_CAR.getDefaultValue()), 
				testee.getConstCar());
		assertEquals(KtiConfigGroup.KtiConfigParameter.PT_HALTESTELLEN_FILENAME.getDefaultValue(), testee.getPtHaltestellenFilename());
		assertEquals(KtiConfigGroup.KtiConfigParameter.PT_TRAVEL_TIME_MATRIX_FILENAME.getDefaultValue(), testee.getPtTraveltimeMatrixFilename());
		assertEquals(KtiConfigGroup.KtiConfigParameter.WORLD_INPUT_FILENAME.getDefaultValue(), testee.getWorldInputFilename());
		assertEquals(
				Boolean.parseBoolean(KtiConfigGroup.KtiConfigParameter.USE_PLANS_CALC_ROUTE_KTI.getDefaultValue()), 
				testee.isUsePlansCalcRouteKti());
		assertEquals(Double.parseDouble(KtiConfigGroup.KtiConfigParameter.DISTANCE_COST_CAR.getDefaultValue()), testee.getDistanceCostCar());
		assertEquals(Double.parseDouble(KtiConfigGroup.KtiConfigParameter.DISTANCE_COST_PT.getDefaultValue()), testee.getDistanceCostPtNoTravelCard());
		assertEquals(Double.parseDouble(KtiConfigGroup.KtiConfigParameter.DISTANCE_COST_PT_UNKNOWN.getDefaultValue()), testee.getDistanceCostPtUnknownTravelCard());
		assertEquals(Double.parseDouble(KtiConfigGroup.KtiConfigParameter.TRAVELING_BIKE.getDefaultValue()), testee.getTravelingBike());
		assertEquals(Double.parseDouble(KtiConfigGroup.KtiConfigParameter.INTRAZONAL_PT_SPEED.getDefaultValue()), testee.getIntrazonalPtSpeed());
	}
	
	public void testAddParam() {

		Config config = new Config();
		KtiConfigGroup testee = new KtiConfigGroup();
		config.addModule(KtiConfigGroup.GROUP_NAME, testee);
		new MatsimConfigReader(config).readFile(this.getInputDirectory() + "config.xml", null);

		assertEquals(999.9, testee.getConstBike());
		assertEquals(-888.8, testee.getConstCar());
		assertEquals("filename2", testee.getPtHaltestellenFilename());
		assertEquals("filename1", testee.getPtTraveltimeMatrixFilename());
		assertEquals("filename3", testee.getWorldInputFilename());
		assertTrue(testee.isUsePlansCalcRouteKti());
		assertEquals(0.1, testee.getDistanceCostCar());
		assertEquals(-0.2, testee.getDistanceCostPtNoTravelCard());
		assertEquals(0.3, testee.getDistanceCostPtUnknownTravelCard());
		assertEquals(-3.1, testee.getTravelingBike());
		assertEquals(3.42, testee.getIntrazonalPtSpeed());
		assertEquals(true, testee.isInvalidateScores());
	}
	
}
