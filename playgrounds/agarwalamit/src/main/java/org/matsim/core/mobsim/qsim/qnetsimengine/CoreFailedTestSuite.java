/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.matsim.core.controler.ControlerTest;
import org.matsim.core.mobsim.qsim.FlowStorageSpillbackTest;
import org.matsim.core.mobsim.qsim.QSimTest;
import org.matsim.core.mobsim.qsim.TransitQueueNetworkTest;
import org.matsim.examples.EquilTest;
import org.matsim.integration.always.ReRoutingTest;
import org.matsim.modules.ScoreStatsModuleTest;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorTest;

import playground.agarwalamit.flowDynamics.StorageCapOnSimultaneousSpillBackTest;

/**
 * @author amit
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({
	
	FlowStorageSpillbackTest.class, //testFlowCongestion
	EquilTest.class, //testEquil
	TravelTimeCollectorTest.class, //testGetLinkTravelTime
	ControlerTest.class, //testTravelTimeCalculation
	ReRoutingTest.class,  //testReRoutingFastAStarLandmarks
	ScoreStatsModuleTest.class,//testScoreStats
	
	/*
	 * flowacc must be updated before doSimStep(now) step. That's why both tests are failing.
	 */
	QLinkTest.class, //	testBuffer, testStorageSpaceDifferentVehicleSizes 

	/*
	 * rounding errors for e.g. remaining flowcap is 0.00000000001 and therefore can allow next vehicle.
	 */
	QSimTest.class, //testFlowCapacityDriving, testFlowCapacityStarting, testFlowCapacityMixed --
	
	StorageCapOnSimultaneousSpillBackTest.class,//storageCapTest4BottleneckLink
	TransitQueueNetworkTest.class //testNonBlockingStop_FirstLink, testBlockingStop_FirstLink
})

public class CoreFailedTestSuite {

}
