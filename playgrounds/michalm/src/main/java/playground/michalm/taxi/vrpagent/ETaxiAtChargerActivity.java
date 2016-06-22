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
import org.matsim.core.mobsim.framework.MobsimTimer;

import playground.michalm.taxi.data.EvrpVehicle.Ev;
import playground.michalm.taxi.ev.ETaxiChargingLogic;
import playground.michalm.taxi.schedule.ETaxiChargingTask;


public class ETaxiAtChargerActivity
    extends AbstractDynActivity
{
    public static final String STAY_AT_CHARGER_ACTIVITY_TYPE = "ETaxiStayAtCharger";

    private final ETaxiChargingTask chargingTask;
    private final MobsimTimer timer;

    private boolean chargingEnded = false;
    private double endTime;


    public ETaxiAtChargerActivity(ETaxiChargingTask chargingTask, MobsimTimer timer)
    {
        super(STAY_AT_CHARGER_ACTIVITY_TYPE);
        this.chargingTask = chargingTask;
        this.timer = timer;

        onActivityStart();
    }


    private void onActivityStart()
    {
        ETaxiChargingLogic logic = chargingTask.getLogic();
        Ev ev = chargingTask.getEv();

        logic.removeDispatchedVehicle(ev);
        logic.addVehicle(ev);
        endTime = timer.getTimeOfDay() + logic.estimateMaxWaitTimeOnArrival()
                + logic.estimateChargeTime(ev);

    }


    @Override
    public void doSimStep(double now)
    {
        if (!chargingEnded && endTime <= now) {
            endTime = now + 1;
        }
    }


    @Override
    public double getEndTime()
    {
        return endTime;
    }


    public void notifyChargingStarted()
    {
        endTime = timer.getTimeOfDay()
                + chargingTask.getLogic().estimateChargeTime(chargingTask.getEv());
    }


    public void notifyChargingEnded()
    {
        chargingEnded = true;
        endTime = timer.getTimeOfDay();
    }
}
