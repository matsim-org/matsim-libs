/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatControlerTest.java
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

package org.matsim.examples;

import org.matsim.config.Config;
import org.matsim.config.groups.PlanomatConfigGroup;
import org.matsim.controler.Controler;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

public class PlanomatControlerTest extends MatsimTestCase {

	private Config config;
	
	protected void setUp() throws Exception {
		super.setUp();
		this.config = this.loadConfig(this.getClassInputDirectory() + "config.xml");
	}

	public void testMainDefault() {
		this.runControlerTest();
	}

	public void testMainCarPt() {
		this.config.charyparNagelScoring().setTravelingPt(-6);
		this.config.planomat().setPossibleModes(PlanomatConfigGroup.POSSIBLE_MODES_CAR_PT);
		this.runControlerTest();
	}

	private void runControlerTest() {
		config.controler().setOutputDirectory(this.getOutputDirectory());
		Controler testee = new Controler(config);
		testee.setCreateGraphs(false);
		testee.setWriteEventsInterval(0);
		testee.run();

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromGZFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromGZFile(this.getOutputDirectory() + "output_plans.xml.gz");
		assertEquals(expectedChecksum, actualChecksum);
	}
	
}
