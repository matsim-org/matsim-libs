/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.companions;


import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author Steffen Axer
 */
public class RunDrtWithCompanionExampleIT {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunDrtWithCompanions() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new OTFVisConfigGroup(), new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new), new DvrpConfigGroup());

		// Add DrtCompanionParams with some default values into existing Drt configurations
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = MultiModeDrtConfigGroup.get(config);
		DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.getModalElements().iterator().next();
		drtWithExtensionsConfigGroup.addParameterSet(new DrtCompanionParams());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Controler controler = DrtCompanionControlerCreator.createControler(config);
		controler.run();

		verifyTotalNumberOfDrtRides(utils.getOutputDirectory(), 475);
	}

	private void verifyTotalNumberOfDrtRides(String outputDirectory, int expectedNumberOfTrips) {

		String filename = outputDirectory + "/drt_customer_stats_drt.csv";

		final List<String> collect;
		try {
			collect = Files.lines(Paths.get(filename)).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		int size = collect.size();
		List<String> keys = List.of(collect.get(0).split(";"));
		List<String> lastIterationValues = List.of(collect.get(size - 1).split(";"));

		Map<String, String> params = new HashMap<>();
		for (int i = 0; i < keys.size(); i++) {
			params.put(keys.get(i), lastIterationValues.get(i));
		}

		int actualRides = Integer.parseInt(params.get("rides"));
		Assert.assertEquals(expectedNumberOfTrips, actualRides);
	}
}
