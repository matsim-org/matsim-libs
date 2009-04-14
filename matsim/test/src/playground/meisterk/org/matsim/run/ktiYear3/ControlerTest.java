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

package playground.meisterk.org.matsim.run.ktiYear3;

import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestCase;

public class ControlerTest extends MatsimTestCase {

	private Config config;
	
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig(this.getClassInputDirectory() + "config.xml");
		this.config.facilities().setInputFile(this.getClassInputDirectory() + "facilities.xml.gz");
		this.config.plans().setInputFile(this.getClassInputDirectory() + "plans.xml.gz");
		this.config.controler().setOutputDirectory(this.getOutputDirectory());
		
	}

	public void testRun() {
		
		playground.meisterk.org.matsim.run.ktiYear3.Controler testee = new playground.meisterk.org.matsim.run.ktiYear3.Controler(this.config);
//		testee.getControler().setCreateGraphs(false);
		testee.run();
		
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.config = null;
	}
	
}
