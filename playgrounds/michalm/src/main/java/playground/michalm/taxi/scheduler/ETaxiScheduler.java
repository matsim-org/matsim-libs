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

package playground.michalm.taxi.scheduler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

import playground.michalm.ev.data.Charger;
import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.ev.ETaxiChargingWithQueueingLogic;
import playground.michalm.taxi.schedule.ETaxiChargingTask;


public class ETaxiScheduler
    extends TaxiScheduler
{

    public ETaxiScheduler(Scenario scenario, TaxiData taxiData, MobsimTimer timer,
            TaxiSchedulerParams params, TravelTime travelTime, TravelDisutility travelDisutility)
    {
        super(scenario, taxiData, timer, params, travelTime, travelDisutility);
    }


    public void scheduleCharging(EvrpVehicle vehicle, Charger charger,
            VrpPathWithTravelData vrpPath)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
        divertOrAppendDrive(schedule, vrpPath);

        ETaxiChargingWithQueueingLogic logic = (ETaxiChargingWithQueueingLogic)charger.getLogic();
        double chargingEndTime = vrpPath.getArrivalTime() + logic.estimateMaxWaitTime()
                + logic.estimateChargeTime(vehicle.getEv());
        schedule.addTask(new ETaxiChargingTask(vrpPath.getArrivalTime(), chargingEndTime, charger));

        appendStayTask(schedule);//equivalent to TaxiScheduler.appendTasksAfterDropoff 
    }
}
