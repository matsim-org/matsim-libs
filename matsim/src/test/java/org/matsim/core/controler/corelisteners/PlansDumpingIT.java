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

package org.matsim.core.controler.corelisteners;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mrieser
 */
public class PlansDumpingIT {

	@RegisterExtension private MatsimTestUtils util = new MatsimTestUtils();

	@Test
	void testPlansDump_Interval() {
		Config config = this.util.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controller().setLastIteration(10);
		config.controller().setWritePlansInterval(3);
		Controler c = new Controler(config);
		c.getConfig().controller().setWriteEventsInterval(0);
        c.getConfig().controller().setCreateGraphs(false);

        c.run();

		assertTrue(new File(c.getControlerIO().getIterationFilename(0, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(1, "plans.xml.gz")).exists()); // it.1 is always written
		assertFalse(new File(c.getControlerIO().getIterationFilename(2, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(3, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(4, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(5, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(6, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(7, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(8, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(9, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(10, "plans.xml.gz")).exists());
	}

	@Test
	void testPlansDump_Never() {
		Config config = this.util.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controller().setLastIteration(10);
		config.controller().setWritePlansInterval(0);
		Controler c = new Controler(config);
		c.getConfig().controller().setWriteEventsInterval(0);
        c.getConfig().controller().setCreateGraphs(false);

        c.run();

		assertFalse(new File(c.getControlerIO().getIterationFilename(0, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(1, "plans.xml.gz")).exists()); // it.1 is deactivated when interval = 0
		assertFalse(new File(c.getControlerIO().getIterationFilename(2, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(3, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(4, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(5, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(6, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(7, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(8, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(9, "plans.xml.gz")).exists());
		assertFalse(new File(c.getControlerIO().getIterationFilename(10, "plans.xml.gz")).exists());
	}

	@Test
	void testPlansDump_Always() {
		Config config = this.util.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controller().setLastIteration(10);
		config.controller().setWritePlansInterval(1);
		Controler c = new Controler(config);
		c.getConfig().controller().setWriteEventsInterval(0);
        c.getConfig().controller().setCreateGraphs(false);

        c.run();

		assertTrue(new File(c.getControlerIO().getIterationFilename(0, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(1, "plans.xml.gz")).exists()); // it.1 is always written
		assertTrue(new File(c.getControlerIO().getIterationFilename(2, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(3, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(4, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(5, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(6, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(7, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(8, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(9, "plans.xml.gz")).exists());
		assertTrue(new File(c.getControlerIO().getIterationFilename(10, "plans.xml.gz")).exists());
	}
}
