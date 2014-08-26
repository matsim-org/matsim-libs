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

package playground.andreas.P2.scoring.deprecated;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.helper.PConfigGroup;


public class ScoreContainerTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testScoreContainer() {
		
		PConfigGroup pC = new PConfigGroup();
		pC.addParam("costPerVehicleAndDay", "40.0");
		pC.addParam("earningsPerBoardingPassenger", "3.0");
		pC.addParam("earningsPerKilometerAndPassenger", "0.20");
		pC.addParam("costPerKilometer", "0.30");
		
		Network net = PScenarioHelper.createTestNetwork().getNetwork();
		Link link1 = net.getLinks().get(new IdImpl("1112"));
		Link link2 = net.getLinks().get(new IdImpl("A"));
		
		ScoreContainer sC = new ScoreContainer(new IdImpl("veh_1"), pC.getEarningsPerBoardingPassenger(), pC.getEarningsPerKilometerAndPassenger() / 1000.0, pC.getCostPerKilometer() / 1000.0, pC.getCostPerVehicleAndDay());
		
		Assert.assertEquals("revenue with zero trips served", -40.0, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("revenue per pax with zero trips served", Double.NaN, sC.getTotalRevenuePerPassenger(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		sC.addPassenger();
		sC.handleLinkTravelled(link1);
		
//		Assert.assertEquals("revenue with one incomplete trip served", -37.12, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("revenue with one incomplete trip served", -37.120007030806455, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		// (disturbing the link lengths (see PScenarioHelper) also changes this value a bit. kai, oct'13)
		Assert.assertEquals("revenue per pax with zero trips served", Double.NaN, sC.getTotalRevenuePerPassenger(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		sC.addPassenger();
		sC.handleLinkTravelled(link2);
//		Assert.assertEquals("revenue with two incomplete trips served", -34.11, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("revenue with two incomplete trips served", -34.11000703080646, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		// (same)
		Assert.assertEquals("revenue per pax with zero trips served", Double.NaN, sC.getTotalRevenuePerPassenger(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		sC.removePassenger();
		sC.removePassenger();
//		Assert.assertEquals("revenue with two trips served", -34.11, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("revenue with two trips served", -34.11000703080646, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		// (same)
//		Assert.assertEquals("revenue per pax with two trips served", -17.055, sC.getTotalRevenuePerPassenger(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("revenue per pax with two trips served", -17.05500351540323, sC.getTotalRevenuePerPassenger(), MatsimTestUtils.EPSILON);
		// (same)
		Assert.assertEquals("trips served", 2, sC.getTripsServed(), MatsimTestUtils.EPSILON);		
	}
}