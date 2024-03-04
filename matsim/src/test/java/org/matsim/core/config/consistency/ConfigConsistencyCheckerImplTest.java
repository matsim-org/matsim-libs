/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.config.consistency;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.pt.PtConstants;
import org.matsim.testcases.utils.LogCounter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author mrieser
 */
public class ConfigConsistencyCheckerImplTest {

	@Test
	void testCheckPlanCalcScore_DefaultsOk() {
		Config config = new Config();
		config.addCoreModules();

		LogCounter logger = new LogCounter(Level.WARN);
		try {
			logger.activate();
			ConfigConsistencyCheckerImpl.checkPlanCalcScore(config);
			Assertions.assertEquals(0, logger.getWarnCount());
		} finally {
			// make sure counter is deactivated at the end
			logger.deactivate();
		}
	}

	@Test
	void testCheckPlanCalcScore_Traveling() {
		Config config = new Config();
		config.addCoreModules();

		config.scoring().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(3.0);

		LogCounter logger = new LogCounter(Level.WARN);
		try {
			logger.activate();
			ConfigConsistencyCheckerImpl.checkPlanCalcScore(config);
			Assertions.assertEquals(1, logger.getWarnCount());
		} finally {
			// make sure counter is deactivated at the end
			logger.deactivate();
		}
	}

	@Test
	void testCheckPlanCalcScore_TravelingPt() {
		Config config = new Config();
		config.addCoreModules();

		config.scoring().getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(3.0);

		LogCounter logger = new LogCounter(Level.WARN);
		try {
			logger.activate();
			ConfigConsistencyCheckerImpl.checkPlanCalcScore(config);
			Assertions.assertEquals(1, logger.getWarnCount());
		} finally {
			// make sure counter is deactivated at the end
			logger.deactivate();
		}
	}

	@Test
	void testCheckPlanCalcScore_TravelingBike() {
		Config config = new Config();
		config.addCoreModules();

		config.scoring().getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling(3.0);

		LogCounter logger = new LogCounter(Level.WARN);
		try {
			logger.activate();
			ConfigConsistencyCheckerImpl.checkPlanCalcScore(config);
			Assertions.assertEquals(1, logger.getWarnCount());
		} finally {
			// make sure counter is deactivated at the end
			logger.deactivate();
		}
	}

	@Test
	void testCheckPlanCalcScore_TravelingWalk() {
		Config config = new Config();
		config.addCoreModules();

		config.scoring().getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(3.0);

		LogCounter logger = new LogCounter(Level.WARN);
		try {
			logger.activate();
			ConfigConsistencyCheckerImpl.checkPlanCalcScore(config);
			Assertions.assertEquals(1, logger.getWarnCount());
		} finally {
			// make sure counter is deactivated at the end
			logger.deactivate();
		}
	}

	@Test
	void testCheckPlanCalcScore_PtInteractionActivity() {
		Config config = new Config();
		config.addCoreModules();

		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setClosingTime(1.) ;
		config.scoring().addActivityParams(transitActivityParams);

		try {
			ConfigConsistencyCheckerImpl.checkPlanCalcScore(config);
			Assertions.assertEquals(0,1) ; // should never get here
		} catch ( Exception ee ){

			System.out.println("expected exception") ;
		}

		config.vspExperimental().setAbleToOverwritePtInteractionParams(true) ;

		try {
			ConfigConsistencyCheckerImpl.checkPlanCalcScore(config );
		} catch ( Exception ee ){
			Assertions.assertEquals(0,1) ; // should never get here
		}

	}


	@Test
	void checkConsistencyBetweenRouterAndTravelTimeCalculatorTest(){
		{
			Config config = ConfigUtils.createConfig();

			// first for separateModes=false:
			config.travelTimeCalculator().setSeparateModes( false );
			{
				boolean problem = ConfigConsistencyCheckerImpl.checkConsistencyBetweenRouterAndTravelTimeCalculator( config );
				Assertions.assertFalse( problem );
			}
			{
				Set<String> modes = new LinkedHashSet<>( config.routing().getNetworkModes() );
				modes.add( TransportMode.bike );
				config.routing().setNetworkModes( modes );

				boolean problem = ConfigConsistencyCheckerImpl.checkConsistencyBetweenRouterAndTravelTimeCalculator( config );
				Assertions.assertFalse( problem );
			}
			{
				Set<String> modes = new LinkedHashSet<>( config.travelTimeCalculator().getAnalyzedModes() );
				modes.add( TransportMode.bike );
				config.travelTimeCalculator().setAnalyzedModes( modes );

				boolean problem = ConfigConsistencyCheckerImpl.checkConsistencyBetweenRouterAndTravelTimeCalculator( config );
				Assertions.assertFalse( problem );
			}
			{
				Set<String> modes = new LinkedHashSet<>( config.travelTimeCalculator().getAnalyzedModes() );
				modes.add( "abc" );
				config.travelTimeCalculator().setAnalyzedModes( modes );

				boolean problem = ConfigConsistencyCheckerImpl.checkConsistencyBetweenRouterAndTravelTimeCalculator( config );
				Assertions.assertFalse( problem );
			}
		}
		{
			// then for separateModes=true

			Config config = ConfigUtils.createConfig() ;

			config.travelTimeCalculator().setSeparateModes( true );

			ConfigConsistencyCheckerImpl.checkConsistencyBetweenRouterAndTravelTimeCalculator( config );

			{
				Set<String> modes = new LinkedHashSet<>( config.routing().getNetworkModes() );
				modes.add( TransportMode.bike );
				config.routing().setNetworkModes( modes );

				boolean problem = ConfigConsistencyCheckerImpl.checkConsistencyBetweenRouterAndTravelTimeCalculator( config );
				// see comments inside that static function. kai, jul'19

//				Assert.assertTrue( problem ); // !!
			}
			{
				Set<String> modes = new LinkedHashSet<>( config.travelTimeCalculator().getAnalyzedModes() );
				modes.add( TransportMode.bike );
				config.travelTimeCalculator().setAnalyzedModes( modes );

				boolean problem = ConfigConsistencyCheckerImpl.checkConsistencyBetweenRouterAndTravelTimeCalculator( config );
				Assertions.assertFalse( problem );
			}
			{
				Set<String> modes = new LinkedHashSet<>( config.travelTimeCalculator().getAnalyzedModes() );
				modes.add( "abc" );
				config.travelTimeCalculator().setAnalyzedModes( modes );

				boolean problem = ConfigConsistencyCheckerImpl.checkConsistencyBetweenRouterAndTravelTimeCalculator( config );
				Assertions.assertFalse( problem );
			}
		}

	}
}
