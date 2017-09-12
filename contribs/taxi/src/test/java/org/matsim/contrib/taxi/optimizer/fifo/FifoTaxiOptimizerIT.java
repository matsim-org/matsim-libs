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

package org.matsim.contrib.taxi.optimizer.fifo;

import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.*;

import java.util.*;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider.OptimizerType;
import org.matsim.testcases.MatsimTestUtils;

public class FifoTaxiOptimizerIT {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testFifo() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");

		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(true);
		Map<String, String> params = createAbstractOptimParams(OptimizerType.FIFO);

		runBenchmark(variants, params, benchmark, utils.getOutputDirectory());
	}
}
