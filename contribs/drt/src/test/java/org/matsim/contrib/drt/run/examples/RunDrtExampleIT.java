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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.insertion.repeatedselective.RepeatedSelectiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.prebooking.PrebookingParams;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogic;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.stops.CorrectedStopTimeCalculator;
import org.matsim.contrib.drt.stops.CumulativeStopTimeCalculator;
import org.matsim.contrib.drt.stops.MinimumStopDurationAdapter;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.stops.StaticPassengerStopDurationProvider;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.common.base.MoreObjects;

/**
 * @author jbischoff
 * @author Sebastian Hörl, IRT SystemX (sebhoerl)
 */
public class RunDrtExampleIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRunDrtExampleWithNoRejections_ExtensiveSearch() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//disable rejections
			drtCfg.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		}

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(0)
				.waitAverage(296.95)
				.inVehicleTravelTimeMean(387.02)
				.totalTravelTimeMean(683.97)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtExampleWithNoRejections_SelectiveSearch() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//replace extensive with selective search
			drtCfg.removeParameterSet(drtCfg.getDrtInsertionSearchParams());
			var selectiveInsertionSearchParams = new SelectiveInsertionSearchParams();
			// using exactly free-speed estimates
			selectiveInsertionSearchParams.restrictiveBeelineSpeedFactor = 1;
			drtCfg.addParameterSet(selectiveInsertionSearchParams);

			//disable rejections
			drtCfg.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		}

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(0)
				.waitAverage(293.63)
				.inVehicleTravelTimeMean(388.85)
				.totalTravelTimeMean(682.48)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtExampleWithNoRejections_RepeatedSelectiveSearch() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//replace extensive with repeated selective search
			drtCfg.removeParameterSet(drtCfg.getDrtInsertionSearchParams());
			var repeatedSelectiveInsertionSearchParams = new RepeatedSelectiveInsertionSearchParams();
			// using adaptive travel time matrix
			repeatedSelectiveInsertionSearchParams.retryInsertion = 5;
			drtCfg.addParameterSet(repeatedSelectiveInsertionSearchParams);

			//disable rejections
			drtCfg.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		}

		config.controller().setLastIteration(3);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
			.rejectionRate(0.0)
			.rejections(0)
			.waitAverage(261.57)
			.inVehicleTravelTimeMean(382.74)
			.totalTravelTimeMean(644.32)
			.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtExampleWithRequestRetry() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//relatively high max age to prevent rejections
			var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
			drtRequestInsertionRetryParams.maxRequestAge = 7200;
			drtCfg.addParameterSet(drtRequestInsertionRetryParams);
		}

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(1)
				.waitAverage(305.97)
				.inVehicleTravelTimeMean(378.18)
				.totalTravelTimeMean(684.16)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtStopbasedExample() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.05)
				.rejections(17)
				.waitAverage(260.41)
				.inVehicleTravelTimeMean(374.87)
				.totalTravelTimeMean(635.28)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtStopbasedExampleWithFlexibleStopDuration() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controler controller = DrtControlerCreator.createControler(config, false);

		// This snippet adds the correction against wait times smaller than the defined stopDuration
		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				StopTimeCalculator stopTimeCalculator = new CorrectedStopTimeCalculator(60.0);
				bindModal(StopTimeCalculator.class).toInstance(stopTimeCalculator);
			}
		});

		controller.run();

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.05)
				.rejections(17)
				.waitAverage(261.88)
				.inVehicleTravelTimeMean(376.04)
				.totalTravelTimeMean(637.93)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunServiceAreabasedExampleWithSpeedUp() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_serviceArea_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.03)
				.rejections(11)
				.waitAverage(223.86)
				.inVehicleTravelTimeMean(389.57)
				.totalTravelTimeMean(613.44)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtExampleWithIncrementalStopDuration() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controler controller = DrtControlerCreator.createControler(config, false);

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				PassengerStopDurationProvider stopDurationProvider = StaticPassengerStopDurationProvider.of(60.0, 5.0);
				StopTimeCalculator stopTimeCalculator = new CumulativeStopTimeCalculator(stopDurationProvider);
				stopTimeCalculator = new MinimumStopDurationAdapter(stopTimeCalculator, 60.0);
				bindModal(StopTimeCalculator.class).toInstance(stopTimeCalculator);
			}
		});

		controller.run();

		// sh, 11/08/2023: updated after introducing prebookg, basically we generate a
		// new feasible insertion (see InsertionGenerator) that previously did not
		// exist, but has the same cost (pickup loss + drop-off loss) as the original
		// one
		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.04)
				.rejections(16)
				.waitAverage(278.76)
				.inVehicleTravelTimeMean(384.93)
				.totalTravelTimeMean(663.68)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtWithPrebooking() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtConfig.addParameterSet(new PrebookingParams());

		Controler controller = DrtControlerCreator.createControler(config, false);
		ProbabilityBasedPrebookingLogic.install(controller, drtConfig, 0.5, 4.0 * 3600.0);

		PrebookingTracker tracker = new PrebookingTracker();
		tracker.install(controller);

		controller.run();

		assertEquals(157, tracker.immediateScheduled);
		assertEquals(205, tracker.prebookedScheduled);
		assertEquals(26, tracker.immediateRejected);
		assertEquals(0, tracker.prebookedRejected);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.07)
				.rejections(26)
				.waitAverage(232.76)
				.inVehicleTravelTimeMean(389.09)
				.totalTravelTimeMean(621.85)
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

		var actualStats = Stats.newBuilder()
				.rejectionRate(Double.parseDouble(params.get("rejectionRate")))
				.rejections(Double.parseDouble(params.get("rejections")))
				.waitAverage(Double.parseDouble(params.get("wait_average")))
				.inVehicleTravelTimeMean(Double.parseDouble(params.get("inVehicleTravelTime_mean")))
				.totalTravelTimeMean(Double.parseDouble(params.get("totalTravelTime_mean")))
				.build();

		assertThat(actualStats).usingRecursiveComparison().isEqualTo(expectedStats);
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

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("rejectionRate", rejectionRate)
					.add("rejections", rejections)
					.add("waitAverage", waitAverage)
					.add("inVehicleTravelTimeMean", inVehicleTravelTimeMean)
					.add("totalTravelTimeMean", totalTravelTimeMean)
					.toString();
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

	static private class PrebookingTracker implements PassengerRequestRejectedEventHandler, PassengerRequestScheduledEventHandler {
		int immediateScheduled = 0;
		int prebookedScheduled = 0;
		int immediateRejected = 0;
		int prebookedRejected = 0;

		@Override
		public void handleEvent(PassengerRequestScheduledEvent event) {
			if (event.getRequestId().toString().contains("prebooked")) {
				prebookedScheduled++;
			} else {
				immediateScheduled++;
			}
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			if (event.getRequestId().toString().contains("prebooked")) {
				prebookedRejected++;
			} else {
				immediateRejected++;
			}
		}

		void install(Controler controller) {
			PrebookingTracker thisTracker = this;

			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(thisTracker);
				}
			});
		}
	}
}
