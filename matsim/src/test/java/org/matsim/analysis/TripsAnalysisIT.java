/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripsAnalysisIT {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testMainMode() {
		final Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		Scenario scenario = ScenarioUtils.loadScenario(config);

		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());

		Controler controller = new Controler(scenario);
		controller.run();

		String tripsCsvFilename = controller.getControlerIO().getOutputFilename(Controler.DefaultFiles.tripscsv);
		try (Reader reader = IOUtils.getBufferedReader(tripsCsvFilename)) {
			CSVParser parser = new CSVParser(reader, CSVFormat.newFormat(config.global().getDefaultDelimiter().charAt(0)));
			List<CSVRecord> records = parser.getRecords();
			CSVRecord header = records.get(0);
			Map<String, Integer> headerMap = new HashMap<>();
			for (int i = 0; i < header.size(); i++) {
				headerMap.put(header.get(i), i);
			}
			int mainModeIndex = headerMap.get("main_mode");
			for (int row = 1, n = Math.min(30, records.size()); row < n; row++) {
				CSVRecord record = records.get(row);
				String mainMode = record.get(mainModeIndex);
				if (mainMode == null || mainMode.isBlank()) {
					Assertions.fail("Row " + row + " has no main_mode set.\nheader = " + header + "\nrow = " + row);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
