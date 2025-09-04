/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.edrt.run;

import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.insertion.spatialFilter.DrtSpatialRequestFleetFilterParams;
import org.matsim.contrib.drt.extension.insertion.spatialFilter.SpatialFilterInsertionSearchQSimModule;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams;
import org.matsim.contrib.drt.optimizer.insertion.parallel.ParallelRequestInserterModule;
import org.matsim.contrib.drt.optimizer.insertion.repeatedselective.RepeatedSelectiveInsertionSearchParams;
import org.matsim.contrib.drt.prebooking.PrebookingParams;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogic;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author michalm
 */
public class RunEDrtScenarioIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void test() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_edrt_config.xml");
		RunEDrtScenario.run(configUrl, false);
	}

	@Test
	void testRunDrtExampleWithParallelInserter_SpatialFiltering() {
		Id.resetCaches();

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new), dvrpConfigGroup,
			new OTFVisConfigGroup());

		var drtCfg = MultiModeDrtConfigGroup.get(config).getModalElements().iterator().next();

		//disable rejections
		drtCfg.addOrGetDrtOptimizationConstraintsParams()
			.addOrGetDefaultDrtOptimizationConstraintsSet()
			.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);

		// Not optimal settings, it is just to ensure that there a multiple threads working
		DrtParallelInserterParams params = new DrtParallelInserterParams();
		params.setLogThreadActivity(true);
		params.setMaxPartitions(2);
		params.setRequestsPartitioner(DrtParallelInserterParams.RequestsPartitioner.RoundRobinRequestsPartitioner);
		drtCfg.addParameterSet(params);

		drtCfg.removeParameterSet(drtCfg.getDrtInsertionSearchParams());
		var repeatedSelectiveInsertionSearchParams = new RepeatedSelectiveInsertionSearchParams();
		drtCfg.addParameterSet(repeatedSelectiveInsertionSearchParams);

		DrtSpatialRequestFleetFilterParams drtSpatialRequestFleetFilterParams = new DrtSpatialRequestFleetFilterParams();
		drtCfg.addParameterSet(drtSpatialRequestFleetFilterParams);

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		Controler controller = DrtControlerCreator.createControler(config, false);
		controller.addOverridingQSimModule(new ParallelRequestInserterModule(drtCfg));
		PassengerPickUpTracker tracker = new PassengerPickUpTracker();
		tracker.install(controller);
		controller.addOverridingQSimModule(new ParallelRequestInserterModule(drtCfg));
		controller.addOverridingQSimModule(new SpatialFilterInsertionSearchQSimModule(drtCfg));
		controller.run();

		assertEquals(388, tracker.passengerPickupEvents);
	}

	@Test
	void testMultiModeDrtDeterminism() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_multiModeEdrt_config.xml");

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
			new OTFVisConfigGroup(), new EvConfigGroup());

		Controler controller = RunEDrtScenario.createControler(config, false);
		config.controller().setLastIteration(2);

		PassengerPickUpTracker tracker = new PassengerPickUpTracker();
		tracker.install(controller);

		controller.run();

		assertEquals(1919, tracker.passengerPickupEvents);
	}


	@Test
	void testWithPrebooking() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_edrt_config.xml");

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfigGroup,
				new OTFVisConfigGroup(), new EvConfigGroup());

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);
		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.setAbortRejectedPrebookings(false);
		drtConfig.addParameterSet(prebookingParams);

		// do not schedule hub returns before prebooked stops
		drtConfig.setReturnToDepotMinIdleGap(Double.MAX_VALUE);

		Controler controller = RunEDrtScenario.createControler(config, false);
		ProbabilityBasedPrebookingLogic.install(controller, drtConfig, 0.5, 4.0 * 3600.0);

		PrebookingTracker tracker = new PrebookingTracker();
		tracker.install(controller);

		controller.run();

		assertEquals(112, tracker.immediateScheduled);
		assertEquals(182, tracker.prebookedScheduled);
		assertEquals(94, tracker.immediateRejected);
		assertEquals(23, tracker.prebookedRejected);
	}

	static private class PassengerPickUpTracker implements PassengerPickedUpEventHandler {
		int passengerPickupEvents = 0;

		void install(Controler controller) {
			PassengerPickUpTracker thisTracker = this;

			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(thisTracker);
				}
			});
		}

		@Override
		public void handleEvent(PassengerPickedUpEvent event) {
			passengerPickupEvents++;
		}

		@Override
		public void reset(int iteration) {
			passengerPickupEvents=0;
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
