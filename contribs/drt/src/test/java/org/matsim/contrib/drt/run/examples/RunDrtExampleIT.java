/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.run.examples;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.drt.optimizer.insertion.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author jbischoff
 */
public class RunDrtExampleIT {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	public void testRunDrtExampleWithRequestRetry() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//relatively high max age to prevent rejections
			drtCfg.addParameterSet(new DrtRequestInsertionRetryParams().setMaxRequestAge(7200));
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		// Early warning system for drastic changes in drt performance with regards to customer experience (see note below)
		verifyDrtCustomerStatsInReasonableRange(utils.getOutputDirectory(), 0.01,
				5, 700.11, 381.06, 1081.17);
	}


	@Test
	public void testRunDrtStopbasedExample() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		// Early warning system for drastic changes in drt performance with regards to customer experience (see note below)
		verifyDrtCustomerStatsInReasonableRange(utils.getOutputDirectory(), 0.05, 18, 255.6,
				378.99, 634.59);
	}

	@Test
	public void testRunServiceAreabasedExampleWithSpeedUp() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_serviceArea_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);


		// Early warning system for drastic changes in drt performance with regards to customer experience (see note below)
		verifyDrtCustomerStatsInReasonableRange(utils.getOutputDirectory(), 0.03, 11, 227.56,
				385.43, 612.98);
	}

	/**
	 *	Early warning system: if customer parameters vary more than 20% above or below the values from commit ee6b608
	 * 	(March 2021), then the following unit tests will fail. This is meant to serve as a red flag if drt performance
	 * 	with respect to customer experience changes with drastically. The following customer parameter checked are:
	 * 	rejectionRate, rejections, waitAverage, inVehicleTravelTimeMean, & totalTravelTimeMean
	 *
	 */

	private void verifyDrtCustomerStatsInReasonableRange(String outputDirectory, double rejectionRateExpected, double rejectionsExpected,
														 double waitAverageExpected, double inVehicleTravelTimeMeanExpected, double totalTravelTimeMeanExpected) {

		String filename = outputDirectory + "/drt_customer_stats_drt.csv";

		List<String> collect;

		try (
				Stream<String> lines = Files.lines(Paths.get(filename))
		) {
			collect = lines.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("output file not found");
		}

		int size = collect.size();

		List<String> keys = Arrays.asList(collect.get(0).split(";"));
		List<String> values = Arrays.asList(collect.get(size - 1).split(";"));

		Map<String, String> params = new HashMap<>();
		for (int i = 0; i < keys.size(); i++) {
			params.put(keys.get(i), values.get(i));
		}

		double inVehicleTravelTimeMean = Double.parseDouble(params.get("inVehicleTravelTime_mean"));
		double waitAverage = Double.parseDouble(params.get("wait_average"));
		double rejections = Double.parseDouble(params.get("rejections"));
		double rejectionRate = Double.parseDouble(params.get("rejectionRate"));
		double totalTravelTimeMean = Double.parseDouble(params.get("totalTravelTime_mean"));


		Assert.assertEquals("rejection rate is within +-20% of expected value",
				rejectionRateExpected, rejectionRate, rejectionRateExpected * 0.2);
		Assert.assertEquals("rejections are within +-20% of expected value",
				rejectionsExpected, rejections, rejectionsExpected * 0.2);
		Assert.assertEquals("waitAverage is within +-20% of expected value",
				waitAverageExpected, waitAverage, waitAverageExpected * 0.2);
		Assert.assertEquals("inVehicleTravelTimeMean is within +-20% of expected value",
				inVehicleTravelTimeMeanExpected, inVehicleTravelTimeMean, inVehicleTravelTimeMeanExpected * 0.2);
		Assert.assertEquals("totalTravelTimeMean is within +-20% of expected value",
				totalTravelTimeMeanExpected, totalTravelTimeMean, totalTravelTimeMeanExpected * 0.2);

	}


}
