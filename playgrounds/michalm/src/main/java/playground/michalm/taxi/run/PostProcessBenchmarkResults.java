/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.contrib.taxi.benchmark.*;

public class PostProcessBenchmarkResults {
	public static void processMielec() {
		String dir = "d:/PP-rad/mielec/2014_02/";
		String subDirPrefix = "";

		new TaxiBenchmarkPostProcessor(TaxiBenchmarkStats.HEADER, //
				"10-50", //
				"15-50", //
				"20-50", //
				"25-50", //
				"30-50", //
				"35-50", //
				"40-50", //
				null, // empty column
				"10-25", //
				"15-25", //
				"20-25", //
				"25-25", //
				"30-25", //
				"35-25", //
				"40-25"//
		).process(dir, subDirPrefix, "stats");
	}

	public static void processNewMielec(String type) {
		String dir = "d:/eclipse/shared-svn/projects/maciejewski/Mielec/2016_06_euro2016_runs/new/";
		String subDirPrefix = "";

		new TaxiBenchmarkPostProcessor(TaxiBenchmarkStats.HEADER, //
				"1.0", //
				"1.5", //
				"2.0", //
				"2.5", //
				"3.0", //
				"3.5", //
				"4.0"//
		).process(dir + type, subDirPrefix, "benchmark_stats");
	}

	public static void processBerlin() {
		String dir = "d:/PP-rad/berlin/Only_Berlin_2015_08/";
		String subDirPrefix = "demand_";

		new TaxiBenchmarkPostProcessor(TaxiBenchmarkStats.HEADER, //
				"1.0", //
				"1.5", //
				"2.0", //
				"2.5", //
				"3.0", //
				// "3.1", //
				// "3.2", //
				// "3.3" //
				"3.5", //
				"4.0", //
				"4.5", //
				"5.0"//
		).process(dir, subDirPrefix, "stats");
	}

	public static void processBarcelonaVariableDemand() {
		String dir = "d:/PP-rad/Barcelona/Barcelona_2015_09/";
		String subDirPrefix = "demand_";

		new TaxiBenchmarkPostProcessor(TaxiBenchmarkStats.HEADER, //
				"0.2", //
				"0.3", //
				"0.4", //
				"0.5", //
				"0.6", //
				"0.7", //
				"0.8", //
				"0.9" //
		// "1.0"//
		).process(dir, subDirPrefix, "stats");
	}

	public static void processBarcelonaVariableSupply() {
		String dir = "d:/PP-rad/Barcelona/Barcelona_2015_09/";
		String subDirPrefix = "supply_from_reqs_";

		new TaxiBenchmarkPostProcessor(TaxiBenchmarkStats.HEADER, //
				// "0.2", //
				// "0.4", //
				// "0.6", //
				// "0.8", //
				// "1.0", //
				// "1.2", //
				// "1.4", //
				// "1.6", //
				// "1.8", //
				// "2.0"//
				"0.45_DSE"//
		).process(dir, subDirPrefix, "stats");
	}

	public static void processAudiAV_10() {
		String dir = "d:/PP-rad/audi_av/audi_av_10pct_2015_10/";
		String subDirPrefix = "taxi_vehicles_";

		new TaxiBenchmarkPostProcessor(TaxiBenchmarkStats.HEADER, //
				// "04000", //
				// "04500", //
				// "05000", //
				// "05500", //
				// "06000", //
				// "06500", //
				// "07000", //
				// "07500", //
				// "08000" //
				"09000", //
				"10000", //
				"11000", //
				"12000", //
				"13000" //
		// "14000", //
		// "15000", //
		// "16000", //
		// "17000", //
		// "18000", //
		// "19000", //
		// "20000", //
		// "21000", //
		// "22000", //
		// "23000", //
		// "24000", //
		// "25000" //
		).process(dir, subDirPrefix, "stats");
	}

	public static void processAudiAV_100() {
		String dir = "d:/PP-rad/audi_av/audi_av_2015_10/";
		String subDirPrefix = "taxi_vehicles_";

		new TaxiBenchmarkPostProcessor(TaxiBenchmarkStats.HEADER, //
				// "050000", //
				// "060000", //
				// "070000", //
				"080000", //
				"090000", //
				"100000", //
				"110000", //
				"120000" //
		// "130000", //
		// "140000", //
		// "150000", //
		// "160000", //
		// "170000", //
		// "180000", //
		// "190000", //
		// "200000", //
		// "210000", //
		// "220000", //
		// "230000", //
		// "240000", //
		// "250000" //
		).process(dir, subDirPrefix, "stats");
	}

	public static void main(String[] args) {
		// processMielec();
		processNewMielec("ASSIGNMENT_");
		processNewMielec("RULE_BASED_");

		// processBerlin();
		// processBarcelonaVariableDemand();
		// processBarcelonaVariableSupply();
		// processAudiAV_10();
		// processAudiAV_100();
	}
}
