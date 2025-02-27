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
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.insertion.repeatedselective.RepeatedSelectiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DefaultOfferAcceptor;
import org.matsim.contrib.drt.passenger.DrtOfferAcceptor;
import org.matsim.contrib.drt.passenger.DrtRequest;
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
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
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

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//disable rejections
			drtCfg.addOrGetDrtOptimizationConstraintsParams()
					.addOrGetDefaultDrtOptimizationConstraintsSet()
					.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		}

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(0)
				.waitAverage(297.19)
				.inVehicleTravelTimeMean(386.78)
				.totalTravelTimeMean(683.97)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtExampleWithNoRejections_SelectiveSearch() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//replace extensive with selective search
			drtCfg.removeParameterSet(drtCfg.getDrtInsertionSearchParams());
			var selectiveInsertionSearchParams = new SelectiveInsertionSearchParams();
			// using exactly free-speed estimates
			selectiveInsertionSearchParams.restrictiveBeelineSpeedFactor = 1;
			drtCfg.addParameterSet(selectiveInsertionSearchParams);

			//disable rejections
			drtCfg.addOrGetDrtOptimizationConstraintsParams()
					.addOrGetDefaultDrtOptimizationConstraintsSet()
					.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
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

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
			new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//replace extensive with repeated selective search
			drtCfg.removeParameterSet(drtCfg.getDrtInsertionSearchParams());
			var repeatedSelectiveInsertionSearchParams = new RepeatedSelectiveInsertionSearchParams();
			// using adaptive travel time matrix
			repeatedSelectiveInsertionSearchParams.retryInsertion = 5;
			drtCfg.addParameterSet(repeatedSelectiveInsertionSearchParams);

			//disable rejections
			drtCfg.addOrGetDrtOptimizationConstraintsParams()
					.addOrGetDefaultDrtOptimizationConstraintsSet()
					.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		}

		config.controller().setLastIteration(3);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
			.rejectionRate(0.0)
			.rejections(0)
			.waitAverage(269.8)
			.inVehicleTravelTimeMean(379.69)
			.totalTravelTimeMean(649.49)
			.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtExampleWithRequestRetry() {
		Id.resetCaches();

		DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfig.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfig,
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
				.waitAverage(306.21)
				.inVehicleTravelTimeMean(377.94)
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
				.waitAverage(260.24)
				.inVehicleTravelTimeMean(375.14)
				.totalTravelTimeMean(635.38)
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
				.waitAverage(261.71)
				.inVehicleTravelTimeMean(376.32)
				.totalTravelTimeMean(638.03)
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
				.rejectionRate(0.02)
				.rejections(9)
				.waitAverage(224.46)
				.inVehicleTravelTimeMean(392.74)
				.totalTravelTimeMean(617.21)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtExampleWithIncrementalStopDuration() {
		Id.resetCaches();

		DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfig.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfig,
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

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.05)
				.rejections(18)
				.waitAverage(276.95)
				.inVehicleTravelTimeMean(384.72)
				.totalTravelTimeMean(661.66)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtWithPrebooking() {
		Id.resetCaches();

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
				new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);
		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.abortRejectedPrebookings = false;
		drtConfig.addParameterSet(prebookingParams);

		Controler controller = DrtControlerCreator.createControler(config, false);
		ProbabilityBasedPrebookingLogic.install(controller, drtConfig, 0.5, 4.0 * 3600.0);

		PrebookingTracker tracker = new PrebookingTracker();
		tracker.install(controller);

		controller.run();

		assertEquals(169, tracker.immediateScheduled);
		assertEquals(205, tracker.prebookedScheduled);
		assertEquals(14, tracker.immediateRejected);
		assertEquals(0, tracker.prebookedRejected);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.04)
				.rejections(14)
				.waitAverage(232.48)
				.inVehicleTravelTimeMean(389.16)
				.totalTravelTimeMean(621.63)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtWithPrebookingAndRoutingUpdates() {
		Id.resetCaches();

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
				new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtConfig.updateRoutes = true;

		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.abortRejectedPrebookings = false;
		drtConfig.addParameterSet(prebookingParams);

		Controler controller = DrtControlerCreator.createControler(config, false);
		ProbabilityBasedPrebookingLogic.install(controller, drtConfig, 0.5, 4.0 * 3600.0);

		PrebookingTracker tracker = new PrebookingTracker();
		tracker.install(controller);

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				bindModal(VariableTravelTime.class).toInstance(new VariableTravelTime());
				addMobsimListenerBinding().to(modalKey(VariableTravelTime.class));
				bindModal(TravelTime.class).to(modalKey(VariableTravelTime.class));
			}
		});

		controller.run();

		assertEquals(124, tracker.immediateScheduled);
		assertEquals(197, tracker.prebookedScheduled);
		assertEquals(67, tracker.immediateRejected);
		assertEquals(8, tracker.prebookedRejected);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.19)
				.rejections(75)
				.waitAverage(208.48)
				.inVehicleTravelTimeMean(367.07)
				.totalTravelTimeMean(575.55)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	static private class VariableTravelTime implements TravelTime, MobsimBeforeSimStepListener {
		private final TravelTime delegate = new FreeSpeedTravelTime();
		private boolean modify = false;

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			if (modify) {
				return 2.0 * delegate.getLinkTravelTime(link, time, person, vehicle);
			} else {
				return delegate.getLinkTravelTime(link, time, person, vehicle);
			}
		}

		@Override
		public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
			modify = e.getSimulationTime() >= 11.0 * 3600.0 && e.getSimulationTime() <= 16.0 * 3600.0;
		}
	} 

	@Test
	void testRunDrtOfferRejectionExample() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controler controller = DrtControlerCreator.createControler(config, false);
		controller.addOverridingQSimModule(new AbstractDvrpModeQSimModule("drt") {
			@Override
			protected void configureQSim() {
				bindModal(DrtOfferAcceptor.class).toProvider(modalProvider(getter -> new ProbabilisticOfferAcceptor()));
			}
		});
		controller.run();


		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.46)
				.rejections(174.0)
				.waitAverage(222.66)
				.inVehicleTravelTimeMean(369.74)
				.totalTravelTimeMean(592.4)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	/**
	 * Early warning system: if customer stats vary more than the defined percentage above or below the expected values
	 * then the following unit tests will fail. This is meant to serve as a red flag.
	 * The following customer parameter checked are:
	 * rejectionRate, rejections, waitAverage, inVehicleTravelTimeMean, & totalTravelTimeMean
	 */

	 static void verifyDrtCustomerStatsCloseToExpectedStats(String outputDirectory, Stats expectedStats) {

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

	static class Stats {
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

	private static class ProbabilisticOfferAcceptor implements DrtOfferAcceptor {

		private final DefaultOfferAcceptor delegate = new DefaultOfferAcceptor();

		private final Random random = new Random(123);

		@Override
		public Optional<AcceptedDrtRequest> acceptDrtOffer(DrtRequest request, double departureTime, double arrivalTime) {
			if (random.nextBoolean()) {
				return Optional.empty();
			} else {
				return delegate.acceptDrtOffer(request, departureTime, arrivalTime);
			}
		}
	}
}
