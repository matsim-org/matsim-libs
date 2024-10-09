/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.etaxi.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RunETaxiBenchmarkTest {
	private static final Logger log = LogManager.getLogger(RunETaxiBenchmarkTest.class);

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRuleBased() {
		String configPath = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "one_etaxi_benchmark_config.xml").toString();
		String[] args = { configPath, "--config:controler.outputDirectory", utils.getOutputDirectory() };
		// the config file suppresses most writing of output.  Presumably, since it is to be run as a benchmark.  One can override it here, but it is again overwritten later.  So
		// I guess that the authors really mean it.  In consequence, cannot test regression on the functionality of the benchmark.  kai, nov'22

		RunETaxiBenchmark.run(args, 2);
	}
}
