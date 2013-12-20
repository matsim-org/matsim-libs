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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.optimizer.*;
import org.matsim.contrib.dynagent.DynAgentLogic;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import pl.poznan.put.vrp.dynamic.data.model.Request;
import pl.poznan.put.vrp.dynamic.data.online.VehicleTracker;
import pl.poznan.put.vrp.dynamic.data.schedule.*;


public class VrpSimEngine
    implements MobsimEngine, MobsimBeforeSimStepListener
{
    private final MatsimVrpData data;
    private final QSim qsim;
    private final VrpOptimizer optimizer;

    private final List<DynAgentLogic> agentLogics = new ArrayList<DynAgentLogic>();

    private InternalInterface internalInterface;


    public VrpSimEngine(QSim qsim, MatsimVrpData data, VrpOptimizer optimizer)
    {
        this.qsim = qsim;
        this.data = data;
        this.optimizer = optimizer;
        qsim.addQueueSimulationListeners(this);
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
        int time = (int)qsim.getSimTimer().getTimeOfDay();
        data.getVrpData().setTime(time);

        Map<Id, MobsimAgent> agents = data.getMobsimAgents();
        for (MobsimAgent mobsimAgent : qsim.getAgents()) {
            agents.put(mobsimAgent.getId(), mobsimAgent);
        }

        optimizer.init();
        notifyAgentLogics();
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        data.getVrpData().setTime((int)qsim.getSimTimer().getTimeOfDay());
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


    public void nextTask(Schedule<? extends Task> schedule, int time)
    {
        optimizer.nextTask(schedule);
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
