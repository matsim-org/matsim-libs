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
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams;
import org.matsim.contrib.drt.optimizer.insertion.parallel.ParallelRequestInserterModule;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
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
	void testRunDrtExampleWithParallelInserter_ExtensiveSearch() {
		Id.resetCaches();

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
			new OTFVisConfigGroup());

		var drtCfg = MultiModeDrtConfigGroup.get(config).getModalElements().iterator().next();

		//disable rejections
		drtCfg.addOrGetDrtOptimizationConstraintsParams()
			.addOrGetDefaultDrtOptimizationConstraintsSet()
			.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		DrtParallelInserterParams params = new DrtParallelInserterParams();
		params.setLogThreadActivity(true);
		drtCfg.addParameterSet(params);


		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		Controler controller = DrtControlerCreator.createControler(config, false);
		controller.addOverridingQSimModule(new ParallelRequestInserterModule(drtCfg));
		controller.run();

		var expectedStats = Stats.newBuilder()
			.rejectionRate(0.0)
			.rejections(0)
			.waitAverage(324.6)
			.inVehicleTravelTimeMean(462.7)
			.totalTravelTimeMean(787.3)
			.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}


	@Test
	void testRunDrtExampleWithParallelInserter_SelectiveSearch() {
		Id.resetCaches();

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
			new OTFVisConfigGroup());

		var drtCfg = MultiModeDrtConfigGroup.get(config).getModalElements().iterator().next();

		//disable rejections
		drtCfg.addOrGetDrtOptimizationConstraintsParams()
			.addOrGetDefaultDrtOptimizationConstraintsSet()
			.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		DrtParallelInserterParams params = new DrtParallelInserterParams();
		params.setLogThreadActivity(true);
		drtCfg.addParameterSet(params);

		drtCfg.removeParameterSet(drtCfg.getDrtInsertionSearchParams());
		var selectiveInsertionSearchParams = new SelectiveInsertionSearchParams();
		// using exactly free-speed estimates
		selectiveInsertionSearchParams.setRestrictiveBeelineSpeedFactor(1);
		drtCfg.addParameterSet(selectiveInsertionSearchParams);


		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		Controler controller = DrtControlerCreator.createControler(config, false);
		controller.addOverridingQSimModule(new ParallelRequestInserterModule(drtCfg));
		controller.run();

		var expectedStats = Stats.newBuilder()
			.rejectionRate(0.0)
			.rejections(0)
			.waitAverage(314.6)
			.inVehicleTravelTimeMean(462.11)
			.totalTravelTimeMean(776.72)
			.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}


	@Test
	void testRunDrtExampleWithParallelInserter_RepeatedSelectiveSearch() {
		Id.resetCaches();

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
			new OTFVisConfigGroup());

		var drtCfg = MultiModeDrtConfigGroup.get(config).getModalElements().iterator().next();

		//disable rejections
		drtCfg.addOrGetDrtOptimizationConstraintsParams()
			.addOrGetDefaultDrtOptimizationConstraintsSet()
			.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		DrtParallelInserterParams params = new DrtParallelInserterParams();
		params.setLogThreadActivity(true);
		drtCfg.addParameterSet(params);

		drtCfg.removeParameterSet(drtCfg.getDrtInsertionSearchParams());
		var repeatedSelectiveInsertionSearchParams = new RepeatedSelectiveInsertionSearchParams();
		drtCfg.addParameterSet(repeatedSelectiveInsertionSearchParams);


		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		Controler controller = DrtControlerCreator.createControler(config, false);
		controller.addOverridingQSimModule(new ParallelRequestInserterModule(drtCfg));
		controller.run();

		var expectedStats = Stats.newBuilder()
			.rejectionRate(0.0)
			.rejections(0)
			.waitAverage(314.46)
			.inVehicleTravelTimeMean(457.59)
			.totalTravelTimeMean(772.05)
			.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}


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
					.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		}

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(0)
				.waitAverage(316.91)
				.inVehicleTravelTimeMean(465.17)
				.totalTravelTimeMean(782.08)
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
			selectiveInsertionSearchParams.setRestrictiveBeelineSpeedFactor(1);
			drtCfg.addParameterSet(selectiveInsertionSearchParams);

			//disable rejections
			drtCfg.addOrGetDrtOptimizationConstraintsParams()
					.addOrGetDefaultDrtOptimizationConstraintsSet()
					.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		}

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(0)
				.waitAverage(315.56)
				.inVehicleTravelTimeMean(465.22)
				.totalTravelTimeMean(780.78)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtExampleWithLateRequest() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
				new OTFVisConfigGroup());

		// !!! IMPORTANT: use the plans with a late request
		config.plans().setInputFile("plans_only_drt_1.0_with_late_request.xml.gz");

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//replace extensive with selective search
			drtCfg.removeParameterSet(drtCfg.getDrtInsertionSearchParams());
			var selectiveInsertionSearchParams = new SelectiveInsertionSearchParams();
			// using exactly free-speed estimates
			selectiveInsertionSearchParams.setRestrictiveBeelineSpeedFactor(1);
			drtCfg.addParameterSet(selectiveInsertionSearchParams);

			//disable rejections
			drtCfg.addOrGetDrtOptimizationConstraintsParams()
					.addOrGetDefaultDrtOptimizationConstraintsSet()
					.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		}

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);
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
			repeatedSelectiveInsertionSearchParams.setRetryInsertion(5);
			drtCfg.addParameterSet(repeatedSelectiveInsertionSearchParams);

			//disable rejections
			drtCfg.addOrGetDrtOptimizationConstraintsParams()
					.addOrGetDefaultDrtOptimizationConstraintsSet()
					.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		}

		config.controller().setLastIteration(3);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
			.rejectionRate(0.0)
			.rejections(0)
			.waitAverage(289.1)
			.inVehicleTravelTimeMean(449.74)
			.totalTravelTimeMean(738.84)
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
			drtRequestInsertionRetryParams.setMaxRequestAge(7200);
			drtCfg.addParameterSet(drtRequestInsertionRetryParams);
		}

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(0)
				.waitAverage(313.99)
				.inVehicleTravelTimeMean(461.24)
				.totalTravelTimeMean(775.23)
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
				.rejectionRate(0.02)
				.rejections(6)
				.waitAverage(286.91)
				.inVehicleTravelTimeMean(458.22)
				.totalTravelTimeMean(745.13)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testRunDrtStopbasedExample_maxRideDuration() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		DrtOptimizationConstraintsSetImpl drtOptimizationConstraintsSet = multiModeDrtConfigGroup.getModalElements().iterator().next().addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();
		drtOptimizationConstraintsSet.setMaxAbsoluteDetour(5 * 60);

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.02)
				.rejections(6)
				.waitAverage(286.92)
				.inVehicleTravelTimeMean(405.92)
				.totalTravelTimeMean(692.84)
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
				.rejectionRate(0.02)
				.rejections(6)
				.waitAverage(289.12)
				.inVehicleTravelTimeMean(461.2)
				.totalTravelTimeMean(750.33)
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
				.rejectionRate(0.01)
				.rejections(4)
				.waitAverage(258.42)
				.inVehicleTravelTimeMean(447.99)
				.totalTravelTimeMean(706.41)
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
				bindModal(PassengerStopDurationProvider.class).toInstance(stopDurationProvider);
				bindModal(StopTimeCalculator.class).toInstance(stopTimeCalculator);
			}
		});

		controller.run();

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.01)
				.rejections(5)
				.waitAverage(303.17)
				.inVehicleTravelTimeMean(462.72)
				.totalTravelTimeMean(765.89)
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
		prebookingParams.setAbortRejectedPrebookings(false);
		drtConfig.addParameterSet(prebookingParams);

		Controler controller = DrtControlerCreator.createControler(config, false);
		ProbabilityBasedPrebookingLogic.install(controller, drtConfig, 0.5, 4.0 * 3600.0);

		PrebookingTracker tracker = new PrebookingTracker();
		tracker.install(controller);

		controller.run();

		assertEquals(177, tracker.immediateScheduled);
		assertEquals(205, tracker.prebookedScheduled);
		assertEquals(6, tracker.immediateRejected);
		assertEquals(0, tracker.prebookedRejected);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.02)
				.rejections(6)
				.waitAverage(260.76)
				.inVehicleTravelTimeMean(478.79)
				.totalTravelTimeMean(739.54)
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
		drtConfig.setUpdateRoutes(true);

		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.setAbortRejectedPrebookings(false);
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

		assertEquals(154, tracker.immediateScheduled);
		assertEquals(203, tracker.prebookedScheduled);
		assertEquals(31, tracker.immediateRejected);
		assertEquals(2, tracker.prebookedRejected);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.08)
				.rejections(33)
				.waitAverage(235.6)
				.inVehicleTravelTimeMean(443.23)
				.totalTravelTimeMean(678.83)
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
				.rejectionRate(0.45)
				.rejections(170.0)
				.waitAverage(250.8)
				.inVehicleTravelTimeMean(433.65)
				.totalTravelTimeMean(684.44)
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
		public Optional<AcceptedDrtRequest> acceptDrtOffer(DrtRequest request,
														   double departureTime, double arrivalTime,
														   double pickupDuration, double dropoffDuration) {
			if (random.nextBoolean()) {
				return Optional.empty();
			} else {
				return delegate.acceptDrtOffer(request, departureTime, arrivalTime, 0, dropoffDuration);
			}
		}
	}
}
