/* *********************************************************************** *
 * project: org.matsim.*
 * RunEquilRuns.java
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

package playground.meisterk.phd.controler;

import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

public class PhDControlerTest extends MatsimTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Controler testee = new PhDControler(new String[]{this.getInputDirectory() + "config.xml"});
		testee.getConfig().controler().setOutputDirectory(this.getOutputDirectory());

		testee.setCreateGraphs(false);
		testee.setWriteEventsInterval(0);
		testee.run();
	}

	public void testTAMRouter() {
		fail("Not yet implemented.");
	}
	
	public void testPlanomatOnly() {
		fail("Not yet implemented.");
	}
	
	public void testPlanomatRouter() {
		fail("Not yet implemented.");
	}
	
	public void testPlanomatRouterCarPt() {
		fail("Not yet implemented.");
	}
	
	public void xtestImprovePtTravelTime() {
		fail("Not yet implemented.");
	}
	
	public void xtestUpgradeRouteToHighway() {
		fail("Not yet implemented.");
	}
	
	public void xtestIntroduceTollOnHighway() {
		fail("Not yet implemented.");
	}
	
	public void xtestRealWorldFeatures() {
		fail("Not yet implemented.");
	}
	
	public void xtestVaryLeisureOpeningTimes() {
		fail("Not yet implemented.");
	}
	
	public void xtestIntroduceMobilityTools() {
		fail("Not yet implemented.");
	}
	
}
