/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2024 by the members listed in the COPYING,       *
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
  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.mobsimExamples;

import static org.junit.jupiter.api.Assertions.fail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class ExampleMobsimOfSimpleLSPTest {
	private static final Logger log = LogManager.getLogger(ExampleMobsimOfSimpleLSPTest.class);
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testForRuntimeExceptionsAndCompareEvents() {
		try {
			ExampleMobsimOfSimpleLSP.main(new String[]{
					"--config:controller.outputDirectory=" + utils.getOutputDirectory()
			});

		} catch (Exception ee) {
			log.fatal(ee);
			fail();
		}

		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}

}
