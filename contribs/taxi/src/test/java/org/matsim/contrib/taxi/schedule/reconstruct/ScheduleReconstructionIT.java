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

package org.matsim.contrib.taxi.schedule.reconstruct;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.passenger.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class ScheduleReconstructionIT {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testOneTaxiReconstruction() {
		URL configUrl = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "one_taxi_benchmark_config.xml");
		runReconstruction(configUrl);
	}

	@Test
	public void testMielecReconstruction() {
		URL configUrl = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_taxi_benchmark_config.xml");
		runReconstruction(configUrl);
	}

	@SuppressWarnings("unchecked")
	private void runReconstruction(URL configUrl) {
		Config config = ConfigUtils.loadConfig(configUrl, new TaxiConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setDumpDataAtEnd(false);

		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
		Controler controler = RunTaxiBenchmark.createControler(config, 1);
		controler.addOverridingModule(new AbstractDvrpModeModule(taxiCfg.getMode()) {
			@Override
			public void install() {
				bindModal(ScheduleReconstructor.class).toProvider(new Provider<ScheduleReconstructor>() {
					@Inject
					private @Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
					Network network;

					@Inject
					private EventsManager eventsManager;

					@Override
					public ScheduleReconstructor get() {
						return new ScheduleReconstructor(network, eventsManager, getMode());
					}
				}).asEagerSingleton();

				installQSimModule(new AbstractDvrpModeQSimModule(taxiCfg.getMode()) {
					@Override
					protected void configureQSim() {
						addModalQSimComponentBinding().toProvider(modalProvider(
								getter -> (MobsimBeforeCleanupListener)(e -> assertScheduleReconstructor(
										getter.getModal(ScheduleReconstructor.class), getter.getModal(Fleet.class),
										getter.getModal(SubmittedTaxiRequestsCollector.class)))));
					}
				});
			}
		});
		controler.run();

	}

	private void assertScheduleReconstructor(ScheduleReconstructor scheduleReconstructor, Fleet fleet,
			SubmittedTaxiRequestsCollector requestCollector) {
		Assert.assertNotEquals(fleet, scheduleReconstructor.getFleet());
		compareVehicles(fleet.getVehicles().values(), scheduleReconstructor.getFleet().getVehicles().values());
		compareRequests((Collection<TaxiRequest>)requestCollector.getRequests().values(),
				scheduleReconstructor.taxiRequests.values());
	}

	private void compareVehicles(Collection<? extends DvrpVehicle> originalVehs,
			Collection<? extends DvrpVehicle> reconstructedVehs) {
		Assert.assertNotEquals(originalVehs, reconstructedVehs);
		Assert.assertEquals(originalVehs.size(), reconstructedVehs.size());

		Iterator<? extends DvrpVehicle> rIter = reconstructedVehs.iterator();
		for (DvrpVehicle o : originalVehs) {
			DvrpVehicle r = rIter.next();

			Assert.assertEquals(o.getId(), r.getId());
			Assert.assertEquals(o.getStartLink(), r.getStartLink());
			Assert.assertEquals(o.getCapacity(), r.getCapacity(), 0);
			Assert.assertEquals(o.getServiceBeginTime(), r.getServiceBeginTime(), 0);
			Assert.assertEquals(o.getServiceEndTime(), r.getServiceEndTime(), 0);

			Schedule oSchedule = o.getSchedule();
			Schedule rSchedule = r.getSchedule();

			Assert.assertEquals(oSchedule.getBeginTime(), rSchedule.getBeginTime(), 0);
			Assert.assertEquals(oSchedule.getEndTime(), rSchedule.getEndTime(), 0);
			Assert.assertEquals(oSchedule.getTaskCount(), rSchedule.getTaskCount());

			Assert.assertEquals(ScheduleStatus.COMPLETED, oSchedule.getStatus());
			Assert.assertEquals(ScheduleStatus.PLANNED, rSchedule.getStatus());

			compareTasks(oSchedule.getTasks(), rSchedule.getTasks());
		}
	}

	private void compareTasks(List<? extends Task> originalTasks, List<? extends Task> reconstructedTasks) {
		Assert.assertEquals(originalTasks.size(), reconstructedTasks.size());

		Iterator<? extends Task> rIter = reconstructedTasks.iterator();
		for (Task o : originalTasks) {
			Task r = rIter.next();

			Assert.assertEquals(o.getBeginTime(), r.getBeginTime(), 0);
			Assert.assertEquals(o.getEndTime(), r.getEndTime(), 0);
			Assert.assertEquals(o.getTaskIdx(), r.getTaskIdx());
			Assert.assertEquals(((TaxiTask)o).getTaxiTaskType(), ((TaxiTask)r).getTaxiTaskType());

			Assert.assertEquals(TaskStatus.PERFORMED, o.getStatus());
			Assert.assertEquals(TaskStatus.PLANNED, r.getStatus());
		}
	}

	private void compareRequests(Collection<TaxiRequest> originalReqs, Collection<TaxiRequest> reconstructedReqs) {
		Assert.assertNotEquals(originalReqs, reconstructedReqs);
		Assert.assertEquals(originalReqs.size(), reconstructedReqs.size());

		Iterator<TaxiRequest> rIter = reconstructedReqs.iterator();
		for (TaxiRequest o : originalReqs) {
			TaxiRequest r = rIter.next();

			// Assert.assertEquals(o.getId(), r.getId()); //TODO we cannot test it before TaxiRequestSubmittedEvent is
			// introduced
			Assert.assertEquals(o.getFromLink(), r.getFromLink());
			Assert.assertEquals(o.getToLink(), r.getToLink());
			Assert.assertEquals(o.getSubmissionTime(), r.getSubmissionTime(), 0);
			Assert.assertEquals(o.getEarliestStartTime(), r.getEarliestStartTime(), 0);
			Assert.assertEquals(o.getLatestStartTime(), r.getLatestStartTime(), 0);

			Assert.assertEquals(TaxiRequestStatus.PERFORMED, o.getStatus());
			Assert.assertEquals(TaxiRequestStatus.PLANNED, r.getStatus());
		}
	}
}
