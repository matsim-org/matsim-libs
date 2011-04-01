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

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.pt.config.TransitRouterConfigGroup;

/**
 * @author mrieser
 */
public class TransitRouterConfigTest {

	@Test
	public void testConstructor() {
		PlanCalcScoreConfigGroup planScoring = new PlanCalcScoreConfigGroup();
		PlansCalcRouteConfigGroup planRouting = new PlansCalcRouteConfigGroup();
		TransitRouterConfigGroup transitRouting = new TransitRouterConfigGroup();
		
		planScoring.setTravelingPt_utils_hr(-9.0);
		planScoring.setTravelingWalk_utils_hr(-11.0);
		planScoring.setWaiting_utils_hr(-13.0);
		planScoring.setPerforming_utils_hr(+6.0);
		
		planScoring.setUtilityOfLineSwitch(-2.34);
		
		planRouting.setWalkSpeed(1.37);
		planRouting.setBeelineDistanceFactor(1.2);
		
		transitRouting.setAdditionalTransferTime(128.0);
		transitRouting.setSearchRadius(987.6);
		transitRouting.setExtensionRadius(123.4);
		transitRouting.setMaxBeelineWalkConnectionDistance(23.4);
		
		TransitRouterConfig config = new TransitRouterConfig(planScoring, planRouting, transitRouting);
		
		Assert.assertEquals(-9.0/3600, config.getMarginalUtilityOfTravelTimePt_utl_s(), 1e-8);
		Assert.assertEquals(-11.0/3600, config.getMarginalUtilityOfTravelTimeWalk_utl_s(), 1e-8);
		Assert.assertEquals(-13.0/3600, config.getMarginalUtiltityOfWaiting_utl_s(), 1e-8);
		Assert.assertEquals(-2.34, config.getUtilityOfLineSwitch_utl(), 1e-8);
		Assert.assertEquals(1.37 / 1.2, config.getBeelineWalkSpeed(), 1e-8);
		
		Assert.assertEquals(128.0, config.additionalTransferTime, 1e-8);
		Assert.assertEquals(987.6, config.searchRadius, 1e-8);
		Assert.assertEquals(123.4, config.extensionRadius, 1e-8);
		Assert.assertEquals(23.4, config.beelineWalkConnectionDistance, 1e-8);
		
	}
}
