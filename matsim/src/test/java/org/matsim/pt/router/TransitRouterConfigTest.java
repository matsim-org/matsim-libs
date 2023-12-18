/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.pt.router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.pt.config.TransitRouterConfigGroup;

/**
 * @author mrieser
 */
public class TransitRouterConfigTest {

	@Test
	void testConstructor() {
		ScoringConfigGroup planScoring = new ScoringConfigGroup();
		RoutingConfigGroup planRouting = new RoutingConfigGroup();
		TransitRouterConfigGroup transitRouting = new TransitRouterConfigGroup();
		VspExperimentalConfigGroup vspConfig = new VspExperimentalConfigGroup() ;

		final double travelingPt = -9.0;
		planScoring.getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(travelingPt);
		final double travelingWalk = -11.0;
		planScoring.getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(travelingWalk);

//		planScoring.setMarginalUtlOfWaiting_utils_hr(-13.0);
		planScoring.setMarginalUtlOfWaitingPt_utils_hr(-13.0);
		// naturally, this failed after the functionality was moved to a separate planCalcScore parameter.  kai, oct'12

		planScoring.setPerforming_utils_hr(+6.0);

		planScoring.setUtilityOfLineSwitch(-2.34);

		planRouting.setTeleportedModeSpeed(TransportMode.walk, 1.37 );
		// (this will clear all defaults!)
		planRouting.setBeelineDistanceFactor(1.2);

		transitRouting.setAdditionalTransferTime(128.0);
		transitRouting.setSearchRadius(987.6);
		transitRouting.setExtensionRadius(123.4);
		transitRouting.setMaxBeelineWalkConnectionDistance(23.4);

		// test without marginal utl of time:
		{
		TransitRouterConfig config = new TransitRouterConfig(planScoring, planRouting, transitRouting, vspConfig );

//		Assert.assertEquals(-9.0/3600, config.getMarginalUtilityOfTravelTimePt_utl_s(), 1e-8);
//		Assert.assertEquals(-11.0/3600, config.getMarginalUtilityOfTravelTimeWalk_utl_s(), 1e-8);
//		Assert.assertEquals(-13.0/3600, config.getMarginalUtiltityOfWaiting_utl_s(), 1e-8);
		// a number of changes related to the fact that the opportunity cost of time is now
		// included in the pt routing.  Either the test here or some scoring
		// test needs to be adapted; this seems the better place. kai/benjamin, oct'12
		Assertions.assertEquals(-15.0/3600, config.getMarginalUtilityOfTravelTimePt_utl_s(), 1e-8);
		Assertions.assertEquals(-17.0/3600, config.getMarginalUtilityOfTravelTimeWalk_utl_s(), 1e-8);
		Assertions.assertEquals(-19.0/3600, config.getMarginalUtilityOfWaitingPt_utl_s(), 1e-8);

		Assertions.assertEquals(-2.34, config.getUtilityOfLineSwitch_utl(), 1e-8);
		Assertions.assertEquals(1.37 / 1.2, config.getBeelineWalkSpeed(), 1e-8);

		Assertions.assertEquals(128.0, config.getAdditionalTransferTime(), 1e-8);
		Assertions.assertEquals(987.6, config.getSearchRadius(), 1e-8);
		Assertions.assertEquals(123.4, config.getExtensionRadius(), 1e-8);
		Assertions.assertEquals(23.4, config.getBeelineWalkConnectionDistance(), 1e-8);
		}

		// test with marginal utl of time:
		{
		TransitRouterConfig config = new TransitRouterConfig(planScoring, planRouting, transitRouting, vspConfig );

		Assertions.assertEquals(-15.0/3600, config.getMarginalUtilityOfTravelTimePt_utl_s(), 1e-8);
		Assertions.assertEquals(-17.0/3600, config.getMarginalUtilityOfTravelTimeWalk_utl_s(), 1e-8);
		Assertions.assertEquals(-19.0/3600, config.getMarginalUtilityOfWaitingPt_utl_s(), 1e-8);
		Assertions.assertEquals(-2.34, config.getUtilityOfLineSwitch_utl(), 1e-8);
		Assertions.assertEquals(1.37 / 1.2, config.getBeelineWalkSpeed(), 1e-8);

		Assertions.assertEquals(128.0, config.getAdditionalTransferTime(), 1e-8);
		Assertions.assertEquals(987.6, config.getSearchRadius(), 1e-8);
		Assertions.assertEquals(123.4, config.getExtensionRadius(), 1e-8);
		Assertions.assertEquals(23.4, config.getBeelineWalkConnectionDistance(), 1e-8);
		}
	}
}
