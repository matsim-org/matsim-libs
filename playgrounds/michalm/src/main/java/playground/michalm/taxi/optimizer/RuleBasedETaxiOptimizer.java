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

package playground.michalm.taxi.optimizer;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.rules.*;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.michalm.ev.data.Battery;
import playground.michalm.taxi.data.EvrpVehicle;


public class RuleBasedETaxiOptimizer
    extends RuleBasedTaxiOptimizer
{
    private static final double MIN_RELATIVE_SOC = 0.3;
    private static final int SOC_CHECK_TIME_STEP = 300; //5 min


    public RuleBasedETaxiOptimizer(TaxiOptimizerContext optimContext,
            RuleBasedTaxiOptimizerParams params)
    {
        super(optimContext, params);
    }


    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e)
    {
        if (e.getSimulationTime() % SOC_CHECK_TIME_STEP == 0) {
            for (Vehicle v : idleTaxiRegistry.getVehicles()) {
                if (isUndercharged(v)) {
                    //TODO send to a charger
                }
            }
        }

        super.notifyMobsimBeforeSimStep(e);
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        super.nextTask(schedule);

        if (optimContext.scheduler.isIdle(schedule.getVehicle())
                && isUndercharged(schedule.getVehicle())) {
            //TODO send to a charger
        }
    }


    private boolean isUndercharged(Vehicle v)
    {
        Battery b = ((EvrpVehicle)v).getEv().getBattery();
        return b.getSoc() < MIN_RELATIVE_SOC * b.getCapacity();
    }
}
