/* **********************************************4************************ *
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

package org.matsim.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;


public class OutputTravelStatsTest {

	@RegisterExtension
	private MatsimTestUtils util = new MatsimTestUtils();

	@Test
	void testActivitiesOutputCSV() throws IOException {
		String outputDirectory = util.getOutputDirectory();

		Config config = this.util.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controller().setLastIteration(10);
		config.controller().setWriteTripsInterval(1);
		config.controller().setOutputDirectory(outputDirectory);
		Controler c = new Controler(config);

		c.run();

		File csv = new File(outputDirectory, "output_activities.csv.gz");

		assertThat(csv).exists();

		assertThat(new GZIPInputStream(new FileInputStream(csv)))
				.asString(StandardCharsets.UTF_8)
				.startsWith("person;activity_number;activity_id;activity_type;start_time;end_time;maximum_duration;link_id;facility_id;coord_x;coord_y");

	}


}
