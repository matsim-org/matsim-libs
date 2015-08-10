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
import org.matsim.core.replanning.ReRoutingTest;
import org.matsim.examples.EquilTest;
import org.matsim.integration.timevariantnetworks.QSimIntegrationTest;
import org.matsim.modules.ScoreStatsModuleTest;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorTest;


/**
 * @author amit
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({
	
	FlowStorageSpillbackTest.class, 
	EquilTest.class,
	TravelTimeCollectorTest.class,
	StorageCapOnSimultaneousSpillBack.class,
	ControlerTest.class,
	ReRoutingTest.class, //(4)
	ScoreStatsModuleTest.class,
	QLinkTest.class,	
	QSimTest.class, //(4)
	QSimIntegrationTest.class
	
})

public class CoreFailedTestSuite {

}
