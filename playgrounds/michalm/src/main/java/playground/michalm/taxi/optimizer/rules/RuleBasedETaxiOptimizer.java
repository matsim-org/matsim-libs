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

package playground.michalm.taxi.optimizer.rules;

import java.util.Collections;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import com.google.common.collect.Iterables;

import playground.michalm.ev.data.*;
import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.schedule.ETaxiChargingTask;
import playground.michalm.taxi.scheduler.ETaxiScheduler;


public class RuleBasedETaxiOptimizer
    extends RuleBasedTaxiOptimizer
{
    //TODO MIN_RELATIVE_SOC should depend on the weather and time of day
    private final RuleBasedETaxiOptimizerParams params;
    private final EvData evData;
    private final BestChargerFinder eDispatchFinder;
    private final ETaxiScheduler eScheduler;


    public RuleBasedETaxiOptimizer(ETaxiOptimizerContext optimContext,
            RuleBasedETaxiOptimizerParams params)
    {
        super(optimContext, params);
        this.params = params;
        evData = optimContext.evData;
        eScheduler = (ETaxiScheduler)optimContext.scheduler;
        eDispatchFinder = new BestChargerFinder(dispatchFinder);
    }


    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e)
    {
        if (isNewDecisionEpoch(e, params.socCheckTimeStep)) {
            chargeIdleUnderchargedVehicles(
                    Iterables.filter(idleTaxiRegistry.getVehicles(), this::isUndercharged));
        }

        super.notifyMobsimBeforeSimStep(e);
    }


    private void chargeIdleUnderchargedVehicles(Iterable<Vehicle> vehicles)
    {
        for (Vehicle v : vehicles) {
            Dispatch<Charger> eDispatch = eDispatchFinder.findBestChargerForVehicle(v,
                    evData.getChargers().values());
            eScheduler.scheduleCharging((EvrpVehicle)v, eDispatch.destination, eDispatch.path);
        }
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        super.nextTask(schedule);

        Vehicle veh = schedule.getVehicle();
        if (optimContext.scheduler.isIdle(veh) && isUndercharged(veh)) {
            chargeIdleUnderchargedVehicles(Collections.singleton(veh));
        }
    }


    @Override
    protected boolean isWaitStay(TaxiTask task)
    {
        return task.getTaxiTaskType() == TaxiTaskType.STAY && ! (task instanceof ETaxiChargingTask);
    }


    private boolean isUndercharged(Vehicle v)
    {
        Battery b = ((EvrpVehicle)v).getEv().getBattery();
        return b.getSoc() < params.minRelativeSoc * b.getCapacity();
    }
}
