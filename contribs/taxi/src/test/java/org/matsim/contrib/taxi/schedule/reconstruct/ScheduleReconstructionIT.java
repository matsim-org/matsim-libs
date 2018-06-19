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

import java.util.*;

import org.junit.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class ScheduleReconstructionIT {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testOneTaxiReconstruction() {
		runReconstruction("one_taxi/one_taxi_config.xml");
	}

	@Test
	public void testMielecReconstruction() {
		runReconstruction("mielec_2014_02/mielec_taxi_benchmark_config.xml");
	}

	@SuppressWarnings("unchecked")
	private void runReconstruction(String configFile) {
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setDumpDataAtEnd(false);

		Controler controler = RunTaxiBenchmark.createControler(config, 1);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ScheduleReconstructor.class).asEagerSingleton();
			}
		});
		controler.run();

		ScheduleReconstructor scheduleReconstructor = controler.getInjector().getInstance(ScheduleReconstructor.class);

		Fleet fleet = controler.getInjector().getInstance(Fleet.class);
		SubmittedTaxiRequestsCollector requestCollector = controler.getInjector()
				.getInstance(SubmittedTaxiRequestsCollector.class);

		Assert.assertNotEquals(fleet, scheduleReconstructor.fleet);

		compareVehicles(fleet.getVehicles().values(), scheduleReconstructor.fleet.getVehicles().values());

		compareRequests((Collection<TaxiRequest>)requestCollector.getRequests().values(),
				scheduleReconstructor.taxiRequests.values());
	}

	private void compareVehicles(Collection<? extends Vehicle> originalVehs,
			Collection<? extends Vehicle> reconstructedVehs) {
		Assert.assertNotEquals(originalVehs, reconstructedVehs);
		Assert.assertEquals(originalVehs.size(), reconstructedVehs.size());

		Iterator<? extends Vehicle> rIter = reconstructedVehs.iterator();
		for (Vehicle o : originalVehs) {
			Vehicle r = rIter.next();

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
