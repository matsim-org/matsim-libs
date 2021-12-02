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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.data.Percentage;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.insertion.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.insertion.SelectiveInsertionSearchParams;
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
	public void testRunDrtExampleWithNoRejections_ExtensiveSearch() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//disable rejections
			drtCfg.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(0)
				.waitAverage(300.75)
				.inVehicleTravelTimeMean(384.59)
				.totalTravelTimeMean(685.35)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	public void testRunDrtExampleWithNoRejections_SelectiveSearch() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//replace extensive with selective search
			drtCfg.removeParameterSet(drtCfg.getDrtInsertionSearchParams());
			var selectiveInsertionSearchParams = new SelectiveInsertionSearchParams();
			selectiveInsertionSearchParams.setRestrictiveBeelineSpeedFactor(1);// using exactly free-speed estimates
			drtCfg.addParameterSet(selectiveInsertionSearchParams);

			//disable rejections
			drtCfg.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(0)
				.waitAverage(304.37)
				.inVehicleTravelTimeMean(387.41)
				.totalTravelTimeMean(691.78)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	public void testRunDrtExampleWithRequestRetry() {
		Id.resetCaches();
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

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(1)
				.waitAverage(309.1)
				.inVehicleTravelTimeMean(373.48)
				.totalTravelTimeMean(682.58)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	public void testRunDrtStopbasedExample() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.05)
				.rejections(20)
				.waitAverage(251.41)
				.inVehicleTravelTimeMean(377.79)
				.totalTravelTimeMean(629.21)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	public void testRunServiceAreabasedExampleWithSpeedUp() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_serviceArea_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.03)
				.rejections(10)
				.waitAverage(226.37)
				.inVehicleTravelTimeMean(389.87)
				.totalTravelTimeMean(616.24)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	/**
	 * Early warning system: if customer stats vary more than the defined percentage above or below the expected values
	 * then the following unit tests will fail. This is meant to serve as a red flag.
	 * The following customer parameter checked are:
	 * rejectionRate, rejections, waitAverage, inVehicleTravelTimeMean, & totalTravelTimeMean
	 */

	private void verifyDrtCustomerStatsCloseToExpectedStats(String outputDirectory, Stats expectedStats) {

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

		double inVehicleTravelTimeMean = Double.parseDouble(params.get("inVehicleTravelTime_mean"));
		double waitAverage = Double.parseDouble(params.get("wait_average"));
		double rejections = Double.parseDouble(params.get("rejections"));
		double rejectionRate = Double.parseDouble(params.get("rejectionRate"));
		double totalTravelTimeMean = Double.parseDouble(params.get("totalTravelTime_mean"));

		var percentage = Percentage.withPercentage(0.00001);
		assertThat(rejectionRate).isCloseTo(expectedStats.rejectionRate, percentage);
		assertThat(rejections).isCloseTo(expectedStats.rejections, percentage);
		assertThat(waitAverage).isCloseTo(expectedStats.waitAverage, percentage);
		assertThat(inVehicleTravelTimeMean).isCloseTo(expectedStats.inVehicleTravelTimeMean, percentage);
		assertThat(totalTravelTimeMean).isCloseTo(expectedStats.totalTravelTimeMean, percentage);
	}

	private static class Stats {
		private final double rejectionRate;
		private final double rejections;
		private final double waitAverage;
		private final double inVehicleTravelTimeMean;
		private final double totalTravelTimeMean;

		private Stats(Builder builder) {
			rejectionRate = builder.rejectionRate;
			rejections = builder.rejections;
			waitAverage = builder.waitAverage;
			inVehicleTravelTimeMean = builder.inVehicleTravelTimeMean;
			totalTravelTimeMean = builder.totalTravelTimeMean;
		}

		public static Builder newBuilder() {
			return new Builder();
		}

		public static final class Builder {
			private double rejectionRate;
			private double rejections;
			private double waitAverage;
			private double inVehicleTravelTimeMean;
			private double totalTravelTimeMean;

			private Builder() {
			}

			public Builder rejectionRate(double val) {
				rejectionRate = val;
				return this;
			}

			public Builder rejections(double val) {
				rejections = val;
				return this;
			}

			public Builder waitAverage(double val) {
				waitAverage = val;
				return this;
			}

			public Builder inVehicleTravelTimeMean(double val) {
				inVehicleTravelTimeMean = val;
				return this;
			}

			public Builder totalTravelTimeMean(double val) {
				totalTravelTimeMean = val;
				return this;
			}

			public Stats build() {
				return new Stats(this);
			}
		}
	}
}
