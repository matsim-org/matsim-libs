/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerTest.java
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

package playground.meisterk.kti.controler;

import java.io.File;

import org.matsim.core.controler.Controler;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.kti.router.KtiTravelCostCalculatorFactory;
import playground.meisterk.kti.router.PlansCalcRouteKti;

public class KtiControlerTest extends MatsimTestCase {

	public void testRun() {
		
		KTIControler testee = new KTIControler(new String[]{this.getClassInputDirectory() + "config.xml"});

		testee.getConfig().facilities().setInputFile(this.getClassInputDirectory() + "facilities.xml.gz");
		testee.getConfig().plans().setInputFile(this.getClassInputDirectory() + "plans.xml.gz");
		testee.getConfig().controler().setOutputDirectory(this.getOutputDirectory());

		testee.setCreateGraphs(false);
		testee.run();

		PlansCalcRoute router = (PlansCalcRoute) testee.getRoutingAlgorithm(testee.getTravelCostCalculator(), testee.getTravelTimeCalculator());
		assertEquals(
				PlansCalcRouteKti.class, 
				router.getClass());
		assertEquals(
				AStarLandmarks.class, 
				router.getLeastCostPathCalculator().getClass());
		assertEquals(
				KtiTravelCostCalculatorFactory.class,
				testee.getTravelCostCalculatorFactory().getClass());
		assertEquals(
				playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory.class, 
				testee.getScoringFunctionFactory().getClass());
		
		
		
//		assertTrue(new File(this.getOutputDirectory() + KTIControler.SVN_INFO_FILE_NAME).exists());
		assertTrue(new File(this.getOutputDirectory() + KTIControler.CALC_LEG_TIMES_KTI_FILE_NAME).exists());
		assertTrue(new File(this.getOutputDirectory() + KTIControler.SCORE_ELEMENTS_FILE_NAME).exists());
		assertTrue(new File(Controler.getIterationFilename(KTIControler.LEG_DISTANCE_DISTRIBUTION_FILE_NAME, 0)).exists());
		assertTrue(new File(Controler.getIterationFilename(KTIControler.LEG_DISTANCE_DISTRIBUTION_FILE_NAME, 10)).exists());
		assertTrue(new File(Controler.getIterationFilename(KTIControler.LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME, 0)).exists());
		assertTrue(new File(Controler.getIterationFilename(KTIControler.LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME, 10)).exists());
	}

}
