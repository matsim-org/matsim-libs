/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

import org.matsim.contrib.taxi.benchmark.TaxiBenchmarkPostProcessor;

public class PostProcessEBenchmarkResults {
	public static void processNewMielec(String type) {
		String dir = "d:/eclipse/shared-svn/projects/maciejewski/Mielec/2016_06_euro2016_runs/";
		String subDirPrefix = "";

		new TaxiBenchmarkPostProcessor(ETaxiBenchmarkStats.HEADER, //
				"1.0", //
				"1.5", //
				"2.0", //
				"2.5", //
				"3.0", //
				"3.5", //
				"4.0"//
		).process(dir + type, subDirPrefix, "ebenchmark_stats");
	}

	public static void main(String[] args) {
		// processMielec();

		// String variant = "";
		String variant = "plugs-2and0";
		// String variant ="plugs-2and1";
		// String variant = "plugs-2and2";

		processNewMielec("E_ASSIGNMENT_" + variant + "_");
		processNewMielec("E_RULE_BASED_" + variant + "_");
	}
}
