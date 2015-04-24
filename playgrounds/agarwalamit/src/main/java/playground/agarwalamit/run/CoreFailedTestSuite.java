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
package playground.agarwalamit.run;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.matsim.core.controler.ControlerTest;
import org.matsim.core.mobsim.qsim.FlowStorageSpillbackTest;
import org.matsim.core.mobsim.qsim.QSimTest;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkTest;
import org.matsim.core.mobsim.qsim.qnetsimengine.StorageCapOnSimultaneousSpillBack;
import org.matsim.core.replanning.ReRoutingTest;
import org.matsim.examples.EquilTest;
import org.matsim.examples.OnePercentBerlin10sTest;
import org.matsim.integration.lanes.LanesIntegrationTest;
import org.matsim.integration.timevariantnetworks.QSimIntegrationTest;
import org.matsim.lanes.LaneDefinitionsReaderWriterTest;
import org.matsim.lanes.MixedLaneTest;
import org.matsim.modules.ScoreStatsModuleTest;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorTest;

/**
 * @author amit
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({
	/*
	 * Default run 5 errors, 1 skipped and 1 failure.
	 */

	//====error=====
	LanesIntegrationTest.class, 
	LaneDefinitionsReaderWriterTest.class, //(4)

	//====failure=====
	StorageCapOnSimultaneousSpillBack.class,

	//skipped
	MatsimTestCase.class,

	/*
	 * accumulatingFlowToZero = true  6 errors, 1 skipped and 18 failure.  
	 */

	//====additional error=====
	TravelTimeCollectorTest.class,

	//====additional failures=====
	FlowStorageSpillbackTest.class, // rounding error -- java.lang.AssertionError: wrong link leave time. expected:<170.0> but was:<169.0>
	EquilTest.class,
	MixedLaneTest.class,

	ControlerTest.class,
	ReRoutingTest.class, //(4)
	ScoreStatsModuleTest.class,
	QLinkTest.class,	
	QSimTest.class, //(4)
	QSimIntegrationTest.class,
	OnePercentBerlin10sTest.class, //(2)
})

public class CoreFailedTestSuite {

}
