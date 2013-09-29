/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp;

import java.util.*;

import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.optimizer.*;
import org.matsim.contrib.dynagent.DynAgentLogic;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.online.VehicleTracker;


public class VrpSimEngine
    implements MobsimEngine, MobsimBeforeSimStepListener
{
    private final VrpData vrpData;
    protected final MobsimTimer simTimer;

    private final VrpOptimizer optimizer;

    private final List<DynAgentLogic> agentLogics = new ArrayList<DynAgentLogic>();

    private InternalInterface internalInterface;


    public VrpSimEngine(Netsim netsim, MatsimVrpData data, VrpOptimizer optimizer)
    {
        this.simTimer = netsim.getSimTimer();
        this.optimizer = optimizer;
        this.vrpData = data.getVrpData();
        netsim.addQueueSimulationListeners(this);
    }


    public InternalInterface getInternalInterface()
    {
        return internalInterface;
    }


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        this.internalInterface = internalInterface;
    }


    @Override
    public void onPrepareSim()
    {
        // important: in some cases one may need to decrease simTimer.time
        int time = (int)simTimer.getTimeOfDay();
        vrpData.setTime(time);

        optimizer.init();
        notifyAgentLogics();
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        vrpData.setTime((int)simTimer.getTimeOfDay());
    }


    /**
     * This function can be generalized (in the future) to encompass request modification,
     * cancellation etc. See:
     * {@link org.matsim.contrib.dvrp.optimizer.VrpOptimizer#requestSubmitted(Request)}
     */
    public void requestSubmitted(Request request, double now)
    {
        optimizer.requestSubmitted(request);
        notifyAgentLogics();
    }


    public void nextTask(Vehicle vrpVehicle, int time)
    {
        optimizer.nextTask(vrpVehicle);
        notifyAgentLogics();
    }


    public void nextPositionReached(VehicleTracker vehicleTracker)
    {
        boolean scheduleChanged = ((VrpOptimizerWithOnlineTracking)optimizer)
                .nextPositionReached(vehicleTracker);

        if (scheduleChanged) {
            notifyAgentLogics();
        }
    }


    /**
     * This method is called just after a queue-simulation step
     */
    @Override
    public void doSimStep(double time)
    {}


    @Override
    public void afterSim()
    {}


    protected void notifyAgentLogics()
    {
        for (DynAgentLogic a : agentLogics) {
            a.actionPossiblyChanged();
        }
    }


    public void addAgentLogic(DynAgentLogic agentLogic)
    {
        agentLogics.add(agentLogic);
    }


    public void removeAgentLogic(DynAgentLogic agentLogic)
    {
        agentLogics.remove(agentLogic);
    }
}
