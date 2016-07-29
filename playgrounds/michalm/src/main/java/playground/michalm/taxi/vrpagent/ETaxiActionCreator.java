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

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.mobsim.framework.MobsimTimer;

import playground.michalm.taxi.schedule.ETaxiChargingTask;


public class ETaxiActionCreator
    extends TaxiActionCreator
{
    public ETaxiActionCreator(PassengerEngine passengerEngine, LegCreator legCreator,
            double pickupDuration, MobsimTimer timer)
    {
        super(passengerEngine, legCreator, pickupDuration);
    }


    @Override
    public DynAction createAction(Task task, double now)
    {
        if (task instanceof ETaxiChargingTask) {
            return new ETaxiAtChargerActivity((ETaxiChargingTask)task);
        }

        return super.createAction(task, now);
    }
}
