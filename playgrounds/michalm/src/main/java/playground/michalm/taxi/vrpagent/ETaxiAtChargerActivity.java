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

package playground.michalm.taxi.vrpagent;

import org.matsim.contrib.dynagent.AbstractDynActivity;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.core.mobsim.framework.MobsimTimer;

import playground.michalm.ev.data.*;
import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.data.EvrpVehicle.Ev;
import playground.michalm.taxi.ev.ETaxiChargingWithQueueingLogic;
import playground.michalm.taxi.schedule.ETaxiStayAtChargerTask;


public class ETaxiAtChargerActivity
    extends AbstractDynActivity
{
    public static final String STAY_AT_CHARGER_ACTIVITY_TYPE = "ETaxiStayAtCharger";

    private final ETaxiStayAtChargerTask stayAtChargerTask;
    private final Ev ev;
    private final MobsimTimer timer;

    private double endTime = DynActivityEngine.END_ACTIVITY_LATER;


    public ETaxiAtChargerActivity(ETaxiStayAtChargerTask stayAtChargerTask, MobsimTimer timer)
    {
        super(STAY_AT_CHARGER_ACTIVITY_TYPE);
        this.stayAtChargerTask = stayAtChargerTask;
        this.timer = timer;

        ev = ((EvrpVehicle)stayAtChargerTask.getSchedule().getVehicle()).getEv();
        ((ETaxiChargingWithQueueingLogic)stayAtChargerTask.getCharger().getLogic())
                .addVehicle(ev);
    }


    @Override
    public void doSimStep(double now)
    {}


    @Override
    public double getEndTime()
    {
        return endTime;
    }


    public void notifyChargingStarted()
    {
        Battery b = ev.getBattery();
        int predictedChargeTime = (int)Math
                .ceil( (b.getCapacity() - b.getSoc()) / stayAtChargerTask.getCharger().getPower());
        endTime = timer.getTimeOfDay() + predictedChargeTime;
    }


    public void notifyChargingEnded()
    {
        endTime = timer.getTimeOfDay();
    }
}
