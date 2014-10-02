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

package org.matsim.examples;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / Senozon AG
 */
public class PtTutorialTest {

	private final static Logger log = Logger.getLogger(PtTutorialTest.class);
	
	public @Rule MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void ensure_tutorial_runs() {
		Config config = this.utils.loadConfig("examples/pt-tutorial/0.config.xml");
		config.controler().setLastIteration(1);

		try {
			Controler controler = new Controler(config);
			controler.run();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.fail("There shouldn't be any exception, but there was ... :-(");
		}
		Assert.assertTrue(new File(config.controler().getOutputDirectory(), "ITERS/it.1/1.plans.xml.gz").exists());
		Assert.assertTrue(new File(config.controler().getOutputDirectory(), "output_config.xml.gz").exists());
		
	}
	
}
