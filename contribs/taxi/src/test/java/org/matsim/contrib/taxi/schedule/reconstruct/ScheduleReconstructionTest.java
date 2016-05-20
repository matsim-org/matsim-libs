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
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.taxi.data.*;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.run.*;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


public class ScheduleReconstructionTest
{
    @Test
    public void testOneTaxiReconstruction()
    {
        runReconstruction("./src/main/resources/one_taxi/one_taxi_config.xml");
    }


    @Test
    public void testMielecReconstruction()
    {
        runReconstruction("./src/main/resources/mielec_2014_02/config.xml");
    }


    private void runReconstruction(String configFile)
    {
        Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(),
                new OTFVisConfigGroup());
        config.controler().setLastIteration(0);

        Controler controler = RunTaxiScenario.createControler(config, false);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install()
            {
                bind(ScheduleReconstructor.class).asEagerSingleton();
            }
        });
        controler.run();

        TaxiData reconstructedTaxiData = controler.getInjector()
                .getInstance(ScheduleReconstructor.class).getTaxiData();
        TaxiData taxiData = controler.getInjector().getInstance(TaxiData.class);
        Assert.assertNotEquals(taxiData, reconstructedTaxiData);

        compareVehicles(taxiData.getVehicles().values(),
                reconstructedTaxiData.getVehicles().values());

        compareRequests(taxiData.getTaxiRequests().values(),
                reconstructedTaxiData.getTaxiRequests().values());
    }


    private void compareVehicles(Collection<Vehicle> originalVehs,
            Collection<Vehicle> reconstructedVehs)
    {
        Assert.assertEquals(originalVehs.size(), reconstructedVehs.size());

        Iterator<Vehicle> rIter = reconstructedVehs.iterator();
        for (Vehicle o : originalVehs) {
            Vehicle r = rIter.next();

            Assert.assertEquals(o.getId(), r.getId());
            Assert.assertEquals(o.getStartLink(), r.getStartLink());
            Assert.assertEquals(o.getCapacity(), r.getCapacity(), 0);
            Assert.assertEquals(o.getT0(), r.getT0(), 0);
            Assert.assertEquals(o.getT1(), r.getT1(), 0);

            Schedule<TaxiTask> oSchedule = TaxiSchedules.asTaxiSchedule(o.getSchedule());
            Schedule<TaxiTask> rSchedule = TaxiSchedules.asTaxiSchedule(r.getSchedule());

            Assert.assertEquals(oSchedule.getBeginTime(), rSchedule.getBeginTime(), 0);
            Assert.assertEquals(oSchedule.getEndTime(), rSchedule.getEndTime(), 0);
            Assert.assertEquals(oSchedule.getTaskCount(), rSchedule.getTaskCount());

            Assert.assertEquals(ScheduleStatus.COMPLETED, oSchedule.getStatus());
            Assert.assertEquals(ScheduleStatus.PLANNED, rSchedule.getStatus());

            compareTasks(oSchedule.getTasks(), rSchedule.getTasks());
        }
    }


    private void compareTasks(List<TaxiTask> originalTasks, List<TaxiTask> reconstructedTasks)
    {
        Assert.assertEquals(originalTasks.size(), reconstructedTasks.size());

        Iterator<TaxiTask> rIter = reconstructedTasks.iterator();
        for (TaxiTask o : originalTasks) {
            TaxiTask r = rIter.next();

            Assert.assertEquals(o.getBeginTime(), r.getBeginTime(), 0);
            Assert.assertEquals(o.getEndTime(), r.getEndTime(), 0);
            Assert.assertEquals(o.getTaskIdx(), r.getTaskIdx());
            Assert.assertEquals(o.getType(), r.getType());
            Assert.assertEquals(o.getTaxiTaskType(), r.getTaxiTaskType());

            Assert.assertEquals(TaskStatus.PERFORMED, o.getStatus());
            Assert.assertEquals(TaskStatus.PLANNED, r.getStatus());
        }
    }


    private void compareRequests(Collection<TaxiRequest> originalReqs,
            Collection<TaxiRequest> reconstructedReqs)
    {
        Assert.assertEquals(originalReqs.size(), reconstructedReqs.size());

        Iterator<TaxiRequest> rIter = reconstructedReqs.iterator();
        for (TaxiRequest o : originalReqs) {
            TaxiRequest r = rIter.next();

            //Assert.assertEquals(o.getId(), r.getId()); //TODO we cannot test it before TaxiRequestSubmittedEvent is introduced
            Assert.assertEquals(o.getFromLink(), r.getFromLink());
            Assert.assertEquals(o.getToLink(), r.getToLink());
            Assert.assertEquals(o.getQuantity(), r.getQuantity(), 0);
            Assert.assertEquals(o.getSubmissionTime(), r.getSubmissionTime(), 0);
            Assert.assertEquals(o.getT0(), r.getT0(), 0);
            Assert.assertEquals(o.getT1(), r.getT1(), 0);

            Assert.assertEquals(TaxiRequestStatus.PERFORMED, o.getStatus());
            Assert.assertEquals(TaxiRequestStatus.PLANNED, r.getStatus());
        }
    }
}
